# Search Service Refactoring Plan

## Summary

Refactor the jhsearch Opensearch indexes from three indexes (manifest, canvas, annotation) to two indexes (manifest, canvas). All annotation data is merged into the canvas document with per-type independently searchable fields. Several fields are renamed, added, or restructured.

## Changes Overview

1. Remove annotation index — merge into canvas with per-type fields
2. Remove `titles` field — merge into `title`
3. Rename `number_of_illustrations` → `num_illustrations`
4. `collection_id` becomes array of all ancestor collection IDs
5. Add `label` to manifest index
6. Add `thumbnail` field to manifest index
7. Add `logo` field to manifest index
8. Remove `text` catch-all field
9. Update documentation

---

## 1. Remove Annotation Index — Merge Into Canvas

The separate `annotation` index is removed. All annotation data that targets a canvas is accumulated into the canvas document. Each annotation type gets its own independently searchable field so users can target specific types (e.g. search only marginalia, or only marks).

### Canvas Document Fields

#### Structural Fields

| Field | Type | Description |
|-------|------|-------------|
| `id` | keyword | Unique canvas ID (`collection.book.image`) |
| `manifest_id` | keyword | Parent manifest/book ID |
| `collection_id` | keyword (array) | All ancestor collection IDs (see §4) |
| `label` | text | Page label (pagination, signature, or image name) |
| `image_name` | keyword | Short image identifier |
| `position` | integer | 1-based page position in the book |

#### Multi-Language Text Fields (annotation data)

Each of these fields is an object with language sub-fields (`.en`, `.fr`, `.la`, `.it`, `.el`, `.es`, `.de`, `.ofr`). All annotations on the canvas are accumulated into the appropriate field. Content from multiple annotations of the same type is concatenated.

| Field | Description | Source |
|-------|-------------|--------|
| `marginalia` | Marginalia referenced text, transcription, and translation | Marginalia annotations |
| `underline` | Underlined text in the printed book | Underline annotations |
| `mark` | Mark names (keyword sub-field) and mark referenced text | Mark annotations |
| `symbol` | Symbol names (keyword sub-field) and symbol referenced text | Symbol annotations |
| `errata` | Corrections: referenced text + amended text | Errata annotations |
| `numeral` | Numeral text and values | Numeral annotations |
| `drawing` | Drawing type (keyword sub-field), text, and translation | Drawing annotations |
| `emphasis` | Emphasized/underlined text within marginalia | Marginalia emphasis |
| `cross_reference` | Cross-reference people, titles, and text | Marginalia XRefs |
| `calculation` | Calculation type (keyword sub-field), data, and content | Calculation annotations |
| `graph` | Graph type (keyword sub-field), node text, and translations | Graph annotations |
| `table` | Table type (keyword sub-field), text, cells, and translation | Table annotations |
| `transcription` | Transcription of manuscript text | Transcription XML |
| `illustration` | Illustration descriptions and character names | Illustration tagging |
| `translation` | Translations (from marginalia, drawings, tables) | Various |
| `anchor_text` | Anchor text from drawings and tables | Drawing/Table TextEl |

#### Keyword Sub-Field Pattern for Type-Specific Fields

Fields like `mark`, `symbol`, `drawing`, `graph`, `table`, and `calculation` contain both enumerated keyword values (the type/name) and free text. In Opensearch this is modeled with a `keyword` sub-field alongside the language sub-fields:

```json
"mark": {
  "type": "object",
  "properties": {
    "keyword": { "type": "keyword" },
    "en": { "type": "text", "analyzer": "english" },
    "fr": { "type": "text", "analyzer": "french" },
    ...
  }
}
```

**Indexing:** The mark name (e.g. `"plus_sign"`) is written to `mark.keyword`. The mark's referenced text is routed to the appropriate language sub-field (e.g. `mark.en`).

**Searching:** To search for a specific mark type: `mark.keyword: "plus_sign"`. To full-text search mark referenced text: `mark.en: "some text"`. To search marks broadly: query across `mark.*`.

This same pattern applies to `symbol`, `drawing`, `graph`, `table`, and `calculation`.

#### Keyword Array Fields

| Field | Type | Description |
|-------|------|-------------|
| `people` | keyword (array) | People referenced across all annotations, with alternates |
| `books` | keyword (array) | Books referenced across all annotations, with alternates |
| `locations` | keyword (array) | Locations/places referenced, with alternates |
| `symbols` | keyword (array) | All symbol names on this canvas |
| `method` | keyword (array) | Methods used (pen, chalk, etc.) |
| `hand` | keyword (array) | Hands identified |
| `annotator` | keyword (array) | Annotator(s) for this page |
| `language` | keyword (array) | All annotation languages on this canvas |
| `marginalia_language` | keyword (array) | Languages used within marginalia specifically |
| `topic` | keyword (array) | Marginalia topics |
| `char_name` | keyword (array) | Character names (rose collection, from illustrations) |

#### Mark Enumerated Values

The `mark.keyword` sub-field accepts these values:

ampersand, apostrophe, arrow, box, bracket, circumflex, colon, comma, dash, diacritic, dot, double_vertical_bar, equal_sign, est_mark, hash, horizontal_bar, page_break, pen_trial, pin, plus_sign, quotation_mark, quattuorpunctus, quattuorpunctus_with_tail, scribble, section_sign, semicolon, slash, straight_quotation_mark, small_circle, tick, tilde, triple_dash, tripunctus, tripunctus_with_tail, duopunctus_with_antenna, vertical_bar, X_sign, dagger, quinquepunctus, arrowhead, Ichthys, w_mark, guillemet, lightening_bolt, hook

#### Symbol Enumerated Values

The `symbol.keyword` sub-field accepts these values:

Asterisk, Bisected_circle, Crown, JC, HT, Hieroglyphic_Monad, Jupiter, LL, Mars, Mercury, Moon, Opposite_planets, Conjunction, Salt, Saturn, Florilegium, Square, Trine, SS, Sulfur, Sun, Venus, Aries, Cancer, Libra, Capricorn, Taurus, Leo, Scorpio, Aquarius, Gemini, Virgo, Sagittarius, Pices, North_Node, South_Node, Sextile, Phi, Simeiosi, Unidentified

#### Drawing Enumerated Values

The `drawing.keyword` sub-field accepts these values:

arrow, atoms, cone, pyramid, egg, grave, axe, face, heart, manicule, mountain, florilegium, crown, coat_of_arms, scientific_instrument, animal, chain, canon, divining rod, shield, map, saddle, church, star, sword, house, ship, dragon, person, scroll, triangle, one_point_perspective_drawing, geometric_diagram, sceptre

---

## 2. Remove `titles` — Merge Into `title`

The separate `titles` field (which was a text array of BookText titles) is removed from the manifest index. All title content is placed into the multi-language `title` field instead. The `title` field already has language sub-fields; BookText titles are appended to the appropriate sub-field (defaulting to `.en`).

---

## 3. Rename `number_of_illustrations` → `num_illustrations`

In the manifest index mapping, the `OpensearchIngestGenerator` output, and `JHSearchInfoGenerator` category references.

---

## 4. `collection_id` Becomes Array of All Ancestor Collection IDs

Both manifest and canvas documents will have `collection_id` as a keyword array containing:
- The immediate collection ID
- All parent collection IDs (from `BookCollection.getParentCollections()`, resolved recursively)

This allows searching across a parent collection to find documents in all sub-collections.

---

## 5. Add `label` to Manifest Index

| Field | Type | Value |
|-------|------|-------|
| `label` | text | `BiblioData.getCommonName()` — the human-readable common name of the book |

This is equivalent to the old `OBJECT_LABEL` / `MANIFEST_LABEL` field.

---

## 6. Add `thumbnail` to Manifest Index

| Field | Type | Description |
|-------|------|-------------|
| `thumbnail` | keyword (array) | Up to 3 canvas IDs for representative page images |

**Logic:** Iterate through the book's images in order. Skip images where:
- `getLocation() == FRONT_MATTER`
- `getLocation() == BINDING`
- `isMissing() == true`

Take the first 3 eligible canvas IDs. If fewer than 3 are available, return however many exist (1 or 2).

---

## 7. Add `logo` to Manifest Index

| Field | Type | Description |
|-------|------|-------------|
| `logo` | keyword | Logo image filename for this book |

**Logic:**
- Default (all non-aor collections): `collectionId + ".jpg"` (e.g. `rose.jpg`, `pizan.jpg`)
- For the `aor` collection: Take the first reader from `BiblioData.getReaders()[0].getName()`, convert to lowercase, replace spaces with underscores, append `.jpg`. Example: `"John Dee"` → `john_dee.jpg`

---

## 8. Remove `text` Catch-All Field

The old system duplicated all annotation content into a generic `TEXT` field for broad search. This is removed. Users search specific annotation-type fields directly. Broad search across all types can be accomplished by querying multiple fields in the Opensearch query DSL (multi_match across `marginalia.en`, `mark.en`, `underline.en`, etc.).

---

## 9. Update Documentation

Update `doc/opensearch-indexes.md` and `doc/search.md` to reflect:
- Two indexes (manifest + canvas) instead of three
- New canvas document structure with per-type fields
- Keyword sub-field pattern and how to search it
- New manifest fields (label, thumbnail, logo)
- Renamed fields (num_illustrations)
- Removed fields (titles, text, annotation index)
- collection_id array semantics

---

## Implementation Tasks

| # | Task | Files |
|---|------|-------|
| 1 | Rewrite `opensearch/canvas.json` with full merged schema | `opensearch/canvas.json` |
| 2 | Delete `opensearch/annotation.json` | `opensearch/annotation.json` |
| 3 | Update `opensearch/manifest.json` | `opensearch/manifest.json` |
| 4 | Rewrite `CanvasDoc.java` record | `rosa-tool/.../opensearch/CanvasDoc.java` |
| 5 | Delete `AnnotationDoc.java` | `rosa-tool/.../opensearch/AnnotationDoc.java` |
| 6 | Update `ManifestDoc.java` record | `rosa-tool/.../opensearch/ManifestDoc.java` |
| 7 | Rewrite `OpensearchIngestGenerator.java` | `rosa-tool/.../opensearch/OpensearchIngestGenerator.java` |
| 8 | Update `JHSearchInfoGenerator.java` | `rosa-tool/.../iiif/JHSearchInfoGenerator.java` |
| 9 | Update `doc/opensearch-indexes.md` | `doc/opensearch-indexes.md` |
| 10 | Update `doc/search.md` | `doc/search.md` |

---

## Field Mapping From Old Code

This table shows how every old `JHSearchField` maps to the new structure:

| Old Field | New Location | Notes |
|-----------|-------------|-------|
| OBJECT_ID | Removed | Replaced by manifest_id / canvas id |
| OBJECT_TYPE | Removed | Implicit from index (manifest vs canvas) |
| OBJECT_LABEL | `label` (on both indexes) | manifest: commonName; canvas: pagination/signature/name |
| COLLECTION_ID | `collection_id` (array) | Now includes all ancestors |
| MANIFEST_ID | `manifest_id` | Unchanged |
| MANIFEST_LABEL | Captured by manifest `label` field | |
| IMAGE_NAME | `image_name` | Unchanged |
| MARGINALIA | `marginalia` (multi-lang) | On canvas |
| UNDERLINE | `underline` (multi-lang) | On canvas |
| EMPHASIS | `emphasis` (multi-lang) | On canvas |
| ERRATA | `errata` (multi-lang) | On canvas |
| MARK | `mark` (keyword + multi-lang) | On canvas; keyword sub-field for mark names |
| SYMBOL | `symbol` (keyword + multi-lang) | On canvas; keyword sub-field for symbol names |
| NUMERAL | `numeral` (multi-lang) | On canvas |
| DRAWING | `drawing` (keyword + multi-lang) | On canvas; keyword sub-field for drawing types |
| CROSS_REFERENCE | `cross_reference` (multi-lang) | On canvas |
| TRANSCRIPTION | `transcription` (multi-lang) | On canvas |
| ILLUSTRATION | `illustration` (multi-lang) | On canvas |
| LANGUAGE | `language` (keyword array) | On canvas |
| MARGINALIA_LANGUAGE | `marginalia_language` (keyword array) | On canvas |
| BOOK | `books` (keyword array) | On canvas |
| METHOD | `method` (keyword array) | On canvas |
| CALCULATION | `calculation` (keyword + multi-lang) | On canvas |
| GRAPH | `graph` (keyword + multi-lang) | On canvas |
| TABLE | `table` (keyword + multi-lang) | On canvas |
| HAND | `hand` (keyword array) | On canvas |
| ANNOTATOR | `annotator` (keyword array) | On canvas |
| TITLE | `title` (multi-lang) | On manifest |
| PEOPLE | `people` (keyword array) | On canvas |
| PLACE | `locations` (keyword array) | On canvas; renamed from PLACE |
| REPO | `repository` (text) | On manifest |
| DESCRIPTION | `description` (text) | On manifest |
| TEXT | Removed | Search type-specific fields directly |
| CHAR_NAME | `char_name` (keyword array) | On canvas |
