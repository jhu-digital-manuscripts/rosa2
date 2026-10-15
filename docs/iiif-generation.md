# IIIF Presentation 3.0 Generation

## Purpose

This document describes how the rosa2 tool generates static IIIF Presentation API 3.0 JSON files from archive data. These files can be served by any static web server and consumed by IIIF-compatible viewers.

## Audience

Developers maintaining the IIIF generation code and content administrators deploying generated files.

## Overview

The `generate-iiif-pres` command reads an archive and produces a file hierarchy that maps directly to IIIF resource URLs. The output conforms to the IIIF Presentation API 3.0 specification, using `type`, `id`, `items`, and language map labels throughout.

## Output File Hierarchy

```
<output-dir>/
  collection.json                         (top-level Collection)
  <collection-id>/
    collection.json                       (sub-collection listing manifests)
    <book-id>/
      manifest.json                       (Manifest for the book)
      description.xml                     (TEI book description, DLMM books only)
```

When a `--base-url` is provided, all `id` values use that URL as prefix. Otherwise, relative paths are used.

## Resource Types

### Collections

The top-level `collection.json` lists all sub-collections. Each sub-collection `collection.json` lists all manifests within that collection. Both use:
- `@context: "http://iiif.io/api/presentation/3/context.json"`
- `type: "Collection"`
- `items` array containing references to child resources

### Manifests

One manifest per book. Contains:
- **Metadata** — title, repository, shelfmark, date, dimensions, authors, readers, etc.
- **requiredStatement** — attribution text from `{book-id}.permission_{lang}.html`, displayed per IIIF 3.0 spec as a statement that must be shown when the resource is displayed
- **rights** — license URL from `{book-id}.metadata.xml` `<license><url>` element, must be a Creative Commons or RightsStatements.org URI per IIIF 3.0 spec
- **seeAlso** — the TEI book description, see [Book descriptions](#book-descriptions)
- **Items** — array of Canvases, one per page image
- **Structures** — Ranges grouping Canvases by image location, text segments, and illustrations
- **Thumbnail** — reference to the first canvas image
- **Viewing hints** — `viewingDirection: "left-to-right"` and `behavior: ["paged"]` for multi-image books; omitted for single-image books

### Canvases

One canvas per page image. Each canvas has:
- A **label** resolved in priority order: AoR pagination, then signature, then image filename
- **Width and height** from the image dimensions
- A **painting annotation** with an IIIF Image Service (`ImageService2` by default, `ImageService3` with `--image-api-version 3`)
- A **thumbnail** reference
- `viewingHint: "non-paged"` for binding or miscellaneous images

Canvas URIs use a 0-based index as the path segment: `{base-url}/{collection-id}/{book-id}/canvas/{i}`. This index corresponds directly to the `page_num` field in the Opensearch canvas index, allowing clients to construct canvas URIs from search results using `manifest_id` and `page_num`.

### Annotation Pages

Annotation pages contain textual and descriptive annotations. The generator produces annotation pages for:
- **AoR annotations** — a page header (pagination, signature, reader), then one annotation per marginal note, underline, mark, symbol, numeral, erratum, drawing, graph, table, calculation and physical link, ordered by place on the page: head, left margin, in text, right margin, tail, full page. Marginal notes include the translation, hand, people, books, locations, cross-references and internal references. A note written across several places is placed by its first `<position>`; the other parts are labelled, e.g. `[Right margin]`. Underlines and errata, which have no place in the transcription, are in text.
- **Illustrations** — titles, characters, textual elements, costume, objects, landscapes, architecture

Numeric entries in the "Illustration title" and "Characters" columns of
`{book-id}.imagetag.csv` are references into the collection's
`illustration_titles.csv` and `character_names.csv` and are resolved to names;
non-numeric entries are used literally. A reference that cannot be resolved
aborts generation rather than leaking a bare id into the output. Characters are
displayed as the site name with the English name in parentheses when it differs
(for example `Haine (Hate)`). Illustration Ranges use the resolved titles as
their label.
- **Transcriptions** — the TEI transcription (`{book-id}.transcription.xml`) split by page at `<pb n>` and by column at `<cb n>`, one annotation per column. Repeated `<pb>` elements with the same `n` continue the page.
- **Commentary** — collection-level HTML annotations (`annos.jsonld`)

### Annotation bodies

Commenting annotations follow the IIIF cookbook recipes
[HTML in Annotations (0019)](https://iiif.io/api/cookbook/recipe/0019-html-in-annotations/)
and [CSS in an Annotation (0045)](https://iiif.io/api/cookbook/recipe/0045-css/).
The body is a `SpecificResource` whose `styleClass` names CSS classes, wrapping an
HTML `TextualBody`. With `--annotation-stylesheet-url` the annotation also has a
`stylesheet`:

```json
{
  "id": ".../canvas/9/annotations/annotation/3",
  "type": "Annotation",
  "motivation": "commenting",
  "stylesheet": "https://example.org/aor/styles/aor.css",
  "body": {
    "type": "SpecificResource",
    "styleClass": "rosa-anno aor-mark aor-place-intext",
    "source": {
      "type": "TextualBody",
      "format": "text/html",
      "language": "la",
      "value": "<p><b>Mark</b> Plus sign <small>pen</small></p><p>\u201cartium usum bonarum\u201d</p>"
    }
  },
  "target": ".../canvas/9"
}
```

`language` is lowercase and left out when unknown. The HTML uses only `a`, `b`,
`br`, `i`, `img`, `p`, `small`, `span`, `sub` and `sup`, with `href` on `a` and
`src`/`alt` on `img`, and starts with `<` and ends with `>`
(`AnnotationHtml.findViolation` checks this).

Style classes (every annotation also has `rosa-anno`):

| Collection | Classes |
|---|---|
| AoR | `aor-page`, or `aor-{type}` with type `marginalia`, `underline`, `mark`, `symbol`, `numeral`, `errata`, `drawing`, `graph`, `table`, `calculation` or `link`, plus `aor-place-{head\|left_margin\|intext\|right_margin\|tail\|full_page}` when the place is known |
| DLMM | `dlmm-illustration`; `dlmm-transcription` with `dlmm-column-{n}`; `dlmm-commentary` |

HTML conventions, which the stylesheets in `rosa-viewer/public/styles/` rely on:

| Content | HTML |
|---|---|
| Heading (first paragraph) | `<p><b>Type</b> detail <small>extra</small></p>` |
| Section heading | `<p><b>Miniature description</b></p>` |
| Label and value | `<p><b>Costume:</b> Ankle-length gown</p>` |
| Reader's or scribe's text | `<p>…</p>` |
| Translation | `<p><i>…</i></p>` |
| Metadata line | `<p><small><b>People:</b> …</small></p>` |
| Line group (`lg`) | one `<p>`; each line `<span>…</span>` separated by `<br>` |
| Logical line number (`l/@n`), every 5th | `<sub>` at the start of the line |
| Lecoy line number (`milestone[@ed='lecoy']` inside the `l` or right after it), every 5th, leading zeros dropped | `<sup>` at the end of the line |
| Initial / rubric / expansion | `<b>M</b>` / `<b><i>…</i></b>` / `<i>…</i>` |
| Addition / deletion | `⸌…⸍` / `⟦…⟧` |
| Miniature (`figure`) | `<p><b>Miniature:</b> <i>head</i><br><small><b>Characters:</b> …</small></p>` |
| Editorial note (`note`) | `<small>*&nbsp;note</small>` where the note occurs |

### Book descriptions

When a book's English description `{book-id}.description_en.xml` has a
`<notesStmt>` with at least one non-empty `<note>`, the file is copied unchanged
to `{collection-id}/{book-id}/description.xml` and the manifest references it
following the IIIF cookbook recipe
[Linking to Structured Metadata (0053)](https://iiif.io/api/cookbook/recipe/0053-seeAlso/):

```json
"seeAlso": [{
  "id": "https://example.org/iiif/rose/Douce332/description.xml",
  "type": "Dataset",
  "label": { "en": ["Book description (TEI)"] },
  "format": "application/tei+xml",
  "profile": "http://www.tei-c.org/ns/1.0"
}]
```

Books without such a description have no `description.xml` and no `seeAlso`.
The viewer's book description plugin (`rosa-viewer/src/plugins/description/`)
looks for this entry and renders the `notesStmt` sections.

## Serialization

All JSON output is:
- Compact (single-line)
- UTF-8 encoded
- Deterministic property ordering
- No null values, empty strings, empty arrays, or empty objects

Books with no images are skipped with a warning message.
