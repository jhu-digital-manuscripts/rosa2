# Opensearch Index Definitions

This directory contains index definition files for the rosa2 Opensearch indexes. Use these definitions to create indexes before running the bulk ingest command.

## Purpose of Each Index

### `manifests`

Book-level metadata for full-text search and faceted filtering. Contains one document per book with bibliographic fields (title, authors, repository, date, origin, etc.) and facet-compatible keyword fields for building search result filters.

### `canvases`

Page-level data for image and position lookups. Contains one document per page/canvas with the image identifier and sequential position within the book. Supports page-based navigation and linking annotations back to specific images.

### `annotations`

Annotation content for full-text and faceted search. Contains one document per annotation across all types (marginalia, underline, mark, symbol, drawing, errata, numeral, transcription, illustration). Text content is routed into language-specific sub-fields for proper stemming and analysis.

## Multi-Language Field Structure

Textual annotation fields use a sub-field naming convention that routes content to language-appropriate analyzers:

```
field_name.language_code
```

For example, a marginalia annotation written in French with its text content would be indexed as `text.fr`, while its Latin translation would go to `translation.la`.

### Supported Languages and Analyzers

| Sub-field | Language   | Analyzer                                          |
|-----------|------------|---------------------------------------------------|
| `.en`     | English    | Built-in `english` (stemming + stop words)        |
| `.fr`     | French     | Built-in `french` (stemming + stop words)         |
| `.ofr`    | Old French | Custom: French stemming + ASCII folding           |
| `.la`     | Latin      | Custom: standard tokenizer + Latin stemming       |
| `.it`     | Italian    | Built-in `italian` (stemming + stop words)        |
| `.el`     | Greek      | Built-in `greek`                                  |
| `.es`     | Spanish    | Built-in `spanish` (stemming + stop words)        |
| `.de`     | German     | Built-in `german` (stemming + stop words)         |

### Fields Using This Pattern

The following fields in the `annotations` index use language sub-fields:

- `text` — primary annotation text content
- `translation` — translated text
- `emphasis` — emphasized/underlined text within marginalia
- `cross_reference` — people and titles from XRef elements
- `anchor_text` — text from drawings/tables

The `manifests` index uses language sub-fields for:

- `title` — book title searched with language-appropriate analysis

### Language Fallback

If an annotation has a language code that is not in the supported set, the content is routed to the `.en` (English) sub-field as a fallback.

## Field Name Mapping (Legacy JHSearch → Opensearch)

The legacy rosa2 search system used Lucene with JHSearch field names. The table below maps those to the new Opensearch field structure.

| Legacy JHSearch Field   | Opensearch Index  | Opensearch Field         | Notes                              |
|-------------------------|-------------------|--------------------------|------------------------------------|
| `object_id`             | (all)             | `id`                     | Document identifier                |
| `collection_id`         | (all)             | `collection_id`          | Unchanged                          |
| `manifest_id`           | annotations       | `manifest_id`            | Unchanged                          |
| `image_name`            | canvases, annotations | `image_name`          | Unchanged                          |
| `marginalia`            | annotations       | `text.*` (type=marginalia) | Filter by `type` + search `text`|
| `underline`             | annotations       | `text.*` (type=underline)  | Filter by `type` + search `text`|
| `emphasis`              | annotations       | `emphasis.*`             | Separate multi-lang field          |
| `errata`                | annotations       | `text.*` (type=errata)   | Filter by `type` + search `text`  |
| `mark`                  | annotations       | `text.*` (type=mark)     | Filter by `type` + search `text`  |
| `symbol`                | annotations       | `text.*` (type=symbol)   | Filter by `type` + search `text`  |
| `numeral`               | annotations       | `text.*` (type=numeral)  | Filter by `type` + search `text`  |
| `drawing`               | annotations       | `text.*` (type=drawing)  | Filter by `type` + search `text`  |
| `cross_reference`       | annotations       | `cross_reference.*`      | Separate multi-lang field          |
| `transcription`         | annotations       | `text.*` (type=transcription) | Filter by `type` + search `text` |
| `illustration`          | annotations       | `text.*` (type=illustration)  | Filter by `type` + search `text` |
| `calculation`           | annotations       | `text.*` (type=calculation) | Filter by `type` + search `text`|
| `graph`                 | annotations       | `text.*` (type=graph)    | Filter by `type` + search `text`  |
| `table`                 | annotations       | `text.*` (type=table)    | Filter by `type` + search `text`  |
| `title`                 | manifests         | `title.*`                | Multi-lang sub-fields              |
| `people`                | annotations       | `people`                 | Keyword array                      |
| `place`                 | annotations       | `locations`              | Renamed to `locations`             |
| `repo`                  | manifests         | `repository`             | Renamed to `repository`            |
| `description`           | manifests         | `description`            | Unchanged                          |
| `text`                  | annotations       | `text.*`                 | Multi-lang sub-fields              |
| `book`                  | annotations       | `books`                  | Renamed to `books`, keyword array  |
| `method`                | annotations       | `method`                 | Keyword field                      |
| `language`              | annotations       | `language`               | Keyword field                      |
| `hand`                  | annotations       | `hand`                   | Keyword field                      |
| `annotator`             | annotations       | `annotator`              | Keyword field                      |

Key differences from the legacy system:
- Per-annotation-type fields (e.g., `marginalia`, `underline`) are unified into a single `text` field with a `type` discriminator for filtering.
- Language routing was `field.ENGLISH`, `field.FRENCH` etc. in Lucene; now it is `field.en`, `field.fr` etc.
- Faceting used Lucene's `SortedSetDocValuesFacetField`; now uses keyword-typed fields directly.

## Example Queries

### Per-Annotation-Type Search

Search for marginalia containing "astronomy" in English:

```json
{
  "query": {
    "bool": {
      "filter": [
        { "term": { "type": "marginalia" } }
      ],
      "must": [
        { "match": { "text.en": "astronomy" } }
      ]
    }
  }
}
```

### Cross-Field Multi-Match Across Language Sub-Fields

Search for text across all language sub-fields simultaneously:

```json
{
  "query": {
    "multi_match": {
      "query": "philosophia naturalis",
      "fields": [
        "text.en",
        "text.fr",
        "text.la",
        "text.it",
        "text.el",
        "text.es",
        "text.de",
        "text.ofr"
      ],
      "type": "best_fields"
    }
  }
}
```

### Faceted Filtering Using Keyword Fields

Filter annotations by annotator and type, with an aggregation on `method`:

```json
{
  "query": {
    "bool": {
      "filter": [
        { "term": { "annotator": "John Dee" } },
        { "term": { "type": "marginalia" } }
      ]
    }
  },
  "aggs": {
    "methods": {
      "terms": { "field": "method" }
    },
    "topics": {
      "terms": { "field": "topic" }
    }
  }
}
```

Filter manifests by current location and type:

```json
{
  "query": {
    "bool": {
      "filter": [
        { "term": { "current_location": "London" } },
        { "term": { "type": "printed" } }
      ],
      "must": [
        { "multi_match": { "query": "rhetoric", "fields": ["title.en", "title.la"] } }
      ]
    }
  },
  "aggs": {
    "authors": {
      "terms": { "field": "authors" }
    }
  }
}
```

### Page-Based Lookup Using image_name

Find all annotations on a specific page:

```json
{
  "query": {
    "term": { "image_name": "Ha2.003r" }
  }
}
```

Find the canvas record for a specific image:

```json
{
  "query": {
    "term": { "image_name": "Ha2.003r" }
  },
  "_source": ["id", "manifest_id", "label", "position"]
}
```
