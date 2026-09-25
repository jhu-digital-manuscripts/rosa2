"""Index administration and bulk ingest for the rosa2 OpenSearch domain.

The domain has no public endpoint, so nothing outside the VPC can create an index
or post bulk data to it. This function runs inside the VPC and is invoked
directly with the AWS CLI, which makes `rosa-search/init-opensearch.sh` runnable
from any machine that has credentials, with no bastion host and no public
endpoint.

Bulk payloads are read from S3 rather than passed in the invocation, which keeps
the request payload tiny and sidesteps the Lambda and API Gateway size limits.
Ingest is resumable: each invocation works through as much of an object as it can
before its time budget runs out, then reports the byte offset to resume from. The
caller loops until `done` is true, so a corpus that takes longer than the Lambda
timeout still completes.

Invoke with a JSON payload containing an `action`:

    {"action": "health"}
    {"action": "cluster_settings"}
    {"action": "recreate_index", "index": "canvas", "definition_key": "..."}
    {"action": "bulk_ingest", "key": "...", "start_byte": 0}
    {"action": "refresh", "index": "canvas"}
    {"action": "count", "index": "canvas"}
    {"action": "analyze", "index": "canvas", "analyzer": "latin", "text": "..."}

Environment variables:
    OPENSEARCH_ENDPOINT  Domain endpoint host, no scheme and no trailing slash.
    OPENSEARCH_REGION    Region of the domain. Defaults to AWS_REGION.
    INGEST_BUCKET        S3 bucket holding index definitions and bulk files.
    BULK_BATCH_LINES     Lines per _bulk request. Must be even. Default 2000.
    BULK_BATCH_BYTES     Byte ceiling per _bulk request. Default 5000000.
    BULK_PAUSE_SECONDS   Delay between _bulk requests. Default 0.3.
    RESERVE_SECONDS      Stop and report progress with this much time left.
    UPSTREAM_TIMEOUT     Socket timeout in seconds when calling OpenSearch.
"""

from __future__ import annotations

import json
import logging
import os
import time
import urllib.error
import urllib.request
from typing import Any

import boto3
from botocore.auth import SigV4Auth
from botocore.awsrequest import AWSRequest

logger = logging.getLogger()
logger.setLevel(logging.INFO)


def _int_env(name: str, default: int) -> int:
    try:
        return int(os.environ.get(name, default))
    except (TypeError, ValueError):
        return default


def _float_env(name: str, default: float) -> float:
    try:
        return float(os.environ.get(name, default))
    except (TypeError, ValueError):
        return default


OPENSEARCH_ENDPOINT = os.environ.get("OPENSEARCH_ENDPOINT", "").replace("https://", "").rstrip("/")
OPENSEARCH_REGION = os.environ.get("OPENSEARCH_REGION") or os.environ.get("AWS_REGION", "us-east-1")
INGEST_BUCKET = os.environ.get("INGEST_BUCKET", "")

BULK_BATCH_LINES = _int_env("BULK_BATCH_LINES", 2000)
BULK_BATCH_BYTES = _int_env("BULK_BATCH_BYTES", 5_000_000)
BULK_PAUSE_SECONDS = _float_env("BULK_PAUSE_SECONDS", 0.3)
RESERVE_SECONDS = _int_env("RESERVE_SECONDS", 60)
UPSTREAM_TIMEOUT = _int_env("UPSTREAM_TIMEOUT", 120)

# A bulk request body is pairs of lines: an action line then a document line. A
# batch must never split a pair.
if BULK_BATCH_LINES % 2:
    BULK_BATCH_LINES += 1

ALLOWED_INDEXES = frozenset({"manifest", "canvas"})

_session = boto3.Session()
_credentials = _session.get_credentials()
_s3 = _session.client("s3")


class AdminError(Exception):
    """An action could not be completed."""


def _require_index(name: Any) -> str:
    if name not in ALLOWED_INDEXES:
        raise AdminError(f"index must be one of {sorted(ALLOWED_INDEXES)}, got {name!r}")
    return str(name)


def request(
    method: str,
    path: str,
    body: bytes | None = None,
    content_type: str = "application/json",
) -> tuple[int, bytes]:
    """Make a SigV4-signed request against the domain."""
    if not OPENSEARCH_ENDPOINT:
        raise AdminError("OPENSEARCH_ENDPOINT is not configured")

    url = f"https://{OPENSEARCH_ENDPOINT}{path}"
    headers = {"Host": OPENSEARCH_ENDPOINT}
    if body is not None:
        headers["Content-Type"] = content_type

    signed = AWSRequest(method=method, url=url, data=body, headers=headers)
    SigV4Auth(_credentials, "es", OPENSEARCH_REGION).add_auth(signed)

    outbound = urllib.request.Request(  # noqa: S310 - fixed https scheme
        url, data=body, headers=dict(signed.headers), method=method
    )

    try:
        with urllib.request.urlopen(outbound, timeout=UPSTREAM_TIMEOUT) as response:
            return response.status, response.read()
    except urllib.error.HTTPError as error:
        return error.code, error.read()
    except urllib.error.URLError as error:
        raise AdminError(f"cannot reach OpenSearch: {error.reason}") from None


def _json_request(method: str, path: str, body: bytes | None = None) -> dict[str, Any]:
    status, payload = request(method, path, body)
    text = payload.decode("utf-8", "replace")
    if status != 200:
        raise AdminError(f"{method} {path} returned HTTP {status}: {text[:2000]}")
    try:
        return json.loads(text)
    except ValueError:
        raise AdminError(f"{method} {path} returned non-JSON: {text[:500]}") from None


# ---------- Actions ----------


def action_health(_: dict[str, Any]) -> dict[str, Any]:
    return _json_request("GET", "/_cluster/health")


def action_cluster_settings(_: dict[str, Any]) -> dict[str, Any]:
    """Report the analyzer packages the domain can see, for troubleshooting."""
    status, payload = request("GET", "/_cat/indices?format=json")
    return {
        "indices_status": status,
        "indices": json.loads(payload.decode("utf-8", "replace")) if status == 200 else None,
    }


def action_recreate_index(event: dict[str, Any]) -> dict[str, Any]:
    """Delete an index if present, then create it from a definition in S3.

    Creation failure is fatal and reported as such. If it were allowed to pass,
    the following bulk ingest would auto-create the index with dynamic mappings,
    typing every facet field as `text` instead of `keyword` and silently breaking
    faceting and sorting in the viewer.
    """
    index = _require_index(event.get("index"))
    key = event.get("definition_key")
    if not key:
        raise AdminError("'definition_key' is required")

    bucket = event.get("bucket") or INGEST_BUCKET
    if not bucket:
        raise AdminError("no S3 bucket configured")

    definition = _s3.get_object(Bucket=bucket, Key=key)["Body"].read()
    try:
        json.loads(definition)
    except ValueError:
        raise AdminError(f"s3://{bucket}/{key} is not valid JSON") from None

    delete_status, _ = request("DELETE", f"/{index}")
    if delete_status not in (200, 404):
        raise AdminError(f"DELETE /{index} returned HTTP {delete_status}")

    create_status, payload = request("PUT", f"/{index}", definition)
    if create_status != 200:
        raise AdminError(
            f"PUT /{index} returned HTTP {create_status}: "
            f"{payload.decode('utf-8', 'replace')[:2000]}"
        )

    return {
        "index": index,
        "deleted": delete_status == 200,
        "created": True,
        "definition": f"s3://{bucket}/{key}",
    }


def action_bulk_ingest(event: dict[str, Any], deadline: float) -> dict[str, Any]:
    """Stream an NDJSON bulk file from S3 into OpenSearch, resumably.

    Returns the offset to resume from when the time budget is exhausted. The
    caller re-invokes with that `start_byte` until `done` is true.
    """
    key = event.get("key")
    if not key:
        raise AdminError("'key' is required")

    bucket = event.get("bucket") or INGEST_BUCKET
    if not bucket:
        raise AdminError("no S3 bucket configured")

    start_byte = int(event.get("start_byte", 0))
    total_bytes = _s3.head_object(Bucket=bucket, Key=key)["ContentLength"]

    if start_byte >= total_bytes:
        return {
            "key": key,
            "done": True,
            "next_byte": total_bytes,
            "total_bytes": total_bytes,
            "indexed": 0,
            "failed": 0,
        }

    body = _s3.get_object(Bucket=bucket, Key=key, Range=f"bytes={start_byte}-")["Body"]

    offset = start_byte
    indexed = 0
    failed = 0
    first_error: str | None = None
    batch: list[bytes] = []
    batch_bytes = 0
    batch_start_offset = start_byte
    truncated = False

    def flush() -> tuple[int, int, str | None]:
        """Post the accumulated batch. Returns (indexed, failed, first_error)."""
        if not batch:
            return 0, 0, None

        payload = b"\n".join(batch) + b"\n"
        status, response = request("POST", "/_bulk", payload, "application/x-ndjson")
        if status != 200:
            raise AdminError(
                f"_bulk returned HTTP {status}: {response.decode('utf-8', 'replace')[:2000]}"
            )

        # The Bulk API returns 200 even when individual documents fail, so the
        # response body has to be inspected.
        result = json.loads(response)
        ok = 0
        bad = 0
        reason = None
        for item in result.get("items", []):
            outcome = next(iter(item.values()), {})
            if "error" in outcome:
                bad += 1
                if reason is None:
                    reason = json.dumps(outcome["error"])[:500]
            else:
                ok += 1
        return ok, bad, reason

    for line in body.iter_lines(chunk_size=1024 * 1024):
        batch.append(line)
        batch_bytes += len(line) + 1

        at_pair_boundary = len(batch) % 2 == 0
        if not at_pair_boundary:
            continue

        if len(batch) >= BULK_BATCH_LINES or batch_bytes >= BULK_BATCH_BYTES:
            ok, bad, reason = flush()
            indexed += ok
            failed += bad
            if first_error is None:
                first_error = reason

            offset = batch_start_offset + batch_bytes
            batch_start_offset = offset
            batch = []
            batch_bytes = 0

            if time.time() > deadline:
                truncated = True
                break

            if BULK_PAUSE_SECONDS:
                time.sleep(BULK_PAUSE_SECONDS)

    if not truncated:
        if len(batch) % 2:
            raise AdminError(
                f"s3://{bucket}/{key} has an odd number of lines; "
                "a bulk file must alternate action and document lines"
            )
        ok, bad, reason = flush()
        indexed += ok
        failed += bad
        if first_error is None:
            first_error = reason
        offset = batch_start_offset + batch_bytes

    body.close()

    done = not truncated or offset >= total_bytes
    result = {
        "key": key,
        "done": done,
        "next_byte": min(offset, total_bytes),
        "total_bytes": total_bytes,
        "indexed": indexed,
        "failed": failed,
    }
    if first_error:
        result["first_error"] = first_error
        logger.warning("%s document(s) failed in %s: %s", failed, key, first_error)
    return result


def action_refresh(event: dict[str, Any]) -> dict[str, Any]:
    index = _require_index(event.get("index"))
    return _json_request("POST", f"/{index}/_refresh")


def action_count(event: dict[str, Any]) -> dict[str, Any]:
    index = _require_index(event.get("index"))
    return _json_request("GET", f"/{index}/_count")


def action_analyze(event: dict[str, Any]) -> dict[str, Any]:
    """Run text through an index analyzer.

    This is the check that the Latin analyzer packages actually loaded. If the
    stemmer dictionary is missing the index would have failed to create, but if
    the package is stale the tokens come back unlemmatised, which is otherwise
    invisible.
    """
    index = _require_index(event.get("index"))
    text = event.get("text")
    if not text:
        raise AdminError("'text' is required")

    payload = json.dumps(
        {"analyzer": event.get("analyzer", "latin"), "text": str(text)[:2000]}
    ).encode("utf-8")
    result = _json_request("POST", f"/{index}/_analyze", payload)
    return {"tokens": [token.get("token") for token in result.get("tokens", [])]}


ACTIONS = {
    "health": action_health,
    "cluster_settings": action_cluster_settings,
    "recreate_index": action_recreate_index,
    "refresh": action_refresh,
    "count": action_count,
    "analyze": action_analyze,
}


def handler(event: dict[str, Any], context: Any) -> dict[str, Any]:
    """Direct-invoke entry point. Always returns a dict with an `ok` field."""
    action = (event or {}).get("action")

    try:
        if action == "bulk_ingest":
            remaining_ms = context.get_remaining_time_in_millis() if context else 900_000
            deadline = time.time() + max(remaining_ms / 1000.0 - RESERVE_SECONDS, 5)
            result = action_bulk_ingest(event, deadline)
        elif action in ACTIONS:
            result = ACTIONS[action](event)
        else:
            raise AdminError(
                f"unknown action {action!r}; expected one of "
                f"{sorted([*ACTIONS, 'bulk_ingest'])}"
            )
    except AdminError as error:
        logger.error("action %s failed: %s", action, error)
        return {"ok": False, "action": action, "error": str(error)}
    except Exception as error:  # noqa: BLE001 - surface any failure to the caller
        logger.exception("action %s raised", action)
        return {"ok": False, "action": action, "error": f"{type(error).__name__}: {error}"}

    return {"ok": True, "action": action, "result": result}
