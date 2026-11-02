# Search

## Introduction

rosa2 provides full-text and faceted search over manuscript collection data through an
Opensearch backend queried directly from the browser. This document is the contract
between a client (the viewer's JHSearch plugin, or any other IIIF client) and the search
backend: how a client discovers the service, what it may send, the limits the production
proxy enforces, and how to handle the answers.

The data behind it is described elsewhere: the indexes and their fields in
[opensearch-indexes.md](opensearch-indexes.md), the files `generate-iiif-pres` writes in
[iiif-generation.md](iiif-generation.md), and building, indexing and deploying in
[deployment.md](deployment.md).

## JHSearch service

The search service is advertised as a custom IIIF Presentation 3.0 `service` on every
collection and manifest generated with `--opensearch-url`:

```json
{
  "@context": "http://iiif.io/api/presentation/3/context.json",
  "id": "https://iiif.example.org/aor/collection.json",
  "type": "Collection",
  "label": { "en": ["Archaeology of Reading"] },
  "service": [
    {
      "id": "https://iiif.example.org/aor/service/jhsearch.json",
      "type": "JHSearchService2",
      "profile": "https://github.com/jhu-digital-manuscripts/rosa2/blob/main/docs/search.md"
    }
  ],
  "items": [...]
}
```

- **Type**: `JHSearchService2`
- **Profile**: this document
- **Id**: the collection's `service/jhsearch.json`, a static file that lists the
  searchable fields, their enumerated values, the facet categories and the Opensearch
  endpoint. Its schema is in
  [iiif-generation.md](iiif-generation.md#jhsearch-service).

### Client workflow

1. Load a collection or manifest from the IIIF static files.
2. Find the `JHSearchService2` entry in the `service` array.
3. Dereference the service `id` to fetch `service/jhsearch.json`.
4. Use `fields` and `categories` to build a search UI.
5. Send Opensearch query DSL to the `opensearch` endpoint, as described below.

## Querying

The endpoint named in `jhsearch.json` is an Opensearch `_search` URL. In the deployed
environments it is the search proxy in front of the AWS domain, which accepts `POST` to
exactly three paths and answers 404 for anything else:

| Path | Searches |
|------|----------|
| `/_search` | both indexes; book and page hits together |
| `/manifest/_search` | books only |
| `/canvas/_search` | pages only |

A canvas hit is recognised by its `manifest_id`; book hits have none.

**Fields with sub-fields.** A field whose `jhsearch.json` entry has
`has_subfields: true` is an object with per-language sub-fields and possibly `.keyword`
(see [opensearch-indexes.md](opensearch-indexes.md#text-analysis)). Search all of them
with the wildcard `name.*`:

```json
{ "query": { "multi_match": { "query": "astronomy", "fields": ["marginalia.*"] } } }
```

**Enumerated values.** A field with `values` is matched exactly with a `term` query: on
`name.keyword` when `has_subfields` is true (`{"term": {"mark.keyword": "dash"}}`),
otherwise on the field itself (`{"term": {"method": "pen"}}`).

**Collections.** Filter on `collection_id`, which contains the book's own collection and
its parent: `{"terms": {"collection_id": ["dlmm"]}}` finds `rose` and `pizan` books and
pages.

**Facets.** `categories` are keyword or integer fields of the `manifest` documents. A
category with `quantize-interval` is bucketed with a `histogram` aggregation of that
interval and filtered with a `range` per selected bucket; the others use `terms`
aggregations and filters.

**Paging.** Use `from` and `size` with a sort that has a tiebreaker (`id`). Results beyond
the 10,000th cannot be paged (see [Limits](#limits)); the viewer caps its page count so
that `from + size` never exceeds 10,000 and tells the user when a search has more
matches than that.

## Limits

The production proxy validates every request before signing it and answers 400 with a
message naming the offending key when a rule is broken. Local development talks to
Opensearch directly on port 9200, so these rules only bite on stage and prod; the
viewer's query builders stay within them.

| Rule | Limit |
|------|-------|
| Method and path | `POST` to `/_search`, `/manifest/_search` or `/canvas/_search`; anything else is 404 |
| Request body | 16 KB, else 413 |
| `size` | at most 300 |
| `from + size` | at most 10,000 (also Opensearch's `max_result_window`) |
| `terms` and `ids` | inline arrays of at most 256 scalar values; no `terms` lookup |
| Aggregations | at most 32, nested at most 2 deep, `size` at most 500, types `terms`, `histogram`, `date_histogram`, `range`, `date_range`, `filter`, `missing`, `min`, `max`, `avg`, `sum`, `stats`, `value_count`, `cardinality`; no `shard_size` |
| Query clauses | `bool`, `constant_score`, `dis_max`; `match`, `match_all` (`{}` only), `match_none`, `match_phrase`, `match_phrase_prefix`, `multi_match` (at most 64 fields), `term`, `terms`, `range`, `exists`, `prefix` (at least 2 characters), `ids`, `simple_query_string`; at most 64 clauses per `bool` list and 256 in all; nesting at most 16 levels |
| Rejected anywhere | `query_string`, `regexp`, `wildcard`, `fuzzy`, `more_like_this`, `function_score`, span queries, scripts (`script`, `script_score`, `script_fields`, `runtime_mappings`, …), `knn`, `neural`, `percolate` |
| Top-level keys | `query`, `from`, `size`, `sort` (at most 4 keys, no scripts), `aggs`/`aggregations`, `highlight` (at most 64 fields), `_source`, `track_total_hits`, `min_score` (a number), `post_filter`, `search_after`; `suggest`, `profile`, `explain` and the rest are rejected |
| Strings | at most 512 characters each |
| Rate | about 120 requests per minute per client with a burst of 30, else 429 with `Retry-After` |

Responses: 200 with the Opensearch body (see the next section for what is removed),
400 for a rejected request, 404 for other paths, 413 for an oversized body, 429 when
throttled, 503 when Opensearch is overloaded (with `Retry-After`), 502 when the backend
could not be reached or answered with an error, and 504 when it timed out. The proxy
retries a request once when Opensearch does not answer or drops the connection before
reporting 502. `terminate_after` is added to every request so that a runaway query stops
early; `terminated_early` in the response says when it did.

## Handling failure and partial responses

**Transient failures.** 502, 503 and 504 all mean the backend did not answer this time,
not that the query was wrong. A 503 or 429 carries a `Retry-After` header. Retry once
with jitter, which is what `src/plugins/jhsearch/services/opensearch.js` does; do not
retry a 429 without honouring the delay, and do not retry a client-side timeout.

**Caching.** The indexes change only on re-ingest, so the viewer keeps complete responses
in memory for the page session (30 minutes, at most 100 entries, see
`services/searchCache.js`) and shares one request between identical in-flight searches.
Errors and partial responses are not cached. After re-indexing, open tabs show the old
results until they are reloaded or the entries expire.

**Partial responses.** Opensearch answers HTTP 200 when only some shards contributed, so
a successful-looking response can be missing matches. Check both `_shards.failed` and
`timed_out` on every response and tell the user when either is set. `hits.total` counts
only the shards that answered, so a partial response is indistinguishable from a smaller
result set unless these fields are read. The proxy removes `_shards.failures`, the
per-shard exception text, from 200 responses and logs it instead, so the counts are all a
client gets:

```json
{
  "took": 143,
  "timed_out": false,
  "_shards": { "total": 2, "successful": 1, "skipped": 0, "failed": 1 },
  "hits": { "total": { "value": 29 }, "hits": [] }
}
```

The trap worth knowing is sorting on a field that only one of the two indexes maps.
`page_num` exists in `canvas` but not `manifest`, so a combined search sorted by page
number fails every `manifest` shard and quietly drops every book from the results. Always
pair such a sort with `unmapped_type`:

```json
{ "sort": [{ "page_num": { "order": "asc", "missing": "_last", "unmapped_type": "long" } }] }
```

## Name filter

On sites with facets enabled (AoR and DLMM), the first entry under "Filter By" is
**Name**, a type-ahead that picks books by label. It is not a `categories` entry in
`jhsearch.json`; the viewer always shows it. Selections are manifest ids, stored with the
other facets under the key `id`, so they appear in the URL as repeatable `f.id`
parameters (`#dlmm?f.id=rose.Douce195&f.id=pizan.Francais1243`) and are cleared by Clear
All. Each suggestion and chip shows the manifest's `logo`.

Suggestions come from the manifest index's `label_suggest` field (see
[opensearch-indexes.md](opensearch-indexes.md#label_suggest)). The viewer sends a query
after 2 characters, debounced by 250 ms, for at most 10 hits. It is filtered by the
selected collection and the other active facets, but not by the books already picked:

```json
{
  "query": {
    "bool": {
      "must": [{ "multi_match": { "query": "douce 1", "type": "bool_prefix", "operator": "and",
                                  "fields": ["label_suggest", "label_suggest._2gram", "label_suggest._3gram"] } }],
      "filter": [{ "terms": { "collection_id": ["dlmm"] } }, { "terms": { "origin": ["France"] } }]
    }
  },
  "size": 10,
  "_source": ["id", "label", "logo"]
}
```

Selected books are ORed. Because a search spans both indexes and a canvas's `id` is its
page id, the filter matches manifests by `id` and canvases by `manifest_id`. Lists longer
than 256 ids, the proxy's `terms` limit, are split over several clauses:

```json
{ "bool": { "should": [ { "terms": { "id": ["rose.Douce195"] } },
                        { "terms": { "manifest_id": ["rose.Douce195"] } } ],
            "minimum_should_match": 1 } }
```

For selections restored from a URL, the viewer fetches their labels and logos with
`terms` queries on `id` (split the same way) and `_source: ["id", "label", "logo"]`,
with `size` capped at 300.

## Facets in searches

Facet categories are manifest fields, but simple and advanced searches span both indexes,
and canvas documents have none of those fields. Opensearch aggregations would count only
the books that matched directly, so a search matching only transcriptions would show no
facet values, and a facet filter on the field would drop every page.

So for searches the viewer works facets out itself
(`rosa-viewer/src/plugins/jhsearch/utils/bookFacets.js`). Browsing queries only the
manifest index and still uses Opensearch aggregations.

1. On the first search it loads the `id`, `collection_id` and category fields of every
   book in the site's top-level collection, 300 per request (the proxy's size limit), so
   in one request for collections of up to 300 books, and keeps them for the page
   session. The top-level collection's books include those of every child collection, so
   a child collection's books are picked from them by `collection_id` with no further
   request:

   ```json
   {
     "query": { "bool": { "filter": [{ "terms": { "collection_id": ["dlmm"] } }] } },
     "from": 0, "size": 300, "sort": [{ "id": "asc" }],
     "_source": ["id", "collection_id", "current_location", "date", "has_transcription"],
     "track_total_hits": true
   }
   ```

2. Instead of facet aggregations, the search asks for the books it matched, either
   through a page or directly (books are the hits without a `manifest_id`):

   ```json
   "aggs": {
     "matched_page_books": { "terms": { "field": "manifest_id", "size": 500 } },
     "matched_books": { "filter": { "bool": { "must_not": [{ "exists": { "field": "manifest_id" } }] } },
                        "aggs": { "ids": { "terms": { "field": "id", "size": 500 } } } }
   }
   ```

   A facet count is the number of those books with the value, so "Transcription:
   Yes (3)" means three books, matched directly or through their pages.

3. Selected facet values are resolved to the books they allow, and the search is
   filtered to those books and their pages the same way as the Name filter: by `id` or
   `manifest_id`. The filter lists whichever is shorter, the allowed books in a `should`
   or the excluded ones in a `must_not`, and nothing allowed is `match_none`. Lists
   longer than 256 ids are split over several clauses.

The 500-bucket aggregation limit means a search matching more than 500 books undercounts
its facets. If the books cannot be loaded, searches fall back to Opensearch aggregations.

## Example queries

### Search marginalia for a term

```json
{ "query": { "match": { "marginalia.en": "astronomy" } } }
```

or across every language sub-field:

```json
{ "query": { "multi_match": { "query": "astronomy", "fields": ["marginalia.*"] } } }
```

### Search for a mark type

```json
{ "query": { "term": { "mark.keyword": "plus_sign" } } }
```

### Search several annotation types at once

```json
{
  "query": {
    "multi_match": {
      "query": "philosophia naturalis",
      "fields": ["marginalia.*", "underline.*", "cross_reference.*", "drawing.*", "graph.*", "table.*"],
      "type": "best_fields"
    }
  }
}
```

### Pages by annotator, with method and language counts

```json
{
  "query": { "bool": { "filter": [{ "term": { "annotator": "John Dee" } }] } },
  "aggs": {
    "methods": { "terms": { "field": "method" } },
    "languages": { "terms": { "field": "marginalia_language" } }
  }
}
```

### Books with facets

Against `/manifest/_search`:

```json
{
  "query": {
    "bool": {
      "filter": [{ "term": { "current_location": "London" } }],
      "must": [{ "multi_match": { "query": "rhetoric", "fields": ["title.en", "title.la"] } }]
    }
  },
  "aggs": {
    "origins": { "terms": { "field": "origin" } },
    "illustrations": { "histogram": { "field": "num_illustrations", "interval": 10 } }
  }
}
```

### Pages showing a character

`char_name` is a keyword field, so it takes exact values and aggregations:

```json
{
  "query": { "term": { "char_name": "Amant" } },
  "aggs": { "characters": { "terms": { "field": "char_name", "size": 50 } } }
}
```

### Canvas lookup by id

```json
{ "query": { "term": { "id": "aor.FolgersHa2.003r" } } }
```

### All canvases of a manifest

```json
{
  "query": { "term": { "manifest_id": "aor.FolgersHa2" } },
  "sort": [{ "page_num": "asc" }]
}
```

A bare `page_num` sort is safe here because this query goes to `/canvas/_search`, where
the field is mapped. Against the combined `/_search` it needs `unmapped_type` as well, or
every `manifest` shard fails the sort; see
[Handling failure and partial responses](#handling-failure-and-partial-responses).

## Relevance scores

Treat `_score` as a relative ranking signal only. Do not display it, persist it, or
compare it against a fixed threshold; its absolute scale is an implementation detail of
the underlying Lucene version. Two examples from the Opensearch 2 to 3 upgrade:

- Lucene 10 removed the `(k1 + 1)` factor from the BM25 numerator, so text-field scores
  are uniformly about 2.2× smaller than on Opensearch 2. Result ordering is unchanged.
- Term queries on `keyword` fields now rewrite to a constant score of `1.0` instead of an
  idf-weighted BM25 score. This shifts how a `field.keyword` match ranks relative to a
  text sub-field match within the same `field.*` `multi_match`.

Also note that sorting by `_score` alone has no tiebreaker. Queries that match many
equally scoring documents, for example a `mark.keyword` lookup where every hit scores the
same, return an arbitrary page of those hits, and the arbitrary order is not stable across
versions or shard layouts. Add a secondary sort key such as `id` if stable pagination is
required.
