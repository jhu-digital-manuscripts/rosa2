"""Search proxy for the rosa2 JHSearch API.

The AWS managed OpenSearch domain has no public endpoint and requires SigV4
authentication, neither of which a browser can satisfy. This function sits behind
API Gateway, validates the incoming query, signs it with the Lambda execution
role's credentials, and forwards it to the domain from inside the VPC.

Only read traffic is allowed. A request must be a POST to one of three `_search`
paths, and the body must parse as a query that rosa-viewer could plausibly have
produced. Anything that could mutate data, execute code, read outside the two
rosa indexes, or consume an unbounded amount of cluster CPU is rejected here
rather than forwarded.

The checks are deliberately cheap: a size cap, a JSON parse, and a single
recursive walk of the parsed body. There are no network calls and no shared
state, so validation adds well under a millisecond to a request.

Environment variables:
    OPENSEARCH_ENDPOINT   Domain endpoint host, no scheme and no trailing slash.
    OPENSEARCH_REGION     Region of the domain. Defaults to AWS_REGION.
    MAX_BODY_BYTES        Reject request bodies larger than this. Default 16384.
    MAX_RESULT_WINDOW     Cap on from + size. Default 10000.
    MAX_PAGE_SIZE         Cap on size. Default 100.
    SEARCH_TIMEOUT        Per-shard timeout injected into every query. Default 10s.
    TERMINATE_AFTER       Per-shard document cap injected into every query.
    RATE_LIMIT_PER_MINUTE Best-effort per-IP request budget. 0 disables.
    RATE_LIMIT_BURST      Best-effort per-IP burst allowance.
    UPSTREAM_TIMEOUT      Socket timeout in seconds when calling OpenSearch.
    LOG_REJECTIONS        Log rejected queries when "true". Default true.
"""

from __future__ import annotations

import json
import logging
import os
import time
import urllib.error
import urllib.request
from collections import OrderedDict
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


OPENSEARCH_ENDPOINT = os.environ.get("OPENSEARCH_ENDPOINT", "").replace("https://", "").rstrip("/")
OPENSEARCH_REGION = os.environ.get("OPENSEARCH_REGION") or os.environ.get("AWS_REGION", "us-east-1")

MAX_BODY_BYTES = _int_env("MAX_BODY_BYTES", 16384)
MAX_RESULT_WINDOW = _int_env("MAX_RESULT_WINDOW", 10000)
MAX_PAGE_SIZE = _int_env("MAX_PAGE_SIZE", 100)
SEARCH_TIMEOUT = os.environ.get("SEARCH_TIMEOUT", "10s")
TERMINATE_AFTER = _int_env("TERMINATE_AFTER", 200000)
RATE_LIMIT_PER_MINUTE = _int_env("RATE_LIMIT_PER_MINUTE", 120)
RATE_LIMIT_BURST = _int_env("RATE_LIMIT_BURST", 30)
UPSTREAM_TIMEOUT = _int_env("UPSTREAM_TIMEOUT", 20)
LOG_REJECTIONS = os.environ.get("LOG_REJECTIONS", "true").lower() == "true"

# Structural limits. These bound the cost of a single query rather than trying to
# enumerate every abusive shape. The numbers are comfortably above anything
# rosa-viewer emits: its widest query is a multi_match over ~8 language
# sub-fields with a handful of facet filters and ~12 terms aggregations.
MAX_JSON_DEPTH = 16
MAX_JSON_NODES = 2000
MAX_AGG_COUNT = 32
MAX_AGG_DEPTH = 2
MAX_AGG_BUCKETS = 500
MAX_HIGHLIGHT_FIELDS = 64
MAX_SORT_KEYS = 4
MAX_TERMS_VALUES = 256
MAX_QUERY_TEXT = 512
MAX_MATCH_FIELDS = 64
MAX_BOOL_CLAUSES = 64
MAX_TOTAL_CLAUSES = 256

# Requests to "/_search" would otherwise hit every index on the domain,
# including the security plugin's own system indexes. Rewriting the path pins
# every request to the two rosa indexes.
PATH_ROUTES = {
    "/_search": "/manifest,canvas/_search",
    "/manifest/_search": "/manifest/_search",
    "/canvas/_search": "/canvas/_search",
}

ALLOWED_TOP_LEVEL = frozenset(
    {
        "query",
        "from",
        "size",
        "sort",
        "aggs",
        "aggregations",
        "highlight",
        "_source",
        "track_total_hits",
        "min_score",
        "post_filter",
        "search_after",
    }
)

# Compound clauses whose children are themselves queries.
COMPOUND_CLAUSES = frozenset({"bool", "constant_score", "dis_max"})

# Leaf clauses. Everything rosa-viewer produces is in here.
LEAF_CLAUSES = frozenset(
    {
        "match",
        "match_all",
        "match_none",
        "match_phrase",
        "match_phrase_prefix",
        "multi_match",
        "term",
        "terms",
        "range",
        "exists",
        "prefix",
        "ids",
        "simple_query_string",
    }
)

ALLOWED_AGG_TYPES = frozenset(
    {
        "terms",
        "histogram",
        "date_histogram",
        "range",
        "date_range",
        "filter",
        "missing",
        "min",
        "max",
        "avg",
        "sum",
        "stats",
        "value_count",
        "cardinality",
    }
)

# Keys that must not appear anywhere in the body, at any depth. These cover code
# execution, cross-index and cross-document joins, response-shape controls that
# leak mappings, and clause types whose cost is not bounded by the structural
# limits above. None of these collide with a field name in the rosa mappings,
# so checking every key is safe.
DENIED_KEYS = frozenset(
    {
        # Code execution.
        "script",
        "script_score",
        "script_fields",
        "scripted_metric",
        "runtime_mappings",
        "source",
        "lang",
        "inline",
        "stored",
        # Unbounded or expensive clause types.
        "query_string",
        "regexp",
        "wildcard",
        "fuzzy",
        "more_like_this",
        "function_score",
        "terms_set",
        "rank_feature",
        "distance_feature",
        "pinned",
        "span_term",
        "span_near",
        "span_or",
        "span_not",
        "span_first",
        "span_multi",
        "span_containing",
        "span_within",
        # Vector and ML search. Not installed, but fail loudly rather than oddly.
        "knn",
        "neural",
        "percolate",
        # Joins across documents or indexes.
        "nested",
        "has_child",
        "has_parent",
        "parent_id",
        "join",
        # Response controls that leak mappings or internals.
        "profile",
        "explain",
        "docvalue_fields",
        "stored_fields",
        "indices_boost",
        "collapse",
        "rescore",
        "inner_hits",
        "suggest",
        "params",
        "ext",
        "pit",
        "slice",
        "aggs_filter",
    }
)


class RequestRejected(Exception):
    """A request failed validation and must not be forwarded."""

    def __init__(self, message: str, status: int = 400) -> None:
        super().__init__(message)
        self.message = message
        self.status = status


class _RateLimiter:
    """Best-effort per-IP token bucket.

    State lives in the execution environment, so the effective limit scales with
    the number of warm containers and resets on cold start. It is a cheap brake
    on a single abusive client, not an accounting mechanism. The authoritative
    global limit is the API Gateway stage throttle, which is enforced before this
    function is ever invoked.
    """

    MAX_TRACKED_IPS = 10000

    def __init__(self, per_minute: int, burst: int) -> None:
        self.rate = per_minute / 60.0
        self.capacity = max(burst, 1)
        self.enabled = per_minute > 0
        self._buckets: OrderedDict[str, tuple[float, float]] = OrderedDict()

    def allow(self, key: str) -> bool:
        if not self.enabled or not key:
            return True

        now = time.monotonic()
        tokens, last_seen = self._buckets.get(key, (float(self.capacity), now))
        tokens = min(self.capacity, tokens + (now - last_seen) * self.rate)

        if tokens < 1.0:
            self._buckets[key] = (tokens, now)
            self._buckets.move_to_end(key)
            return False

        self._buckets[key] = (tokens - 1.0, now)
        self._buckets.move_to_end(key)

        while len(self._buckets) > self.MAX_TRACKED_IPS:
            self._buckets.popitem(last=False)

        return True


_rate_limiter = _RateLimiter(RATE_LIMIT_PER_MINUTE, RATE_LIMIT_BURST)
_credentials = boto3.Session().get_credentials()


# ---------- Validation ----------


def _walk(node: Any, depth: int, counter: list[int]) -> None:
    """Enforce depth, node count, and the global key denylist."""
    if depth > MAX_JSON_DEPTH:
        raise RequestRejected(f"query nests deeper than {MAX_JSON_DEPTH} levels")

    counter[0] += 1
    if counter[0] > MAX_JSON_NODES:
        raise RequestRejected(f"query contains more than {MAX_JSON_NODES} elements")

    if isinstance(node, dict):
        for key, value in node.items():
            if not isinstance(key, str):
                raise RequestRejected("object keys must be strings")
            if key in DENIED_KEYS:
                raise RequestRejected(f"'{key}' is not permitted")
            _walk(value, depth + 1, counter)
    elif isinstance(node, list):
        for item in node:
            _walk(item, depth + 1, counter)
    elif isinstance(node, str):
        if len(node) > MAX_QUERY_TEXT:
            raise RequestRejected(f"string values must be {MAX_QUERY_TEXT} characters or fewer")


def _validate_query(node: Any, clauses: list[int]) -> None:
    """Validate a node appearing in a query position."""
    if not isinstance(node, dict):
        raise RequestRejected("query clauses must be objects")
    if len(node) != 1:
        raise RequestRejected("each query clause must have exactly one type")

    name, body = next(iter(node.items()))

    clauses[0] += 1
    if clauses[0] > MAX_TOTAL_CLAUSES:
        raise RequestRejected(f"query contains more than {MAX_TOTAL_CLAUSES} clauses")

    if name in COMPOUND_CLAUSES:
        _validate_compound(name, body, clauses)
    elif name in LEAF_CLAUSES:
        _validate_leaf(name, body)
    else:
        raise RequestRejected(f"query type '{name}' is not permitted")


def _validate_compound(name: str, body: Any, clauses: list[int]) -> None:
    if not isinstance(body, dict):
        raise RequestRejected(f"'{name}' must be an object")

    if name == "bool":
        allowed = {"must", "must_not", "should", "filter", "minimum_should_match", "boost"}
        for key, value in body.items():
            if key not in allowed:
                raise RequestRejected(f"'bool.{key}' is not permitted")
            if key in ("minimum_should_match", "boost"):
                continue
            children = value if isinstance(value, list) else [value]
            if len(children) > MAX_BOOL_CLAUSES:
                raise RequestRejected(f"'bool.{key}' has more than {MAX_BOOL_CLAUSES} clauses")
            for child in children:
                _validate_query(child, clauses)
    elif name == "constant_score":
        for key, value in body.items():
            if key == "filter":
                _validate_query(value, clauses)
            elif key != "boost":
                raise RequestRejected(f"'constant_score.{key}' is not permitted")
    else:  # dis_max
        for key, value in body.items():
            if key == "queries":
                if not isinstance(value, list):
                    raise RequestRejected("'dis_max.queries' must be an array")
                if len(value) > MAX_BOOL_CLAUSES:
                    raise RequestRejected(
                        f"'dis_max.queries' has more than {MAX_BOOL_CLAUSES} clauses"
                    )
                for child in value:
                    _validate_query(child, clauses)
            elif key not in ("tie_breaker", "boost"):
                raise RequestRejected(f"'dis_max.{key}' is not permitted")


def _validate_leaf(name: str, body: Any) -> None:
    if name in ("match_all", "match_none"):
        if body not in ({}, None) and not isinstance(body, dict):
            raise RequestRejected(f"'{name}' must be an empty object")
        return

    if name == "multi_match":
        if not isinstance(body, dict):
            raise RequestRejected("'multi_match' must be an object")
        fields = body.get("fields")
        if fields is not None:
            if not isinstance(fields, list):
                raise RequestRejected("'multi_match.fields' must be an array")
            if len(fields) > MAX_MATCH_FIELDS:
                raise RequestRejected(
                    f"'multi_match.fields' lists more than {MAX_MATCH_FIELDS} fields"
                )
            for field in fields:
                if not isinstance(field, str):
                    raise RequestRejected("'multi_match.fields' entries must be strings")
                _reject_leading_wildcard(field)
        return

    if name == "terms":
        if not isinstance(body, dict):
            raise RequestRejected("'terms' must be an object")
        for key, value in body.items():
            if key == "boost":
                continue
            # A dict here is a terms lookup, which reads a document from another
            # index. Only inline value lists are allowed.
            if not isinstance(value, list):
                raise RequestRejected("'terms' requires an inline array of values")
            if len(value) > MAX_TERMS_VALUES:
                raise RequestRejected(f"'terms' lists more than {MAX_TERMS_VALUES} values")
            for item in value:
                if isinstance(item, (dict, list)):
                    raise RequestRejected("'terms' values must be scalars")
        return

    if name == "ids":
        if not isinstance(body, dict):
            raise RequestRejected("'ids' must be an object")
        values = body.get("values")
        if not isinstance(values, list) or len(values) > MAX_TERMS_VALUES:
            raise RequestRejected(f"'ids.values' must be an array of at most {MAX_TERMS_VALUES}")
        return

    if name == "prefix":
        if not isinstance(body, dict):
            raise RequestRejected("'prefix' must be an object")
        for key, value in body.items():
            text = value.get("value") if isinstance(value, dict) else value
            if isinstance(text, str) and len(text.strip()) < 2:
                raise RequestRejected("'prefix' requires at least two characters")
        return

    if name == "simple_query_string":
        if not isinstance(body, dict):
            raise RequestRejected("'simple_query_string' must be an object")
        for field in body.get("fields", []) or []:
            if isinstance(field, str):
                _reject_leading_wildcard(field)
        return

    if not isinstance(body, dict):
        raise RequestRejected(f"'{name}' must be an object")


def _reject_leading_wildcard(field: str) -> None:
    """Allow 'marginalia.*' but not '*' or '*.la'.

    rosa-viewer expands fields that have language sub-fields into
    'fieldname.*'. A pattern that begins with a wildcard would instead fan out
    across every field in the mapping.
    """
    if field.startswith("*") or field.startswith("."):
        raise RequestRejected(f"field pattern '{field}' may not start with a wildcard")


def _validate_aggs(aggs: Any, depth: int, counter: list[int]) -> None:
    if not isinstance(aggs, dict):
        raise RequestRejected("aggregations must be an object")
    if depth > MAX_AGG_DEPTH:
        raise RequestRejected(f"aggregations nest deeper than {MAX_AGG_DEPTH} levels")

    for name, spec in aggs.items():
        counter[0] += 1
        if counter[0] > MAX_AGG_COUNT:
            raise RequestRejected(f"more than {MAX_AGG_COUNT} aggregations requested")
        if not isinstance(spec, dict):
            raise RequestRejected(f"aggregation '{name}' must be an object")

        for key, value in spec.items():
            if key in ("aggs", "aggregations"):
                _validate_aggs(value, depth + 1, counter)
                continue
            if key == "meta":
                continue
            if key not in ALLOWED_AGG_TYPES:
                raise RequestRejected(f"aggregation type '{key}' is not permitted")
            if isinstance(value, dict):
                size = value.get("size")
                if isinstance(size, int) and size > MAX_AGG_BUCKETS:
                    raise RequestRejected(
                        f"aggregation '{name}' requests more than {MAX_AGG_BUCKETS} buckets"
                    )
                if key == "filter":
                    _validate_query(value, [0])


def _validate_source(value: Any) -> None:
    if isinstance(value, (bool, str)):
        return
    if isinstance(value, list):
        if all(isinstance(item, str) for item in value):
            return
        raise RequestRejected("'_source' entries must be strings")
    if isinstance(value, dict):
        for key, entries in value.items():
            if key not in ("includes", "excludes"):
                raise RequestRejected(f"'_source.{key}' is not permitted")
            if not isinstance(entries, list) or not all(isinstance(e, str) for e in entries):
                raise RequestRejected(f"'_source.{key}' must be an array of strings")
        return
    raise RequestRejected("'_source' must be a boolean, string, array, or object")


def validate_body(raw: bytes) -> dict[str, Any]:
    """Parse and validate a search body, returning the query to forward.

    Raises RequestRejected with a client-safe message on any violation.
    """
    if len(raw) > MAX_BODY_BYTES:
        raise RequestRejected(f"request body exceeds {MAX_BODY_BYTES} bytes", status=413)
    if not raw.strip():
        raise RequestRejected("request body is empty")

    try:
        body = json.loads(raw)
    except (ValueError, UnicodeDecodeError):
        raise RequestRejected("request body is not valid JSON") from None

    if not isinstance(body, dict):
        raise RequestRejected("request body must be a JSON object")

    unknown = set(body) - ALLOWED_TOP_LEVEL
    if unknown:
        raise RequestRejected(f"unsupported search parameters: {', '.join(sorted(unknown))}")

    _walk(body, 0, [0])

    size = body.get("size", 10)
    if not isinstance(size, int) or isinstance(size, bool) or size < 0:
        raise RequestRejected("'size' must be a non-negative integer")
    if size > MAX_PAGE_SIZE:
        raise RequestRejected(f"'size' may not exceed {MAX_PAGE_SIZE}")

    offset = body.get("from", 0)
    if not isinstance(offset, int) or isinstance(offset, bool) or offset < 0:
        raise RequestRejected("'from' must be a non-negative integer")
    if offset + size > MAX_RESULT_WINDOW:
        raise RequestRejected(f"'from' + 'size' may not exceed {MAX_RESULT_WINDOW}")

    if "query" in body:
        _validate_query(body["query"], [0])
    if "post_filter" in body:
        _validate_query(body["post_filter"], [0])

    agg_counter = [0]
    for key in ("aggs", "aggregations"):
        if key in body:
            _validate_aggs(body[key], 1, agg_counter)

    if "sort" in body:
        sort = body["sort"]
        entries = sort if isinstance(sort, list) else [sort]
        if len(entries) > MAX_SORT_KEYS:
            raise RequestRejected(f"'sort' may list at most {MAX_SORT_KEYS} keys")
        for entry in entries:
            if isinstance(entry, str):
                _reject_leading_wildcard(entry)
            elif isinstance(entry, dict):
                for field in entry:
                    _reject_leading_wildcard(field)
            else:
                raise RequestRejected("'sort' entries must be strings or objects")

    if "highlight" in body:
        highlight = body["highlight"]
        if not isinstance(highlight, dict):
            raise RequestRejected("'highlight' must be an object")
        fields = highlight.get("fields", {})
        if not isinstance(fields, dict):
            raise RequestRejected("'highlight.fields' must be an object")
        if len(fields) > MAX_HIGHLIGHT_FIELDS:
            raise RequestRejected(
                f"'highlight.fields' names more than {MAX_HIGHLIGHT_FIELDS} fields"
            )
        for field in fields:
            _reject_leading_wildcard(field)

    if "_source" in body:
        _validate_source(body["_source"])

    if "min_score" in body and not isinstance(body["min_score"], (int, float)):
        raise RequestRejected("'min_score' must be a number")

    track = body.get("track_total_hits")
    if track is not None:
        if isinstance(track, bool):
            pass
        elif isinstance(track, int):
            body["track_total_hits"] = min(track, MAX_RESULT_WINDOW)
        else:
            raise RequestRejected("'track_total_hits' must be a boolean or integer")

    if "search_after" in body and not isinstance(body["search_after"], list):
        raise RequestRejected("'search_after' must be an array")

    # Server-side safety net. Neither value affects results for a corpus this
    # size; they bound the damage from a pathological query.
    body["timeout"] = SEARCH_TIMEOUT
    body["terminate_after"] = TERMINATE_AFTER

    return body


# ---------- Upstream call ----------


def forward(path: str, body: dict[str, Any]) -> tuple[int, bytes]:
    """Sign and forward a validated query to OpenSearch."""
    url = f"https://{OPENSEARCH_ENDPOINT}{path}"
    payload = json.dumps(body, separators=(",", ":")).encode("utf-8")

    signed = AWSRequest(
        method="POST",
        url=url,
        data=payload,
        headers={"Content-Type": "application/json", "Host": OPENSEARCH_ENDPOINT},
    )
    SigV4Auth(_credentials, "es", OPENSEARCH_REGION).add_auth(signed)

    request = urllib.request.Request(  # noqa: S310 - fixed https scheme
        url, data=payload, headers=dict(signed.headers), method="POST"
    )

    try:
        with urllib.request.urlopen(request, timeout=UPSTREAM_TIMEOUT) as response:
            return response.status, response.read()
    except urllib.error.HTTPError as error:
        return error.code, error.read()
    except urllib.error.URLError as error:
        logger.error("OpenSearch unreachable: %s", error.reason)
        raise RequestRejected("search backend unavailable", status=502) from None
    except TimeoutError:
        raise RequestRejected("search backend timed out", status=504) from None


# ---------- Handler ----------


def _response(status: int, payload: dict[str, Any] | bytes) -> dict[str, Any]:
    raw = payload if isinstance(payload, bytes) else json.dumps(payload).encode("utf-8")
    return {
        "statusCode": status,
        "headers": {
            "Content-Type": "application/json",
            "Cache-Control": "no-store",
            "X-Content-Type-Options": "nosniff",
        },
        "body": raw.decode("utf-8"),
    }


def handler(event: dict[str, Any], context: Any) -> dict[str, Any]:  # noqa: ARG001
    """API Gateway HTTP API (payload format 2.0) entry point."""
    if not OPENSEARCH_ENDPOINT:
        logger.error("OPENSEARCH_ENDPOINT is not configured")
        return _response(500, {"error": "search is not configured"})

    http = event.get("requestContext", {}).get("http", {})
    method = http.get("method", "")
    path = event.get("rawPath", "")
    source_ip = http.get("sourceIp", "")

    # API Gateway answers preflight from its own CORS configuration; this is only
    # reached if that configuration is missing.
    if method == "OPTIONS":
        return _response(204, b"")

    if method != "POST":
        return _response(405, {"error": "only POST is supported"})

    upstream_path = PATH_ROUTES.get(path)
    if upstream_path is None:
        return _response(404, {"error": "unknown search path"})

    if not _rate_limiter.allow(source_ip):
        logger.warning("rate limit exceeded for %s", source_ip)
        return _response(429, {"error": "too many requests"})

    raw = event.get("body") or ""
    if event.get("isBase64Encoded"):
        import base64

        try:
            raw_bytes = base64.b64decode(raw)
        except ValueError:
            return _response(400, {"error": "request body is not valid base64"})
    else:
        raw_bytes = raw.encode("utf-8")

    try:
        body = validate_body(raw_bytes)
    except RequestRejected as rejection:
        if LOG_REJECTIONS:
            logger.warning(
                "rejected %s from %s: %s | body=%s",
                path,
                source_ip,
                rejection.message,
                raw_bytes[:1024].decode("utf-8", "replace"),
            )
        return _response(rejection.status, {"error": rejection.message})

    try:
        status, payload = forward(upstream_path, body)
    except RequestRejected as failure:
        return _response(failure.status, {"error": failure.message})

    if status != 200:
        # OpenSearch error bodies carry mappings, shard layout, and stack traces.
        # Log them for operators and return something opaque to the caller.
        logger.error(
            "OpenSearch returned %s for %s: %s",
            status,
            upstream_path,
            payload[:2048].decode("utf-8", "replace"),
        )
        message = "malformed search request" if 400 <= status < 500 else "search backend error"
        return _response(status if status in (400, 404, 429) else 502, {"error": message})

    return _response(200, payload)
