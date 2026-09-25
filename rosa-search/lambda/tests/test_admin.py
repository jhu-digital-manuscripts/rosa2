"""Tests for the admin Lambda.

The focus is bulk_ingest: it must never split an action/document pair across two
_bulk requests, its byte accounting must let a later invocation resume exactly
where the previous one stopped, and it must reach the end of the object without
skipping or repeating documents.

Run with:  python3 -m unittest discover -s rosa-search/lambda -v
"""

from __future__ import annotations

import json
import sys
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))

from _stubs import FAKE_S3, load_lambda  # noqa: E402

_fake_s3 = FAKE_S3
app = load_lambda("admin", "rosa_search_admin")

app._s3 = _fake_s3
app.OPENSEARCH_ENDPOINT = "search-test.us-east-1.es.amazonaws.com"
app.BULK_PAUSE_SECONDS = 0

BUCKET = "jhu-library-images"


def make_bulk(count: int, index: str = "canvas") -> bytes:
    """Build a valid NDJSON bulk file with `count` documents."""
    lines = []
    for i in range(count):
        lines.append(json.dumps({"index": {"_index": index, "_id": f"aor.Ha2.{i:04d}"}}))
        lines.append(json.dumps({"id": f"aor.Ha2.{i:04d}", "page_num": i}))
    return ("\n".join(lines) + "\n").encode("utf-8")


class BulkIngest(unittest.TestCase):
    def setUp(self):
        self.requests = []
        self._original_request = app.request

        def fake_request(method, path, body=None, content_type="application/json"):
            self.requests.append((method, path, body))
            if path == "/_bulk":
                docs = (body or b"").count(b'"_id"')
                items = [{"index": {"_id": str(i), "status": 201}} for i in range(docs)]
                return 200, json.dumps({"errors": False, "items": items}).encode()
            return 200, b"{}"

        app.request = fake_request
        _fake_s3.objects.clear()

    def tearDown(self):
        app.request = self._original_request

    def ingest_all(self, key, batch_lines=None, deadline=None):
        """Drive bulk_ingest to completion the way init-opensearch.sh does."""
        if batch_lines:
            app.BULK_BATCH_LINES = batch_lines
        start = 0
        results = []
        for _ in range(200):
            result = app.action_bulk_ingest(
                {"bucket": BUCKET, "key": key, "start_byte": start},
                deadline if deadline is not None else float("inf"),
            )
            results.append(result)
            if result["done"]:
                break
            self.assertGreater(result["next_byte"], start, "resume offset must advance")
            start = result["next_byte"]
        return results

    def test_every_bulk_request_has_even_line_count(self):
        _fake_s3.put(BUCKET, "aor.bulk.json", make_bulk(50))
        self.ingest_all("aor.bulk.json", batch_lines=10)
        bulk_bodies = [body for method, path, body in self.requests if path == "/_bulk"]
        self.assertTrue(bulk_bodies)
        for body in bulk_bodies:
            lines = body.rstrip(b"\n").split(b"\n")
            self.assertEqual(len(lines) % 2, 0, "a bulk batch split an action/document pair")

    def test_action_and_document_lines_stay_paired(self):
        _fake_s3.put(BUCKET, "aor.bulk.json", make_bulk(20))
        self.ingest_all("aor.bulk.json", batch_lines=4)
        for method, path, body in self.requests:
            if path != "/_bulk":
                continue
            lines = body.rstrip(b"\n").split(b"\n")
            for i, line in enumerate(lines):
                parsed = json.loads(line)
                if i % 2 == 0:
                    self.assertIn("index", parsed, "even lines must be action lines")
                else:
                    self.assertIn("id", parsed, "odd lines must be document lines")

    def test_all_documents_indexed_exactly_once(self):
        data = make_bulk(101)
        _fake_s3.put(BUCKET, "aor.bulk.json", data)
        results = self.ingest_all("aor.bulk.json", batch_lines=10)
        total = sum(r["indexed"] for r in results)
        self.assertEqual(total, 101)

    def test_consumes_whole_object(self):
        data = make_bulk(37)
        _fake_s3.put(BUCKET, "aor.bulk.json", data)
        results = self.ingest_all("aor.bulk.json", batch_lines=8)
        self.assertTrue(results[-1]["done"])
        self.assertEqual(results[-1]["next_byte"], len(data))
        self.assertEqual(results[-1]["total_bytes"], len(data))

    def test_resume_covers_remainder_without_gaps(self):
        """Stop early, then resume, and confirm the union is the whole file."""
        data = make_bulk(60)
        _fake_s3.put(BUCKET, "aor.bulk.json", data)
        app.BULK_BATCH_LINES = 4

        # A deadline already in the past makes the loop stop after one batch.
        first = app.action_bulk_ingest(
            {"bucket": BUCKET, "key": "aor.bulk.json", "start_byte": 0}, 0.0
        )
        self.assertFalse(first["done"])
        self.assertGreater(first["next_byte"], 0)
        self.assertLess(first["next_byte"], len(data))

        # The resumed read must start exactly at a line boundary.
        self.assertEqual(data[first["next_byte"] - 1:first["next_byte"]], b"\n")

        rest = self.ingest_all("aor.bulk.json")  # no deadline
        remaining = app.action_bulk_ingest(
            {"bucket": BUCKET, "key": "aor.bulk.json", "start_byte": first["next_byte"]},
            float("inf"),
        )
        self.assertTrue(remaining["done"])
        self.assertEqual(first["indexed"] + remaining["indexed"], 60)
        self.assertTrue(rest)

    def test_start_beyond_end_is_a_noop(self):
        data = make_bulk(5)
        _fake_s3.put(BUCKET, "aor.bulk.json", data)
        result = app.action_bulk_ingest(
            {"bucket": BUCKET, "key": "aor.bulk.json", "start_byte": len(data)}, float("inf")
        )
        self.assertTrue(result["done"])
        self.assertEqual(result["indexed"], 0)
        self.assertEqual(self.requests, [])

    def test_odd_line_count_is_fatal(self):
        _fake_s3.put(BUCKET, "bad.bulk.json", make_bulk(3) + b'{"index":{"_index":"canvas"}}\n')
        with self.assertRaises(app.AdminError) as caught:
            app.action_bulk_ingest(
                {"bucket": BUCKET, "key": "bad.bulk.json", "start_byte": 0}, float("inf")
            )
        self.assertIn("odd number of lines", str(caught.exception))

    def test_document_level_failures_reported(self):
        _fake_s3.put(BUCKET, "aor.bulk.json", make_bulk(4))

        def failing_bulk(method, path, body=None, content_type="application/json"):
            if path == "/_bulk":
                return 200, json.dumps(
                    {
                        "errors": True,
                        "items": [
                            {"index": {"status": 201}},
                            {"index": {"error": {"reason": "mapper_parsing_exception"}}},
                            {"index": {"status": 201}},
                            {"index": {"error": {"reason": "mapper_parsing_exception"}}},
                        ],
                    }
                ).encode()
            return 200, b"{}"

        app.request = failing_bulk
        result = app.action_bulk_ingest(
            {"bucket": BUCKET, "key": "aor.bulk.json", "start_byte": 0}, float("inf")
        )
        self.assertEqual(result["failed"], 2)
        self.assertEqual(result["indexed"], 2)
        self.assertIn("mapper_parsing_exception", result["first_error"])

    def test_http_error_is_fatal(self):
        _fake_s3.put(BUCKET, "aor.bulk.json", make_bulk(2))

        def broken(method, path, body=None, content_type="application/json"):
            return 429, b'{"error":"circuit_breaking_exception"}'

        app.request = broken
        with self.assertRaises(app.AdminError) as caught:
            app.action_bulk_ingest(
                {"bucket": BUCKET, "key": "aor.bulk.json", "start_byte": 0}, float("inf")
            )
        self.assertIn("HTTP 429", str(caught.exception))

    def test_batch_lines_forced_even(self):
        self.assertEqual(app.BULK_BATCH_LINES % 2, 0)


class RecreateIndex(unittest.TestCase):
    def setUp(self):
        self.requests = []
        self._original_request = app.request

        def fake_request(method, path, body=None, content_type="application/json"):
            self.requests.append((method, path))
            return 200, b'{"acknowledged":true}'

        app.request = fake_request
        _fake_s3.objects.clear()
        _fake_s3.put(BUCKET, "canvas.json", b'{"settings":{},"mappings":{}}')

    def tearDown(self):
        app.request = self._original_request

    def test_deletes_then_creates(self):
        result = app.action_recreate_index(
            {"bucket": BUCKET, "index": "canvas", "definition_key": "canvas.json"}
        )
        self.assertEqual(self.requests, [("DELETE", "/canvas"), ("PUT", "/canvas")])
        self.assertTrue(result["created"])

    def test_missing_index_is_not_an_error(self):
        def delete_404(method, path, body=None, content_type="application/json"):
            self.requests.append((method, path))
            if method == "DELETE":
                return 404, b'{"error":"index_not_found_exception"}'
            return 200, b'{"acknowledged":true}'

        app.request = delete_404
        result = app.action_recreate_index(
            {"bucket": BUCKET, "index": "canvas", "definition_key": "canvas.json"}
        )
        self.assertFalse(result["deleted"])
        self.assertTrue(result["created"])

    def test_create_failure_is_fatal(self):
        def put_fails(method, path, body=None, content_type="application/json"):
            if method == "PUT":
                return 400, b'{"error":"IOException while reading mappings_path"}'
            return 200, b"{}"

        app.request = put_fails
        with self.assertRaises(app.AdminError) as caught:
            app.action_recreate_index(
                {"bucket": BUCKET, "index": "canvas", "definition_key": "canvas.json"}
            )
        self.assertIn("mappings_path", str(caught.exception))

    def test_unknown_index_rejected(self):
        with self.assertRaises(app.AdminError):
            app.action_recreate_index(
                {"bucket": BUCKET, "index": ".opendistro_security", "definition_key": "canvas.json"}
            )

    def test_invalid_definition_rejected(self):
        _fake_s3.put(BUCKET, "broken.json", b"not json")
        with self.assertRaises(app.AdminError) as caught:
            app.action_recreate_index(
                {"bucket": BUCKET, "index": "canvas", "definition_key": "broken.json"}
            )
        self.assertIn("not valid JSON", str(caught.exception))
        self.assertEqual(self.requests, [], "must not touch the index if the definition is bad")


class HandlerContract(unittest.TestCase):
    """The script checks `ok`, so failures must be reported, not raised."""

    class _Context:
        @staticmethod
        def get_remaining_time_in_millis():
            return 900_000

    def test_unknown_action(self):
        result = app.handler({"action": "drop_everything"}, self._Context())
        self.assertFalse(result["ok"])
        self.assertIn("unknown action", result["error"])

    def test_missing_action(self):
        self.assertFalse(app.handler({}, self._Context())["ok"])

    def test_admin_error_returned_not_raised(self):
        result = app.handler({"action": "count", "index": "nope"}, self._Context())
        self.assertFalse(result["ok"])
        self.assertIn("index must be one of", result["error"])

    def test_successful_action_shape(self):
        original = app.request
        app.request = lambda *a, **k: (200, b'{"count":1234}')
        try:
            result = app.handler({"action": "count", "index": "canvas"}, self._Context())
        finally:
            app.request = original
        self.assertTrue(result["ok"])
        self.assertEqual(result["result"]["count"], 1234)


if __name__ == "__main__":
    unittest.main(verbosity=2)
