# Opensearch Index Design

## Purpose

This document is the reference for the two Opensearch indexes rosa2 uses for full-text
and faceted search: their settings, every field with its type and the data
`generate-opensearch-ingest` routes into it, the text analysis, and the bulk file format.

Creating the indexes and loading data is described in
[deployment.md](deployment.md#creating-indexes-and-ingesting); the client-side query
contract is in [search.md](search.md).

## Audience

Developers working on the generator, the index definitions or the search integration,
and administrators who need to know what a rebuild changes.

## Indexes

The system uses two indexes, each defined in `rosa-search/opensearch/`:

| Index | Definition file | Description |
|-------|----------------|-------------|
| `manifest` | `rosa-search/opensearch/manifest.json` | One document per book: bibliographic metadata, identifiers, facet fields |
| `canvas` | `rosa-search/opensearch/canvas.json` | One document per page, with every annotation that targets the page merged in |

All annotation data that targets a canvas is accumulated into the canvas document rather
than stored in a separate index, so each canvas document has independently searchable
fields per annotation type (marginalia, underlines, marks, …).

A change to either definition is live only after the indexes are rebuilt
(`local-dev/start.sh`, or `rosa-search/init-opensearch.sh` for AWS), because analyzers
and most settings are fixed at creation.

### Shard count

Both definitions set `"number_of_shards": 1`. The corpus is roughly 35 MB of bulk data
across the two indexes, so a single shard per index holds all of it comfortably. The
setting is explicit because the alternative is whatever default the cluster applies: AWS
managed domains created without it ended up with 5 primary shards per index, which meant
a combined `/_search` fanned out to 10 shard searches and 10 search contexts for a corpus
that fits in one. On a small single-node domain that overhead is measurable in both
latency and heap. Changing the shard count requires rebuilding the indexes.

### Replica count

Both definitions also set `"number_of_replicas": 0`. Every environment (local-dev, stage,
prod) runs a single data node, and a replica can never be placed on the same node as its
primary. With the default of 1 replica, each index leaves one shard unassigned, so the
cluster stays permanently yellow and health alarms carry no signal. If a domain is ever
scaled to two or more data nodes, raise this setting to get redundancy. Unlike the shard
count, `number_of_replicas` is dynamic and can be changed on a live index with
`PUT /<index>/_settings`.

## Strict mapping

Both mappings set `"dynamic": "strict"`. A document with a field the mapping does not
know is rejected by the Bulk API as an item error instead of being indexed with a guessed
type, which is what used to happen silently when the generator and the mapping drifted
apart. When a field is added, change `rosa-search/opensearch/{manifest,canvas}.json`, the
generator in `rosa-tool/src/main/java/rosa/archive/opensearch/` and this document
together, then rebuild and ingest; `init-opensearch.sh` fails when any bulk item is
rejected.

## Document ids

### Manifest id

`{collection_id}.{book_id}`, e.g. `rose.Douce195`, `aor.FolgersHa2`. The dot-separated
pair is unique across collections.

### Canvas id

`{collection_id}.{image_id_without_extension}`, e.g. `rose.Douce195.001r` (from
`Douce195.001r.tif`), `aor.FolgersHa2.003r`. The archive image id already carries the
book id, so only the collection prefix is added.

### Constructing IIIF URIs from search results

A canvas document's `manifest_id` and `page_num` give the IIIF Presentation canvas URI:

```
{base_url}/{collection_id}/{book_id}/canvas/{page_num}
```

Split `manifest_id` on the first `.` to get `collection_id` and `book_id`. For
`manifest_id: "rose.Douce195"` and `page_num: 2` that is
`https://iiif.example.org/rose/Douce195/canvas/2`. `page_num` is the 0-based position of
the page in the book's image list, and the IIIF canvases are numbered the same way (see
[iiif-generation.md](iiif-generation.md#canvases)).

The `iiif_image_id` field of canvas documents and of manifest `thumbnail` entries is the
IIIF Image API identifier of the page image, unencoded (e.g.
`rose/Douce195/cropped/Douce195.001r`). Percent-encode it as a single path segment and
append region, size, rotation and quality to get an image URL; see
[iiif-generation.md](iiif-generation.md#identifiers) for the identifier scheme and the
placeholder used for missing images.

## Text analysis

Textual content is routed into language-specific sub-fields so that the right analyzer
(stemming, stop words) is applied at index time. The sub-field is the language code of
the text:

| Language | Sub-field | Canvas analyzer | Manifest `title` analyzer |
|----------|-----------|-----------------|---------------------------|
| English | `.en` | `english_stripped` | built-in `english` |
| French | `.fr` | `french_stripped` | built-in `french` |
| Old French | `.ofr` | `old_french` (see below) | `old_french` |
| Latin | `.la` | `latin` (see below) | `latin` |
| Italian | `.it` | `italian_stripped` | built-in `italian` |
| Greek | `.el` | `greek_stripped` | built-in `greek` |
| Spanish | `.es` | `spanish_stripped` | built-in `spanish` |
| German | `.de` | `german_stripped` | not mapped |

The `*_stripped` analyzers of the canvas index are the built-in language analyzers with a
`strip_brackets` character filter in front, which removes the editorial `[` and `]`
around expansions (`i[nfr]a` is analysed as `infra`); the generator removes the brackets
from the indexed text as well. The Latin analyzer runs a character filter that maps `v`
to `u` and `j` to `i`, then `lowercase`, `asciifolding`, a dictionary lemmatizer
(`latin_stemmer`, a `stemmer_override` over `latin-stemmer-rules.txt`) and the same v/u,
j/i folding again on the lemma, so that a query for a dictionary headword such as
`divido` and an inflected form in the text such as `dividunt` meet at `diuido`. Its
design, dictionaries and limits are described in
[rosa-search/opensearch/LATIN-ANALYZER.md](../rosa-search/opensearch/LATIN-ANALYZER.md).

The Old French analyzer is built for the spelling of the Roman de la Rose transcriptions
rather than for modern French: it strips the elided pronouns and articles (`l'`, `qu'`,
`n'`, `s'`, `d'`, `m'`, `c'`, `j'`, `t'`, `jusqu'`, …) so that `l'avision` is found by
`avision`, folds the editorial diacritics (`eüst`, `oïr`, `aprés`), normalizes the
medieval allographs and spelling variants to one form (`u`/`v` by position, `j` and `y`
to `i`, `ou` to `o`, final `z` to `s`, `-ier` to `-er` after a palatal) and then applies
the French light stemmer. It uses no stop words, so function words such as `li` and
`ne` remain searchable in phrases. Design, evidence and limits are described in
[rosa-search/opensearch/OLD-FRENCH-ANALYZER.md](../rosa-search/opensearch/OLD-FRENCH-ANALYZER.md).

Text whose language is not in the table is routed to `.en`. In the data this concerns
`iw` (Hebrew) and `cy` (Welsh); the language codes themselves are still indexed in the
`language` and `marginalia_language` keyword fields.

Every language sub-field is an array with one element per piece of text routed to it,
for example one per marginal note: `"marginalia.la": ["Nota bene", "Vide infra"]`.
Opensearch leaves a position gap between array elements (`position_increment_gap`, 100 by
default), so phrase and proximity queries do not match across two annotations, and
highlight fragments stay within one.

### Keyword sub-field pattern

Annotation types that have both an enumerated name and free text use a `keyword`
sub-field next to the language sub-fields:

```json
"mark": {
  "type": "object",
  "properties": {
    "keyword": { "type": "keyword" },
    "en": { "type": "text", "analyzer": "english_stripped" },
    "la": { "type": "text", "analyzer": "latin" },
    ...
  }
}
```

Fields using this pattern: `mark`, `symbol`, `drawing`, `calculation`, `graph`, `table`.

**Indexing.** The type or name (e.g. `plus_sign` for a mark) is written to
`field.keyword` as an array of the distinct values on the page
(`"drawing.keyword": ["cone", "pyramid"]`); the text goes to the language sub-field.
`symbol.keyword` also receives the symbols named inside marginalia, drawings, graphs and
tables (`<symbol_in_text>`, `<marginalia_symbol>`), so a symbol search finds pages that
mention a symbol as well as pages with a symbol annotation.

**Searching.** `{"term": {"mark.keyword": "plus_sign"}}` finds a mark type;
`{"multi_match": {"query": "…", "fields": ["mark.*"]}}` searches its text in every
language (the `keyword` sub-field is included in the wildcard and matches exact values).

## Manifest index

One document per book, including books without images.

| Field | Type | Value |
|-------|------|-------|
| `id` | keyword | `{collection_id}.{book_id}` |
| `collection_id` | keyword (array) | The book's collection id followed by the ids in its `parents` (one level; `["rose", "dlmm"]`). A search filtered on `dlmm` therefore finds `rose` and `pizan` books. |
| `label` | text, `.keyword` | The book's English common name, else the book id. Copied to `label_suggest`. The viewer sorts on `label.keyword`. |
| `label_suggest` | search_as_you_type | Filled from `label` by `copy_to`; not in the bulk data. See below. |
| `title` | text with `.keyword`, `.en`, `.fr`, `.ofr`, `.la`, `.el`, `.it`, `.es` | One string: the common name followed by the titles of the book's texts, or the book id. It is not routed by language: every sub-field analyses the same string with its own analyzer. There is no `.de`. |
| `repository` | text | Holding repository, `""` if unknown |
| `shelfmark` | text | Repository shelfmark, `""` if unknown |
| `date` | keyword | Date label (facet), `""` if unknown |
| `origin` | keyword | Place of origin (facet), `""` if unknown |
| `num_pages` | integer | `<totalPages>` of `{book}.metadata.xml` (not the image count) |
| `languages` | keyword (array) | The collection's `languages` from `config.properties`, the same for every book in it |
| `description` | text | Full text of the English TEI description, `""` if none |
| `authors` | keyword (array) | Authors of the bibliographic record plus the authors of the book's texts |
| `type` | keyword | `manuscript` or `print`, `""` if unknown |
| `current_location` | keyword | Current physical location, `""` if unknown |
| `num_illustrations` | integer | From `{book}.metadata.xml` |
| `material` | keyword | `parchment`, `paper`, …, `""` if unknown |
| `has_transcription` | boolean | Whether `{book}.transcription.xml` exists |
| `thumbnail` | object, `enabled: false` (array) | Up to 3 `{iiif_image_id, page_num}` entries, see below |
| `logo` | keyword | Logo file name, see below |

Missing string values are indexed as `""`, so a `terms` aggregation on a keyword facet can
return an empty-string bucket for books without the value.

### label_suggest

A [`search_as_you_type`](https://docs.opensearch.org/latest/mappings/supported-field-types/search-as-you-type/)
field populated from `label` with `copy_to`, so the bulk data does not contain it.
Opensearch indexes `label_suggest`, `label_suggest._2gram`, `label_suggest._3gram` and
`label_suggest._index_prefix`, which makes prefix matching on each word of a label cheap.
`label` itself stays a `text` field with a `.keyword` sub-field because the viewer sorts
by `label.keyword`, and `search_as_you_type` takes no `fields`.

The viewer queries it with a `bool_prefix` `multi_match`:

```json
{
  "size": 10,
  "_source": ["id", "label", "logo"],
  "query": {
    "bool": {
      "must": [{
        "multi_match": {
          "query": "douce 1",
          "type": "bool_prefix",
          "operator": "and",
          "fields": ["label_suggest", "label_suggest._2gram", "label_suggest._3gram"]
        }
      }],
      "filter": [{ "terms": { "collection_id": ["rose"] } }]
    }
  }
}
```

`operator: and` requires every typed word to match, the last one as a prefix, so
`douce 1` narrows to `Douce 195` instead of also matching every label with a word
starting with `1`.

### thumbnail

A disabled object array (not searchable) with up to 3 pages suitable as book
thumbnails: the first 3 pages of the image list that are not missing and not front
matter or binding. Each entry has:

- `iiif_image_id`: the IIIF Image API identifier, e.g. `rose/Douce195/cropped/Douce195.001r`
  (the `cropped/` path when the book has a cropped image list).
- `page_num`: the page's 0-based position in the image list, usable to construct the
  canvas URI.

```json
"thumbnail": [
  { "iiif_image_id": "rose/Douce195/cropped/Douce195.001r", "page_num": 2 },
  { "iiif_image_id": "rose/Douce195/cropped/Douce195.001v", "page_num": 3 },
  { "iiif_image_id": "rose/Douce195/cropped/Douce195.002r", "page_num": 4 }
]
```

### logo

The file name of the logo shown with the book, under the viewer's `public/logo/`:

- `aor`: the first reader's name lowercased with spaces replaced by underscores, plus
  `.jpg` (`gabriel_harvey.jpg`); `aor.jpg` when the book names no reader.
- every other collection: `{collection_id}.jpg` (`rose.jpg`).

## Canvas index

One document per row of the book's image list, including missing images.

### Structural fields

| Field | Type | Value |
|-------|------|-------|
| `id` | keyword | `{collection_id}.{image_id_without_extension}` |
| `manifest_id` | keyword | `{collection_id}.{book_id}` |
| `manifest_label` | keyword | The book's label, the same value as the manifest document's `label`; the viewer shows it in page results |
| `collection_id` | keyword (array) | As in the manifest document |
| `iiif_image_id` | keyword | IIIF Image API identifier, e.g. `rose/Douce195/cropped/Douce195.001r` (the `cropped/` path when the book has a cropped image list) |
| `label` | text, `.keyword` | Page label: AoR pagination, else signature, else the image list label or derived name |
| `page_num` | integer | 0-based position in the image list, equal to the IIIF canvas index |

### Default language

Text that carries no language of its own is routed by the page's default language: the
language of the page's first underline (underlines carry the language of the printed
text), else the language of the book's first `<text>` in `{book}.metadata.xml`, else
`en`.

### Annotation type fields

Each is an object with the language sub-fields of [Text analysis](#text-analysis) and,
where marked, a `keyword` sub-field. One array element per annotation text.

| Field | `.keyword` | What is routed into it |
|-------|-----------|------------------------|
| `marginalia` | no | The transcribed text of each marginal note, in its language, and the note's translation (always `.en`). The text the note anchors to goes to `anchor_text`, not here. Annotations of an unknown type also land here. |
| `underline` | no | Underlined text of the printed book, in the underline's language |
| `mark` | yes | Mark names in `.keyword`; the marked text in the mark's language |
| `symbol` | yes | Symbol names in `.keyword`, from symbol annotations and from symbols named inside marginalia, drawings, graphs and tables; the referenced text in the symbol's language |
| `errata` | no | Corrected text: the copy text and the amended text |
| `numeral` | no | The numeral and its referenced text |
| `drawing` | yes | Drawing types in `.keyword`; the drawing's text elements (each in its own language, default the drawing's) and its referenced text |
| `emphasis` | no | Text emphasized within a marginal note, in the note's language |
| `cross_reference` | no | Cross-references in marginal notes: the person and the title always in `.en`, the quoted text in its own language (default the note's) |
| `calculation` | yes | Calculation types in `.keyword`; data and content always in `.en` |
| `graph` | yes | Graph types in `.keyword`; graph notes in their own language, node text and content in the page language, graph translations in `.en` |
| `table` | yes | Table types in `.keyword`; the table's text elements in their own language; column headers, cells and aggregated info in `.en` |
| `transcription` | no | The TEI transcription of the page. The manuscript text is Old French, in `.ofr`: the verse of the page as one entry (line numbers, Lecoy numbers, gaps and scribal punctuation notes dropped; initials, expansions, additions and deletions kept inline), then one entry per rubric (`hi rend="rubric"`) and per catchword (`fw`). The transcriber's text is English, in `.en`: one entry per miniature (`figure`: its `head` and `note type="character"` names) and one per editorial note (`note` of any other type, even when it quotes an Old French reading) |
| `illustration` | no | Illustration tagging of the page (resolved titles, every character name variant, textual elements, costume, objects, landscape, architecture, other) and the text of the collection's `annos.jsonld` commentary on the page, all in `.en` |
| `translation` | no | Translations of drawings and tables, in `.en` (marginalia translations are in `marginalia.en`, graph translations in `graph.en`) |
| `anchor_text` | no | Text of the printed book that an annotation refers to: the anchor text of marginal notes, drawings, tables, graph nodes and graph notes |

### Keyword array fields

Written only when non-empty, as arrays of distinct values.

| Field | Value |
|-------|-------|
| `people` | People referenced by annotations on the page, with the alternate names from the collection's `people.csv`; ISNI identifiers are resolved to names and never indexed themselves |
| `books` | Books referenced, with alternates from `books.csv` |
| `locations` | Places referenced, with alternates from `locations.csv` |
| `method` | Methods used, lowercased (`pen`, `chalk`, `pencil`, `scoring`, `typewriter`) |
| `hand` | Hands identified |
| `annotator` | The page's reader and any `other_reader` of its marginal notes |
| `language` | Lowercased language codes of all annotations on the page (including codes such as `iw` and `cy` whose text is analysed as English) |
| `marginalia_language` | Language codes of the marginal notes only |
| `topic` | Marginalia topics. Mapped, but no transcription in the current archive has a `topic`, so the field is never populated |
| `char_name` | Character names from the page's illustrations, every name variant |

## Bulk format

`generate-opensearch-ingest` writes one `<collection-id>.bulk.json` per collection that
has books (`dlmm`, which has none, gets no file). Each uses the Opensearch Bulk API
format, alternating action and document lines (newline-delimited JSON):

```json
{"index":{"_index":"manifest","_id":"rose.Douce195"}}
{"id":"rose.Douce195","collection_id":["rose","dlmm"],"label":"Douce 195","title":"Douce 195 Roman de la Rose",...}
{"index":{"_index":"canvas","_id":"aor.FolgersHa2.001r"}}
{"id":"aor.FolgersHa2.001r","manifest_id":"aor.FolgersHa2","manifest_label":"Lodovico Domenichi, Facetie (Venice, 1571) & ...","collection_id":["aor"],"label":"323","page_num":2,"marginalia.la":["Romae Athenarum populares humores ..."],"mark.keyword":["plus_sign"],"mark.la":["artium usum bonarum"],...}
```

Language and `keyword` sub-fields are written with dot notation (`marginalia.la`,
`mark.keyword`), always as arrays; Opensearch maps them onto the object structure of the
index definition. The Bulk API returns HTTP 200 even when documents fail, so check the
response for `"errors": true`; with the strict mappings an unknown field is such a
failure.
