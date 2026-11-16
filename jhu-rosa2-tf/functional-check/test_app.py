"""Offline tests for failures that an HTTP-200-only check would miss."""
import importlib.util
import json
from pathlib import Path
import sys
import unittest
from unittest.mock import MagicMock, patch

spec = importlib.util.spec_from_file_location(
    "rosa_probe", Path(__file__).parent / "app.py")
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

    SEARCH_ENV = {"DEPLOYMENT": "stage", "SEARCH_API_ENDPOINT": "https://search",
                  "MANIFEST_ID": "rose.Douce195"}
    IMAGE_ENV = {"DEPLOYMENT": "stage", "IMAGE_BASE_URL": "https://images/iiif/3",
                 "IMAGE_IDENTIFIER": "rose/book/page"}

    def run_handler(self, env, search_effect=None):
        boto3 = MagicMock()
        with patch.dict(sys.modules, {"boto3": boto3}), \
                patch.dict(app.os.environ, env, clear=True), \
                patch.object(app, "check_search", side_effect=search_effect) as search, \
                patch.object(app, "check_image") as image, \
                self.assertLogs(app.LOG, level="INFO"):
            outcomes = app.handler({}, None)
        calls = boto3.client.return_value.put_metric_data.call_args_list
        return outcomes, calls, search, image

    def test_search_deployment_reports_independent_failures_and_continues_checks(self):
        outcomes, calls, _, image = self.run_handler(self.SEARCH_ENV, [TimeoutError(), None])
        self.assertEqual(outcomes, {"search-manifest": False, "search-canvas": True})
        self.assertEqual([c.kwargs["MetricData"][0]["Value"] for c in calls], [0, 1])
        image.assert_not_called()

    def test_image_deployment_checks_only_image_delivery(self):
        outcomes, calls, search, image = self.run_handler(self.IMAGE_ENV)
        self.assertEqual(outcomes, {"image-delivery": True})
        image.assert_called_once_with("https://images/iiif/3", "rose/book/page")
        search.assert_not_called()
        self.assertEqual(calls[0].kwargs["MetricData"][0]["Dimensions"][1]["Value"],
                         "image-delivery")

    def test_handler_fails_when_no_endpoint_is_configured(self):
        with patch.dict(sys.modules, {"boto3": MagicMock()}), \
                patch.dict(app.os.environ, {"DEPLOYMENT": "stage"}, clear=True):
            with self.assertRaises(RuntimeError):
                app.handler({}, None)

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
