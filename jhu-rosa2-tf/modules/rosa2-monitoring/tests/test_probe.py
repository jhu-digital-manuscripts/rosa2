"""Offline tests for failures that an HTTP-200-only check would miss."""
import importlib.util
import json
from pathlib import Path
import sys
import unittest
from unittest.mock import MagicMock, patch

spec = importlib.util.spec_from_file_location(
    "rosa_probe", Path(__file__).parents[1] / "probe" / "app.py")
app = importlib.util.module_from_spec(spec)
spec.loader.exec_module(app)


class ProbeTests(unittest.TestCase):
    def search_result(self, field="id"):
        return {"timed_out": False, "_shards": {"successful": 1, "failed": 0},
                "hits": {"hits": [{"_source": {field: "rose.Douce195"}}]}}

    def test_search_checks_known_document_in_both_indexes(self):
        for index, field in [("manifest", "id"), ("canvas", "manifest_id")]:
            with self.subTest(index=index), patch.object(
                    app, "fetch", return_value=(json.dumps(self.search_result(field)).encode(),
                                               "application/json")) as fetch:
                app.check_search("https://search", index, "rose.Douce195")
                query = json.loads(fetch.call_args.args[1])
                self.assertEqual(query["query"], {"term": {field: "rose.Douce195"}})

    def test_search_rejects_partial_failed_empty_or_wrong_results(self):
        results = []
        result = self.search_result()
        result["timed_out"] = True
        results.append(result)
        result = self.search_result()
        result["_shards"]["failed"] = 1
        results.append(result)
        result = self.search_result()
        result["hits"]["hits"] = []
        results.append(result)
        result = self.search_result()
        result["hits"]["hits"][0]["_source"]["id"] = "wrong"
        results.append(result)
        results.extend([{}, {"error": "upstream failed"}])
        for result in results:
            with self.subTest(result=result), patch.object(
                    app, "fetch", return_value=(json.dumps(result).encode(), "application/json")):
                with self.assertRaises(ValueError):
                    app.check_search("https://search", "manifest", "rose.Douce195")

    def image_info(self):
        return json.dumps({"type": "ImageService3", "id": "https://images/iiif/3/rose%2Fbook%2Fpage",
                           "width": 3732, "height": 5742}).encode()

    def test_image_validates_metadata_and_jpeg(self):
        jpeg = b"\xff\xd8" + b"x" * 100 + b"\xff\xd9"
        with patch.object(app, "fetch", side_effect=[
                (self.image_info(), "application/json"), (jpeg, "image/jpeg")]) as fetch:
            app.check_image("https://images/iiif/3", "rose/book/page")
            self.assertEqual(fetch.call_count, 2)
            self.assertIn("rose%2Fbook%2Fpage/full/80,", fetch.call_args.args[0])

    def test_image_rejects_invalid_metadata(self):
        for changes in [{"width": 0}, {"id": "https://wrong"}, {"type": "error"}]:
            info = json.loads(self.image_info()) | changes
            with self.subTest(changes=changes), patch.object(
                    app, "fetch", return_value=(json.dumps(info).encode(), "application/json")):
                with self.assertRaises(ValueError):
                    app.check_image("https://images/iiif/3", "rose/book/page")

    def test_image_rejects_html_or_truncated_jpeg_even_with_http_200(self):
        for data, mime in [(b"<html>error</html>", "text/html"),
                           (b"\xff\xd8" + b"x" * 100, "image/jpeg")]:
            with self.subTest(mime=mime), patch.object(app, "fetch", side_effect=[
                    (self.image_info(), "application/json"), (data, mime)]):
                with self.assertRaises(ValueError):
                    app.check_image("https://images/iiif/3", "rose/book/page")

    def test_handler_reports_independent_failures_and_continues_checks(self):
        boto3 = MagicMock()
        env = {"DEPLOYMENT": "stage", "SEARCH_API_ENDPOINT": "https://search",
               "MANIFEST_ID": "rose.Douce195", "IMAGE_ENABLED": "true",
               "IMAGE_BASE_URL": "https://images/iiif/3", "IMAGE_IDENTIFIER": "rose/book/page"}
        with patch.dict(sys.modules, {"boto3": boto3}), patch.dict(app.os.environ, env), \
                patch.object(app, "check_search", side_effect=[TimeoutError(), None]), \
                patch.object(app, "check_image"), self.assertLogs(app.LOG, level="INFO"):
            outcomes = app.handler({}, None)
        self.assertEqual(outcomes, {"search-manifest": False, "search-canvas": True,
                                    "image-delivery": True})
        calls = boto3.client.return_value.put_metric_data.call_args_list
        self.assertEqual([c.kwargs["MetricData"][0]["Value"] for c in calls], [0, 1, 1])

    def test_fetch_rejects_oversized_responses(self):
        response = MagicMock()
        response.__enter__.return_value = response
        response.status = 200
        response.read.return_value = b"x" * (app.MAX_BODY + 1)
        with patch.object(app, "urlopen", return_value=response):
            with self.assertRaises(ValueError):
                app.fetch("https://search")


if __name__ == "__main__":
    unittest.main()
