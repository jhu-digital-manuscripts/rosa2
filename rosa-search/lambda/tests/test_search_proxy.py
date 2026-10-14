"""Tests for the search proxy validator.

The accept cases are the exact query shapes that
rosa-viewer/src/plugins/jhsearch/utils/queryBuilder.js emits, so a regression
here means the viewer breaks. The reject cases cover write access, code
execution, index escape, and unbounded cost.

Run with:  python3 -m unittest discover -s rosa-search/lambda -v
"""

from __future__ import annotations

import json
import os
import sys
import types
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
                "should": [
                    {
                        "multi_match": {
                            "query": "astronomia",
                            "fields": ["marginalia.*"],
                            "type": "best_fields",
                        }
                    },
                    # A dropdown value: a term query on the keyword sub-field
                    {"term": {"mark.keyword": "plus_sign"}},
                ],
                "minimum_should_match": 1,
                "filter": [{"terms": {"collection_id": ["aor"]}}],
            }
        },
        "from": 0,
        "size": 30,
        "sort": [{"page_num": {"order": "asc", "missing": "_last", "unmapped_type": "long"}}],
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


NAME_IDS = ["rose.Douce195", "rose.Douce332"]


def name_filter() -> dict:
    """buildNameFilter() from queryBuilder.js: manifests by id, canvases by manifest_id."""
    return {
        "bool": {
            "should": [{"terms": {"id": NAME_IDS}}, {"terms": {"manifest_id": NAME_IDS}}],
            "minimum_should_match": 1,
        }
    }


def name_suggest_query() -> dict:
    """buildNameSuggestQuery() from queryBuilder.js (Name filter type-ahead)."""
    return {
        "query": {
            "bool": {
                "must": [
                    {
                        "multi_match": {
                            "query": "douce 1",
                            "type": "bool_prefix",
                            "operator": "and",
                            "fields": [
                                "label_suggest",
                                "label_suggest._2gram",
                                "label_suggest._3gram",
                            ],
                        }
                    }
                ],
                "filter": [
                    {"terms": {"collection_id": ["dlmm"]}},
                    {"terms": {"material": ["parchment"]}},
                ],
            }
        },
        "size": 10,
        "_source": ["id", "label", "logo"],
    }


def name_lookup_query() -> dict:
    """buildNameLookupQuery() from queryBuilder.js (labels for ids restored from a URL)."""
    return {
        "query": {"bool": {"filter": [{"terms": {"id": NAME_IDS}}]}},
        "size": len(NAME_IDS),
        "_source": ["id", "label", "logo"],
    }


class AcceptsViewerQueries(unittest.TestCase):
    """Every shape rosa-viewer produces must pass."""

    def test_browse(self):
        self.assertIn("query", validate(browse_query()))

    def test_simple_search(self):
        self.assertIn("query", validate(simple_search_query()))

    def test_advanced_search(self):
        self.assertIn("query", validate(advanced_search_query()))

    def test_advanced_search_and_within_or(self):
        # (marginalia AND drawing) OR mark, as built for rows "a AND b OR c"
        body = advanced_search_query()
        should = body["query"]["bool"]["should"]
        drawing = {
            "multi_match": {"query": "pyramid", "fields": ["drawing.*"], "type": "best_fields"}
        }
        should[0] = {"bool": {"must": [should[0], drawing]}}
        body["highlight"]["fields"]["drawing.*"] = {}
        self.assertIn("query", validate(body))

    def test_advanced_search_value_with_extra_text(self):
        # A row for an enumerated field with sub-fields, with a chosen value and
        # extra text, both required: mark 'dash' plus 'regnum'.
        body = advanced_search_query()
        dash = {"term": {"mark.keyword": "dash"}}
        regnum = {"multi_match": {"query": "regnum", "fields": ["mark.*"], "type": "best_fields"}}
        body["query"]["bool"]["should"][1] = {"bool": {"must": [dash, regnum]}}
        self.assertIn("query", validate(body))

    def test_advanced_search_keyword_field_value(self):
        # A dropdown value on a plain keyword field: a term query on the field.
        body = advanced_search_query()
        body["query"]["bool"]["should"][1] = {"term": {"method": "pen"}}
        self.assertIn("query", validate(body))

    def test_collection_aggregation(self):
        self.assertIn("aggs", validate(collection_aggregation_query()))

    def test_name_suggest(self):
        body = validate(name_suggest_query())
        self.assertEqual(body["query"]["bool"]["must"][0]["multi_match"]["type"], "bool_prefix")

    def test_name_lookup(self):
        self.assertEqual(validate(name_lookup_query())["size"], 2)

    def test_browse_with_name_filter(self):
        body = browse_query()
        body["query"]["bool"]["filter"].append(name_filter())
        self.assertIn("query", validate(body))

    def test_simple_search_with_name_filter(self):
        body = simple_search_query()
        body["query"]["bool"]["filter"].append(name_filter())
        self.assertIn("query", validate(body))

    def test_advanced_search_with_name_filter(self):
        body = advanced_search_query()
        body["query"]["bool"]["filter"].append(name_filter())
        self.assertIn("query", validate(body))

    def test_term_lookup_by_id(self):
        validate({"query": {"term": {"id": "aor.Ha2.003r"}}})

    def test_manifest_canvas_lookup(self):
        validate({"query": {"term": {"manifest_id": "aor.Ha2"}}, "sort": [{"page_num": "asc"}]})

    def test_unmapped_type_sort_allowed(self):
        # buildSortClause() in queryBuilder.js sends this for a Page Order sort.
        # page_num exists only in the canvas mapping, and a search spans both
        # indexes, so without unmapped_type every manifest shard fails the sort and
        # Opensearch answers 200 with the manifests quietly missing.
        validate(
            {
                "query": {"match_all": {}},
                "sort": [{"page_num": {"order": "asc", "missing": "_last", "unmapped_type": "long"}}],
            }
        )

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

    def test_no_retry_after_on_success(self):
        self.assertNotIn("Retry-After", app.handler(self.event(), None)["headers"])

    def test_rate_limited_carries_retry_after(self):
        app._rate_limiter = app._RateLimiter(per_minute=60, burst=1)
        app.handler(self.event(), None)
        response = app.handler(self.event(), None)
        self.assertEqual(response["statusCode"], 429)
        self.assertEqual(response["headers"]["Retry-After"], app.RETRY_AFTER_SECONDS)


class UpstreamStatusMapping(unittest.TestCase):
    """How an upstream status reaches the browser.

    The viewer retries 502/503/504 and gives up on everything else, so the
    distinction between "busy" and "broken" has to survive the proxy. It used to
    not: every status outside 400/404/429 became a 502, which told the viewer a
    transient overload was a permanent fault.
    """

    def setUp(self):
        self._original_forward = app.forward
        app._rate_limiter = app._RateLimiter(0, 0)

    def tearDown(self):
        app.forward = self._original_forward

    def respond_with(self, status, body=b'{"error":{"reason":"internal detail"}}'):
        app.forward = lambda path, forwarded: (status, body)
        return app.handler(Handler.event(), None)

    def test_upstream_503_passes_through(self):
        response = self.respond_with(503)
        self.assertEqual(response["statusCode"], 503)
        self.assertEqual(json.loads(response["body"])["error"], "search backend busy")

    def test_upstream_503_carries_retry_after(self):
        self.assertEqual(self.respond_with(503)["headers"]["Retry-After"], app.RETRY_AFTER_SECONDS)

    def test_upstream_429_passes_through_as_busy(self):
        response = self.respond_with(429)
        self.assertEqual(response["statusCode"], 429)
        self.assertEqual(json.loads(response["body"])["error"], "search backend busy")
        self.assertEqual(response["headers"]["Retry-After"], app.RETRY_AFTER_SECONDS)

    def test_upstream_500_becomes_502(self):
        response = self.respond_with(500)
        self.assertEqual(response["statusCode"], 502)
        self.assertEqual(json.loads(response["body"])["error"], "search backend error")

    def test_upstream_403_becomes_502(self):
        # What stale signing credentials look like from here.
        self.assertEqual(self.respond_with(403)["statusCode"], 502)

    def test_upstream_bodies_never_reach_the_caller(self):
        for status in (403, 429, 500, 503):
            with self.subTest(status=status):
                self.assertNotIn("internal detail", self.respond_with(status)["body"])


class UpstreamRetry(unittest.TestCase):
    """forward() retries, and what it refuses to retry.

    A single-node domain answers 502/503/504, or refuses the connection outright,
    while it rides out a load spike or a node replacement. One retry turns most of
    those into a successful search. Timeouts are the exception: a request blocked
    on the domain holds a Lambda concurrency slot for the whole wait, so retrying
    one would double the worst-case hold and spread the overload rather than
    absorb it.
    """

    def setUp(self):
        self.attempts = []
        self._original_post = app._post_once
        self._original_backoff = app.UPSTREAM_BACKOFF
        self._original_attempts = app.UPSTREAM_ATTEMPTS
        app.UPSTREAM_BACKOFF = 0
        app.UPSTREAM_ATTEMPTS = 2

    def tearDown(self):
        app._post_once = self._original_post
        app.UPSTREAM_BACKOFF = self._original_backoff
        app.UPSTREAM_ATTEMPTS = self._original_attempts

    def responses(self, *outcomes):
        """Make _post_once yield each outcome in turn; callables are raised."""

        def fake_post(url, payload):
            outcome = outcomes[min(len(self.attempts), len(outcomes) - 1)]
            self.attempts.append((url, payload))
            if isinstance(outcome, BaseException):
                raise outcome
            return outcome

        app._post_once = fake_post

    def forward(self):
        return app.forward("/manifest,canvas/_search", {"query": {"match_all": {}}})

    def test_success_is_not_retried(self):
        self.responses((200, b"{}"))
        self.assertEqual(self.forward(), (200, b"{}"))
        self.assertEqual(len(self.attempts), 1)

    def test_retries_once_on_503_then_succeeds(self):
        self.responses((503, b"busy"), (200, b"{}"))
        self.assertEqual(self.forward(), (200, b"{}"))
        self.assertEqual(len(self.attempts), 2)

    def test_retries_on_502_and_504(self):
        for status in (502, 504):
            with self.subTest(status=status):
                self.attempts = []
                self.responses((status, b"x"), (200, b"{}"))
                self.assertEqual(self.forward()[0], 200)
                self.assertEqual(len(self.attempts), 2)

    def test_persistent_503_is_returned_not_masked(self):
        self.responses((503, b"busy"))
        status, _ = self.forward()
        self.assertEqual(status, 503)
        self.assertEqual(len(self.attempts), 2)

    def test_client_error_is_not_retried(self):
        self.responses((400, b"bad"))
        self.assertEqual(self.forward()[0], 400)
        self.assertEqual(len(self.attempts), 1)

    def test_unreachable_is_retried_then_becomes_502(self):
        self.responses(app._Unreachable("connection reset"))
        with self.assertRaises(app.RequestRejected) as caught:
            self.forward()
        self.assertEqual(caught.exception.status, 502)
        self.assertEqual(len(self.attempts), 2)

    def test_unreachable_then_success(self):
        self.responses(app._Unreachable("connection reset"), (200, b"{}"))
        self.assertEqual(self.forward(), (200, b"{}"))
        self.assertEqual(len(self.attempts), 2)

    def test_timeout_is_not_retried(self):
        self.responses(TimeoutError("timed out"))
        with self.assertRaises(app.RequestRejected) as caught:
            self.forward()
        self.assertEqual(caught.exception.status, 504)
        self.assertEqual(len(self.attempts), 1)

    def test_attempts_can_be_disabled(self):
        app.UPSTREAM_ATTEMPTS = 1
        self.responses((503, b"busy"))
        self.assertEqual(self.forward()[0], 503)
        self.assertEqual(len(self.attempts), 1)

    def test_each_attempt_is_signed_separately(self):
        # Re-signing per attempt is what lets a retry pick up rotated credentials.
        self.responses((503, b"busy"), (200, b"{}"))
        self.forward()
        self.assertEqual(len(self.attempts), 2)
        self.assertEqual(self.attempts[0][1], self.attempts[1][1])


class CredentialResolution(unittest.TestCase):
    """Credentials must not be a snapshot taken at import time.

    Lambda hands the execution role's credentials to the function in
    AWS_ACCESS_KEY_ID and friends, and rotates them inside an execution
    environment that can outlive them. botocore's EnvProvider returns a
    non-refreshing credentials object for that source, so a snapshot goes stale,
    every signature it makes is rejected with a 403, and the proxy reports 502 for
    every request that sandbox serves until it is recycled.
    """

    def setUp(self):
        self._original_cache = app._credentials_cache
        self._original_key = os.environ.get("AWS_ACCESS_KEY_ID")
        self.resolved = []

        def fake_session():
            credentials = object()
            self.resolved.append(credentials)
            return types.SimpleNamespace(get_credentials=lambda: credentials)

        self._original_boto3 = app.boto3
        app.boto3 = types.SimpleNamespace(Session=fake_session)
        app._credentials_cache = None

    def tearDown(self):
        app.boto3 = self._original_boto3
        app._credentials_cache = self._original_cache
        if self._original_key is None:
            os.environ.pop("AWS_ACCESS_KEY_ID", None)
        else:
            os.environ["AWS_ACCESS_KEY_ID"] = self._original_key

    def test_stable_credentials_resolved_once(self):
        os.environ["AWS_ACCESS_KEY_ID"] = "ASIAFIRST"
        first = app._resolve_credentials()
        self.assertIs(app._resolve_credentials(), first)
        self.assertEqual(len(self.resolved), 1)

    def test_rotation_is_picked_up(self):
        os.environ["AWS_ACCESS_KEY_ID"] = "ASIAFIRST"
        first = app._resolve_credentials()

        os.environ["AWS_ACCESS_KEY_ID"] = "ASIASECOND"
        second = app._resolve_credentials()

        self.assertIsNot(second, first)
        self.assertEqual(len(self.resolved), 2)

    def test_missing_credentials_is_a_server_error(self):
        os.environ["AWS_ACCESS_KEY_ID"] = "ASIAFIRST"
        app.boto3 = types.SimpleNamespace(
            Session=lambda: types.SimpleNamespace(get_credentials=lambda: None)
        )
        with self.assertRaises(app.RequestRejected) as caught:
            app._resolve_credentials()
        self.assertEqual(caught.exception.status, 500)


class UpstreamDefaults(unittest.TestCase):
    """Timeout budget. The proxy must fail before anything above it does.

    Order matters: upstream timeout < Lambda timeout, and the worst-case total
    must stay under the API Gateway integration timeout of 29 s, or a slow search
    surfaces as a gateway error instead of the proxy's own 504.
    """

    def test_upstream_timeout_is_short(self):
        self.assertLessEqual(app.UPSTREAM_TIMEOUT, 10)

    def test_search_timeout_is_below_upstream_timeout(self):
        # The ordering is the point. OpenSearch honouring its own timeout gives the
        # caller a 200 with partial results and timed_out set, which the viewer
        # shows as a warning next to the hits it did get. The proxy's socket
        # expiring first gives the caller a 504 and nothing. So the in-query
        # timeout has to be reached first.
        self.assertTrue(app.SEARCH_TIMEOUT.endswith("s"), app.SEARCH_TIMEOUT)
        search_timeout_seconds = float(app.SEARCH_TIMEOUT.removesuffix("s"))
        self.assertLess(search_timeout_seconds, app.UPSTREAM_TIMEOUT)

    def test_worst_case_hold_fits_under_api_gateway(self):
        # A timeout is never retried, so the worst case is one timeout plus one
        # non-timeout attempt and the backoff between them.
        worst_case = app.UPSTREAM_TIMEOUT * app.UPSTREAM_ATTEMPTS + app.UPSTREAM_BACKOFF * (
            app.UPSTREAM_ATTEMPTS - 1
        )
        self.assertLess(worst_case, 29)


if __name__ == "__main__":
    unittest.main(verbosity=2)
