"""Tests for the search proxy validator.

The accept cases are the exact query shapes that
rosa-viewer/src/plugins/jhsearch/utils/queryBuilder.js emits, so a regression
here means the viewer breaks. The reject cases cover write access, code
execution, index escape, and unbounded cost.

Run with:  python3 -m unittest discover -s rosa-search/lambda -v
"""

from __future__ import annotations

import json
import sys
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))

from _stubs import load_lambda  # noqa: E402

app = load_lambda("search_proxy", "rosa_search_proxy")

app.OPENSEARCH_ENDPOINT = "search-test.us-east-1.es.amazonaws.com"


# Field and category definitions matching rosa-search/opensearch/*.json.
LANGUAGE_FIELDS = ["marginalia.*", "underline.*", "mark.*", "symbol.*", "transcription.*"]
CATEGORIES = [
    "authors",
    "current_location",
    "date",
    "material",
    "type",
    "method",
    "hand",
    "annotator",
    "language",
    "topic",
]


def browse_query() -> dict:
    """buildBrowseQuery() from queryBuilder.js."""
    return {
        "query": {"bool": {"filter": [{"terms": {"collection_id": ["aor"]}}]}},
        "from": 0,
        "size": 20,
        "sort": [{"label.keyword": "asc"}],
        "aggs": {name: {"terms": {"field": name, "size": 100}} for name in CATEGORIES},
    }


def simple_search_query() -> dict:
    """buildSimpleSearchQuery() from queryBuilder.js."""
    return {
        "query": {
            "bool": {
                "must": [
                    {
                        "multi_match": {
                            "query": "philosophia naturalis",
                            "fields": LANGUAGE_FIELDS,
                            "type": "best_fields",
                        }
                    }
                ],
                "filter": [
                    {"terms": {"collection_id": ["aor"]}},
                    {"terms": {"annotator": ["John Dee"]}},
                    {"range": {"num_pages": {"gte": 100, "lt": 200}}},
                ],
            }
        },
        "from": 20,
        "size": 20,
        "highlight": {
            "fields": {name: {} for name in LANGUAGE_FIELDS},
            "pre_tags": ["<mark>"],
            "post_tags": ["</mark>"],
        },
        "aggs": {
            **{name: {"terms": {"field": name, "size": 100}} for name in CATEGORIES},
            "num_pages": {"histogram": {"field": "num_pages", "interval": 100, "min_doc_count": 1}},
        },
    }


def advanced_search_query() -> dict:
    """buildAdvancedSearchQuery() from queryBuilder.js, with an OR row."""
    return {
        "query": {
            "bool": {
                "must": [
                    {
                        "multi_match": {
                            "query": "astronomia",
                            "fields": ["marginalia.*"],
                            "type": "best_fields",
                        }
                    }
                ],
                "should": [
                    {
                        "multi_match": {
                            "query": "plus_sign",
                            "fields": ["mark.*"],
                            "type": "best_fields",
                        }
                    }
                ],
                "minimum_should_match": 1,
                "filter": [{"terms": {"collection_id": ["aor"]}}],
            }
        },
        "from": 0,
        "size": 30,
        "sort": [{"page_num": {"order": "asc", "missing": "_last"}}],
        "highlight": {
            "fields": {"marginalia.*": {}, "mark.*": {}},
            "pre_tags": ["<mark>"],
            "post_tags": ["</mark>"],
        },
        "aggs": {name: {"terms": {"field": name, "size": 100}} for name in CATEGORIES},
    }


def collection_aggregation_query() -> dict:
    """buildCollectionAggregationQuery() from queryBuilder.js."""
    return {
        "query": {"bool": {"filter": [{"terms": {"collection_id": ["aor", "rose", "top"]}}]}},
        "size": 0,
        "aggs": {"collections": {"terms": {"field": "collection_id", "size": 100}}},
    }


def validate(query: dict) -> dict:
    return app.validate_body(json.dumps(query).encode("utf-8"))


class AcceptsViewerQueries(unittest.TestCase):
    """Every shape rosa-viewer produces must pass."""

    def test_browse(self):
        self.assertIn("query", validate(browse_query()))

    def test_simple_search(self):
        self.assertIn("query", validate(simple_search_query()))

    def test_advanced_search(self):
        self.assertIn("query", validate(advanced_search_query()))

    def test_collection_aggregation(self):
        self.assertIn("aggs", validate(collection_aggregation_query()))

    def test_term_lookup_by_id(self):
        validate({"query": {"term": {"id": "aor.Ha2.003r"}}})

    def test_manifest_canvas_lookup(self):
        validate({"query": {"term": {"manifest_id": "aor.Ha2"}}, "sort": [{"page_num": "asc"}]})

    def test_keyword_subfield_term(self):
        validate({"query": {"term": {"mark.keyword": "plus_sign"}}})

    def test_match_all(self):
        validate({"query": {"match_all": {}}, "size": 1})

    def test_source_filtering(self):
        validate({"query": {"match_all": {}}, "_source": ["id", "label"]})
        validate({"query": {"match_all": {}}, "_source": {"includes": ["id"]}})
        validate({"query": {"match_all": {}}, "_source": False})

    def test_safety_net_injected(self):
        body = validate(browse_query())
        self.assertEqual(body["timeout"], app.SEARCH_TIMEOUT)
        self.assertEqual(body["terminate_after"], app.TERMINATE_AFTER)

    def test_track_total_hits_capped(self):
        body = validate({"query": {"match_all": {}}, "track_total_hits": 99999999})
        self.assertEqual(body["track_total_hits"], app.MAX_RESULT_WINDOW)


class RejectsCodeExecution(unittest.TestCase):
    def assert_rejected(self, query, fragment=None):
        with self.assertRaises(app.RequestRejected) as caught:
            validate(query)
        if fragment:
            self.assertIn(fragment, str(caught.exception))

    def test_script_field(self):
        self.assert_rejected({"query": {"match_all": {}}, "script_fields": {"x": {}}})

    def test_script_in_sort(self):
        self.assert_rejected({"sort": [{"_script": {"script": "1"}}]})

    def test_script_score(self):
        self.assert_rejected({"query": {"script_score": {"query": {"match_all": {}}}}})

    def test_script_nested_deep(self):
        self.assert_rejected(
            {"query": {"bool": {"filter": [{"bool": {"must": [{"script": {"source": "1"}}]}}]}}}
        )

    def test_runtime_mappings(self):
        self.assert_rejected({"runtime_mappings": {"x": {"type": "keyword"}}})

    def test_scripted_metric_agg(self):
        self.assert_rejected({"aggs": {"x": {"scripted_metric": {"init_script": "s"}}}})


class RejectsExpensiveQueries(unittest.TestCase):
    def assert_rejected(self, query, fragment=None):
        with self.assertRaises(app.RequestRejected) as caught:
            validate(query)
        if fragment:
            self.assertIn(fragment, str(caught.exception))

    def test_query_string(self):
        self.assert_rejected({"query": {"query_string": {"query": "*:*"}}})

    def test_regexp(self):
        self.assert_rejected({"query": {"regexp": {"label": ".*"}}})

    def test_wildcard(self):
        self.assert_rejected({"query": {"wildcard": {"label": "*a*"}}})

    def test_fuzzy(self):
        self.assert_rejected({"query": {"fuzzy": {"label": "x"}}})

    def test_more_like_this(self):
        self.assert_rejected({"query": {"more_like_this": {"like": "x"}}})

    def test_size_above_cap(self):
        self.assert_rejected({"query": {"match_all": {}}, "size": 5000}, "'size' may not exceed")

    def test_deep_pagination(self):
        self.assert_rejected(
            {"query": {"match_all": {}}, "from": 500000, "size": 10},
            "may not exceed",
        )

    def test_leading_wildcard_field(self):
        self.assert_rejected(
            {"query": {"multi_match": {"query": "x", "fields": ["*"]}}},
            "may not start with a wildcard",
        )

    def test_agg_bucket_explosion(self):
        self.assert_rejected(
            {"aggs": {"x": {"terms": {"field": "people", "size": 100000}}}},
            "buckets",
        )

    def test_too_many_aggs(self):
        aggs = {f"a{i}": {"terms": {"field": "people"}} for i in range(app.MAX_AGG_COUNT + 5)}
        self.assert_rejected({"aggs": aggs}, "aggregations requested")

    def test_agg_nesting_depth(self):
        self.assert_rejected(
            {
                "aggs": {
                    "a": {
                        "terms": {"field": "people"},
                        "aggs": {
                            "b": {
                                "terms": {"field": "books"},
                                "aggs": {"c": {"terms": {"field": "topic"}}},
                            }
                        },
                    }
                }
            },
            "nest deeper",
        )

    def test_excessive_bool_clauses(self):
        clauses = [{"term": {"id": str(i)}} for i in range(app.MAX_BOOL_CLAUSES + 10)]
        self.assert_rejected({"query": {"bool": {"should": clauses}}}, "clauses")

    def test_deep_nesting(self):
        node = {"match_all": {}}
        for _ in range(30):
            node = {"bool": {"filter": [node]}}
        with self.assertRaises(app.RequestRejected):
            validate({"query": node})

    def test_oversized_body(self):
        oversized = b'{"query":{"match_all":{}},"_source":' + b'["a",' * 5000 + b'"b"]}'
        with self.assertRaises(app.RequestRejected) as caught:
            app.validate_body(oversized)
        self.assertEqual(caught.exception.status, 413)

    def test_long_string_value(self):
        self.assert_rejected(
            {"query": {"match": {"label": "x" * (app.MAX_QUERY_TEXT + 1)}}},
            "characters or fewer",
        )


class RejectsIndexEscapeAndLeaks(unittest.TestCase):
    def assert_rejected(self, query, fragment=None):
        with self.assertRaises(app.RequestRejected) as caught:
            validate(query)
        if fragment:
            self.assertIn(fragment, str(caught.exception))

    def test_terms_lookup_reads_another_index(self):
        self.assert_rejected(
            {
                "query": {
                    "terms": {
                        "collection_id": {
                            "index": ".opendistro_security",
                            "id": "1",
                            "path": "x",
                        }
                    }
                }
            },
            "inline array",
        )

    def test_profile_leaks_internals(self):
        self.assert_rejected({"query": {"match_all": {}}, "profile": True})

    def test_explain_leaks_internals(self):
        self.assert_rejected({"query": {"match_all": {}}, "explain": True})

    def test_search_path_pins_indexes(self):
        """A bare /_search must not be able to reach system indexes."""
        self.assertEqual(app.PATH_ROUTES["/_search"], "/manifest,canvas/_search")

    def test_only_search_paths_routable(self):
        self.assertEqual(
            sorted(app.PATH_ROUTES),
            ["/_search", "/canvas/_search", "/manifest/_search"],
        )

    def test_inner_hits_rejected(self):
        self.assert_rejected({"query": {"match_all": {}}, "inner_hits": {}})

    def test_suggest_rejected(self):
        self.assert_rejected({"query": {"match_all": {}}, "suggest": {"s": {}}})

    def test_script_params_rejected(self):
        self.assert_rejected({"aggs": {"a": {"terms": {"field": "x", "params": {}}}}})


class EscapedKeysAreNormalized(unittest.TestCase):
    """The denylist matches decoded keys, not raw bytes.

    A \\uXXXX escape in a JSON key decodes before the walk sees it, so spelling
    'script' with escapes does not evade the check. This test pins that
    assumption, since the whole denylist depends on it.
    """

    def test_unicode_escaped_denied_key(self):
        raw = b'{"query":{"match_all":{}},"scr\\u0069pt_fields":{"x":{}}}'
        with self.assertRaises(app.RequestRejected):
            app.validate_body(raw)

    def test_unicode_escaped_nested_script(self):
        raw = b'{"query":{"bool":{"filter":[{"\\u0073cript":{"source":"1"}}]}}}'
        with self.assertRaises(app.RequestRejected):
            app.validate_body(raw)

    def test_escape_decoding_actually_happens(self):
        self.assertEqual(json.loads('{"scr\\u0069pt":1}'), {"script": 1})


class RejectsMalformedInput(unittest.TestCase):
    def test_not_json(self):
        with self.assertRaises(app.RequestRejected):
            app.validate_body(b"this is not json")

    def test_empty(self):
        with self.assertRaises(app.RequestRejected):
            app.validate_body(b"")

    def test_array_body(self):
        with self.assertRaises(app.RequestRejected):
            app.validate_body(b"[1,2,3]")

    def test_unknown_top_level_key(self):
        with self.assertRaises(app.RequestRejected) as caught:
            validate({"query": {"match_all": {}}, "delete_by_query": {}})
        self.assertIn("unsupported search parameters", str(caught.exception))

    def test_unknown_query_type(self):
        with self.assertRaises(app.RequestRejected) as caught:
            validate({"query": {"not_a_real_query": {}}})
        self.assertIn("is not permitted", str(caught.exception))

    def test_multiple_clause_types(self):
        with self.assertRaises(app.RequestRejected):
            validate({"query": {"match_all": {}, "term": {"id": "x"}}})

    def test_negative_size(self):
        with self.assertRaises(app.RequestRejected):
            validate({"size": -1})

    def test_boolean_size(self):
        with self.assertRaises(app.RequestRejected):
            validate({"size": True})


class RateLimiter(unittest.TestCase):
    def test_burst_then_block(self):
        limiter = app._RateLimiter(per_minute=60, burst=5)
        self.assertTrue(all(limiter.allow("10.0.0.1") for _ in range(5)))
        self.assertFalse(limiter.allow("10.0.0.1"))

    def test_independent_per_ip(self):
        limiter = app._RateLimiter(per_minute=60, burst=2)
        self.assertTrue(limiter.allow("10.0.0.1"))
        self.assertTrue(limiter.allow("10.0.0.1"))
        self.assertFalse(limiter.allow("10.0.0.1"))
        self.assertTrue(limiter.allow("10.0.0.2"))

    def test_disabled_allows_everything(self):
        limiter = app._RateLimiter(per_minute=0, burst=0)
        self.assertTrue(all(limiter.allow("10.0.0.1") for _ in range(1000)))

    def test_tracked_ips_bounded(self):
        limiter = app._RateLimiter(per_minute=6000, burst=10)
        for i in range(app._RateLimiter.MAX_TRACKED_IPS + 500):
            limiter.allow(f"10.1.{i // 256}.{i % 256}")
        self.assertLessEqual(len(limiter._buckets), app._RateLimiter.MAX_TRACKED_IPS)


class Handler(unittest.TestCase):
    def setUp(self):
        self.forwarded = []
        self._original_forward = app.forward

        def fake_forward(path, body):
            self.forwarded.append((path, body))
            return 200, b'{"hits":{"total":{"value":0},"hits":[]}}'

        app.forward = fake_forward
        app._rate_limiter = app._RateLimiter(0, 0)

    def tearDown(self):
        app.forward = self._original_forward

    @staticmethod
    def event(method="POST", path="/_search", body=None):
        return {
            "rawPath": path,
            "requestContext": {"http": {"method": method, "sourceIp": "10.0.0.9"}},
            "body": json.dumps(body if body is not None else browse_query()),
        }

    def test_post_search_forwards_pinned_path(self):
        response = app.handler(self.event(), None)
        self.assertEqual(response["statusCode"], 200)
        self.assertEqual(self.forwarded[0][0], "/manifest,canvas/_search")

    def test_canvas_path_preserved(self):
        app.handler(self.event(path="/canvas/_search"), None)
        self.assertEqual(self.forwarded[0][0], "/canvas/_search")

    def test_get_rejected(self):
        self.assertEqual(app.handler(self.event(method="GET"), None)["statusCode"], 405)

    def test_delete_rejected(self):
        self.assertEqual(app.handler(self.event(method="DELETE"), None)["statusCode"], 405)

    def test_put_rejected(self):
        self.assertEqual(app.handler(self.event(method="PUT"), None)["statusCode"], 405)

    def test_unknown_path_rejected(self):
        response = app.handler(self.event(path="/canvas/_doc/1"), None)
        self.assertEqual(response["statusCode"], 404)
        self.assertEqual(self.forwarded, [])

    def test_bulk_path_rejected(self):
        self.assertEqual(app.handler(self.event(path="/_bulk"), None)["statusCode"], 404)

    def test_delete_by_query_path_rejected(self):
        response = app.handler(self.event(path="/canvas/_delete_by_query"), None)
        self.assertEqual(response["statusCode"], 404)

    def test_invalid_query_not_forwarded(self):
        response = app.handler(self.event(body={"query": {"regexp": {"label": ".*"}}}), None)
        self.assertEqual(response["statusCode"], 400)
        self.assertEqual(self.forwarded, [])

    def test_rate_limited(self):
        app._rate_limiter = app._RateLimiter(per_minute=60, burst=1)
        self.assertEqual(app.handler(self.event(), None)["statusCode"], 200)
        self.assertEqual(app.handler(self.event(), None)["statusCode"], 429)

    def test_upstream_error_message_is_opaque(self):
        def failing_forward(path, body):
            return 400, b'{"error":{"root_cause":[{"reason":"mapping [secret] not found"}]}}'

        app.forward = failing_forward
        response = app.handler(self.event(), None)
        self.assertNotIn("secret", response["body"])
        self.assertEqual(json.loads(response["body"])["error"], "malformed search request")

    def test_response_headers(self):
        headers = app.handler(self.event(), None)["headers"]
        self.assertEqual(headers["Content-Type"], "application/json")
        self.assertEqual(headers["Cache-Control"], "no-store")


if __name__ == "__main__":
    unittest.main(verbosity=2)
