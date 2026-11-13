"""Public functional checks. No VPC access or access to source images via AWS APIs.

The Lambda runtime supplies boto3; HTTP and validation use the standard library.
Each check publishes success independently so one failed dependency cannot hide
another. Missing metrics alarm too, covering scheduler/runtime/publisher failure.
"""

import json
import logging
import os
import time
from urllib.parse import quote
from urllib.request import Request, urlopen

LOG = logging.getLogger(__name__)
LOG.setLevel(logging.INFO)
NAMESPACE = "Rosa/FunctionalChecks"
MAX_BODY = 1024 * 1024


def fetch(url, body=None):
    headers = {"User-Agent": "Rosa-Functional-Check/1.0"}
    if body is not None:
        headers["Content-Type"] = "application/json"
    request = Request(url, data=body, headers=headers)
    with urlopen(request, timeout=10) as response:
        if response.status != 200:
            raise ValueError(f"Expected HTTP 200, received {response.status}")
        data = response.read(MAX_BODY + 1)
        if len(data) > MAX_BODY:
            raise ValueError("Response exceeded functional-check size limit")
        return data, response.headers.get("Content-Type", "").split(";")[0]


def check_search(base, index, manifest_id):
    field = "id" if index == "manifest" else "manifest_id"
    query = {"size": 1, "query": {"term": {field: manifest_id}}}
    data, _ = fetch(f"{base}/{index}/_search", json.dumps(query).encode())
    result = json.loads(data)
    if result.get("timed_out") is not False or result.get("error"):
        raise ValueError("Search timed out or returned an error")
    shards = result.get("_shards", {})
    if shards.get("failed") != 0 or shards.get("successful", 0) < 1:
        raise ValueError("Search did not complete successfully on its shards")
    hits = result.get("hits", {}).get("hits", [])
    if not hits or hits[0].get("_source", {}).get(field) != manifest_id:
        raise ValueError(f"Known manifest missing from {index} search results")


def check_image(base, identifier):
    image_url = f"{base}/{quote(identifier, safe='')}"
    data, _ = fetch(f"{image_url}/info.json")
    info = json.loads(data)
    if (info.get("type") != "ImageService3" or info.get("id") != image_url
            or not isinstance(info.get("width"), int) or info["width"] <= 0
            or not isinstance(info.get("height"), int) or info["height"] <= 0):
        raise ValueError("Invalid IIIF Image API 3 metadata")
    data, content_type = fetch(f"{image_url}/full/80,/0/default.jpg")
    if content_type != "image/jpeg" or len(data) < 100 or not (
            data.startswith(b"\xff\xd8") and data.endswith(b"\xff\xd9")):
        raise ValueError("Image endpoint did not return JPEG content")


def handler(event, context):
    import boto3

    cloudwatch = boto3.client("cloudwatch")
    base = os.environ["SEARCH_API_ENDPOINT"].rstrip("/")
    manifest_id = os.environ["MANIFEST_ID"]
    checks = {
        "search-manifest": lambda: check_search(base, "manifest", manifest_id),
        "search-canvas": lambda: check_search(base, "canvas", manifest_id),
    }
    if os.environ["IMAGE_ENABLED"] == "true":
        checks["image-delivery"] = lambda: check_image(
            os.environ["IMAGE_BASE_URL"].rstrip("/"), os.environ["IMAGE_IDENTIFIER"])
    outcomes = {}
    for name, check in checks.items():
        started = time.monotonic()
        success = 0
        try:
            check()
            success = 1
        except Exception:
            LOG.exception("Functional check %s failed", name)
        elapsed = (time.monotonic() - started) * 1000
        dimensions = [
            {"Name": "Deployment", "Value": os.environ["DEPLOYMENT"]},
            {"Name": "Check", "Value": name},
        ]
        cloudwatch.put_metric_data(Namespace=NAMESPACE, MetricData=[
            {"MetricName": "Success", "Dimensions": dimensions,
             "Value": success, "Unit": "Count"},
            {"MetricName": "Duration", "Dimensions": dimensions,
             "Value": elapsed, "Unit": "Milliseconds"},
        ])
        outcomes[name] = bool(success)
        LOG.info("check=%s success=%s duration_ms=%.1f", name, success, elapsed)
    return outcomes
