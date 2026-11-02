# IIIF Presentation 3.0 Generation

## Purpose

This document describes everything the `generate-iiif-pres` command writes: the output
files, the identifiers inside them, how collections, manifests, canvases and annotation
pages are built from archive data, and the HTML conventions of annotation bodies. It is
the reference for the viewer, for the stylesheets in `rosa-viewer/public/styles/` and for
anyone serving the files.

For the command's options see [usage.md](usage.md#generate-iiif-pres); for the archive
files it reads see [archive-structure.md](archive-structure.md); for how the output
relates to the search indexes see [opensearch-indexes.md](opensearch-indexes.md).

## Audience

Developers maintaining the IIIF generation code and the viewer, and content
administrators deploying generated files.

## Overview

The command reads every collection in the archive and produces a flat file hierarchy of
IIIF Presentation API 3.0 resources that any static web server can serve. Output uses
`type`, `id`, `items` and language-map labels throughout, and is compact, deterministic
JSON (see [Serialization](#serialization)).

## Output files

```
<output-dir>/
  collection.json                          top-level Collection: the root collections
  <collection-id>/
    collection.json                        the collection's child collections and manifests
    service/jhsearch.json                  search service description (only with --opensearch-url)
    <book-id>/
      manifest.json                        the book
      description.xml                      TEI book description (only when the book has one, see below)
      canvas/<i>/annotations.json          commenting annotations of page i (only pages that have any)
```

Every collection directory is written directly under the output directory, child
collections included: `dlmm/`, `rose/` and `pizan/` are siblings even though `rose` and
`pizan` list `parents=dlmm` in their `config.properties`. The parent/child relationship
appears only inside the collection files.

The output directory is not cleaned first; stale files from earlier runs survive
(`rosa-viewer/scripts/package-site.sh` deletes its output directory before generating).
Books whose image list is missing or empty are skipped with a warning on stderr, and a
summary line gives the number skipped. A canvas numbering of the skipped book's pages
therefore never exists, and `generate-opensearch-ingest` still indexes such a book.

## Identifiers

`{base}` is `--base-url` without its trailing slash. Without `--base-url`, ids are the
paths below without a leading slash (relative ids). `{i}` and `{n}` are 0-based.

| Resource | `id` |
|---|---|
| Top-level collection | `{base}/collection.json` |
| Collection | `{base}/{collection}/collection.json` |
| Manifest | `{base}/{collection}/{book}/manifest.json` |
| Canvas | `{base}/{collection}/{book}/canvas/{i}` |
| Painting annotation page / annotation | `{canvas}/page`, `{canvas}/page/annotation` |
| Commenting annotation page | `{canvas}/annotations.json` |
| Commenting annotation | `{canvas}/annotations/annotation/{n}` |
| Table of contents Range | `{base}/{collection}/{book}/range/top` |
| Image location Range | `{base}/{collection}/{book}/range/location/{location}` with `location` one of `binding`, `front_matter`, `body_matter`, `end_matter`, `misc` |
| Text Range | `{base}/{collection}/{book}/range/text/{n}` |
| Illustration Range | `{base}/{collection}/{book}/range/illustration/{n}` |
| Book description (`seeAlso`) | `{base}/{collection}/{book}/description.xml` |
| JHSearch service | `{base}/{collection}/service/jhsearch.json` |

Collection, manifest, annotation page and description ids name the file they are
written to, so a static server dereferences them as is. Canvas, Range and annotation ids
are identifiers only and are not served.

### Image identifiers

Image services and image URLs use `--image-base-url`, or `--base-url` when it is not
given. The IIIF Image API identifier of an image is

```
{collection}/{book}/[cropped/]{image id without its extension}
```

encoded as a single path segment, so the slashes become `%2F`:
`https://image.example.org/iiif/3/rose%2FDouce195%2Fcropped%2FDouce195.001r`. The
`cropped/` segment is present when the canvas paints the cropped version of the image
(see [Canvases](#canvases)). A missing image (`*` in the image list) is painted with the
collection's placeholder, whose identifier has no book segment:
`{collection}/missing_image` (from `missing_image.tif`; its size comes from the
collection's `config.properties`). The placeholder exists on the image server only for
collections that have missing images; today that is `rose`.

The same identifier, unencoded, is the `iiif_image_id` of the Opensearch documents, so
search results and manifests point at the same images.

### Image API version

`--image-api-version` selects the service description on every painting body:

| Version | `service.type` | `service.profile` |
|---|---|---|
| `2` (default) | `ImageService2` | `http://iiif.io/api/image/2/level2.json` |
| `3` | `ImageService3` | `level2` |

The painting body's own `id` is `{service}/full/max/0/default.jpg` in both versions.
Image API 3 removed the `full` size, and `max` is also valid in version 2, so one URL
serves both; on a server with a pixel limit, `max` scales the image down instead of
failing. Thumbnails are `{service}/full/80,/0/default.jpg` with `width` 80 and `height`
scaled from the image's dimensions (rounded, at least 1); the size is omitted when the
image's dimensions are unknown.

## Collections

**Top-level collection** (`collection.json`): `label` `{"none": ["rosa2 Archive"]}`,
`items` one Collection reference per root collection, that is per collection whose
`config.properties` has no `parents`. Child collections are not listed here.

**Collection** (`{collection}/collection.json`): `label` from the collection's `label`
property, in the collection's first `languages` entry (`none` when the collection declares
no languages; the id when it has no label). `items` lists the collections named in
`children`, as Collection references carrying their own configured labels, followed by one
Manifest reference per book with the manifest label and an 80 px thumbnail of the book's
first image (without a size, since only the image id is known here). With
`--opensearch-url`, the collection also carries the [JHSearch service](#jhsearch-service).

A collection with no books, such as `dlmm`, still gets a `collection.json` (child
collections only) and a `service/jhsearch.json`.

## Manifests

One manifest per book with a non-empty image list.

- **`label`**: the `commonName` of the book's bibliographic data in the collection
  language, else of the first bibliographic record, else its `title`, else the book id.
- **`metadata`**: label/value pairs tagged with the collection language. Empty values are
  left out. In order: Title, Repository, Shelfmark, Current Location, Origin, Date, Type,
  Material (from `{book}.metadata.xml` `<bibliography>`), Readers, Authors, Websites
  (joined with `; `), Dimensions (`{width}{units} x {height}{units}`), Year Start, Year
  End, Number of Pages, Number of Illustrations.
- **`requiredStatement`**: label `{"en": ["Attribution"]}`; the value is the text of
  `{book}.permission_en.html`, or the permission in the first collection language that
  has one, tagged with that language. Omitted when the book has no permission file.
- **`rights`**: the `<license><url>` of `{book}.metadata.xml`, written as given. IIIF 3.0
  requires a Creative Commons or RightsStatements.org URI; any other value is written with
  a warning on stderr.
- **`seeAlso`**: the book description, see [Book descriptions](#book-descriptions).
- **`viewingDirection`** `left-to-right` and **`behavior`** `["paged"]` when the book has
  more than one image; both omitted for single-image books.
- **`thumbnail`**: the first image of the image list, 80 px wide, with its size; the
  cropped version when that image has a cropped entry.
- **`items`**: the canvases, see [Canvases](#canvases).
- **`structures`**: a single Range labelled "Table of Contents" (`range/top`) whose
  `items` are, in order: one Range per image location present in the book (label is the
  location's display name, e.g. `front matter`), one Range per `<text>` of
  `{book}.metadata.xml` that has a title or first page (label the text title, else
  `Text n`; its canvases run from the image whose name contains the text's first page to
  the one containing its last page), and one Range per illustration in
  `{book}.imagetag.csv` whose page can be matched to an image (label the resolved
  illustration titles joined with `, `, else the illustration id; a single canvas).
- **`service`**: the [JHSearch service](#jhsearch-service), with `--opensearch-url`.

Canvases are not marked `non-paged`; binding and miscellaneous images take part in the
paged sequence like every other image.

### Book descriptions

When a book's English TEI description `{book}.description_en.xml` has a `<notesStmt>`
with at least one non-empty `<note>`, the file is copied unchanged to
`{collection}/{book}/description.xml` and the manifest references it following the IIIF
cookbook recipe
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

The rule applies to every collection, but only DLMM books have such descriptions. Books
without one have no `description.xml` and no `seeAlso`. The viewer's book description
plugin (`rosa-viewer/src/plugins/description/`) looks for this entry and renders the
`notesStmt` sections.

## Canvases

The canvases of a manifest follow `{book}.images.csv`: canvas `i` is row `i` of the
image list (0-based), in its order, and `i` is also the `page_num` of that page's
document in the Opensearch `canvas` index. Ranges, annotation files and search results all
use this numbering, so a client can build a canvas URI from a search hit as
`{base}/{collection}/{book}/canvas/{page_num}` (see
[opensearch-indexes.md](opensearch-indexes.md#constructing-iiif-uris-from-search-results)).

When the book has `{book}.images.crop.csv`, each canvas looks up the cropped entry with
the same image id. If one exists and is not marked missing, the canvas paints the cropped
image: its `width` and `height` are the cropped dimensions and the image identifier gets
the `cropped/` segment. Otherwise the canvas paints the image from the image list. Entries
of the crop list that are not in the image list are ignored (`check` warns about them),
so a crop list that differs from the image list can never shift canvas numbers.

Each canvas has:

- **`label`**: the page's AoR `pagination`, else its `signature` (from the
  `{book}.aor.{page}.xml` whose `page/@filename` matches), else the image's label from the
  image list, else the name derived from the image file name (e.g. `1r`, `binding front
  cover`).
- **`width`** and **`height`** of the painted image; `1000` by `1000` when the image
  list gives no dimensions.
- **`items`**: one AnnotationPage (`{canvas}/page`) with one `painting` Annotation whose
  body is the image (`format` `image/jpeg`, the canvas dimensions, and the image service,
  see [Image API version](#image-api-version)). A missing image is painted with the
  collection placeholder.
- **`thumbnail`**: 80 px wide, sized from the painted image; no size for a missing image.
- **`annotations`**: a reference to `{canvas}/annotations.json` when the page has any
  commenting annotation (see below); absent otherwise.

## Annotation pages

`{collection}/{book}/canvas/{i}/annotations.json` is a standalone AnnotationPage (it
carries `@context`) whose `items` are `commenting` annotations targeting the canvas,
numbered from 0 in this order:

1. **AoR annotations** from `{book}.aor.{page}.xml`: first a page header (pagination or
   signature as the heading, the signature as extra detail when both exist, and the
   reader), then one annotation per marginal note, drawing, graph, table, calculation,
   symbol, numeral, mark, underline, erratum and physical link. Annotations are ordered by
   place on the page, head, left margin, in text, right margin, tail, full page, and
   within a place by the type order just given. A marginal note is placed by its first
   `<position>`; other parts are prefixed with their place, e.g. `[Right margin]`.
   Underlines and errata, which have no place in the transcription, are placed in text.
   Annotations without a place come last within their type.
2. **Illustrations**: one annotation per row of `{book}.imagetag.csv` for the page.
   Numeric entries in the `Illustration title` and `Characters` columns are references
   into the collection's `illustration_titles.csv` and `character_names.csv` and are
   resolved to names; other entries are used literally; a numeric reference without a
   matching row aborts generation. Characters display as the site name with the English
   name in parentheses when it differs (`Haine (Hate)`).
3. **Transcription**: the TEI transcription `{book}.transcription.xml` split by page at
   `<pb n>` and by column at `<cb n>`, one annotation per column. Repeated `<pb>` elements
   with the same `n` continue the page.
4. **Commentary**: the collection's `annos.jsonld` entry targeting the image, HTML passed
   through unchanged.

The body `language` is `en` for the page header, the annotation's own language
(lowercased) for AoR annotations when known, the collection language for illustrations
and commentary, and absent for transcriptions, calculations and physical links.

## Annotation bodies

Commenting annotations follow the IIIF cookbook recipes
[HTML in Annotations (0019)](https://iiif.io/api/cookbook/recipe/0019-html-in-annotations/)
and [CSS in an Annotation (0045)](https://iiif.io/api/cookbook/recipe/0045-css/).
The body is a `SpecificResource` whose `styleClass` names CSS classes, wrapping an HTML
`TextualBody`. With `--annotation-stylesheet-url` the annotation also has a
`stylesheet` (see [Annotation stylesheet](#annotation-stylesheet)):

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
      "value": "<p><b>Mark</b> Plus sign <small>pen</small></p><p>“artium usum bonarum”</p>"
    }
  },
  "target": ".../canvas/9"
}
```

The HTML uses only `a`, `b`, `br`, `i`, `img`, `p`, `small`, `span`, `sub` and `sup`,
with `href` on `a` and `src`/`alt` on `img`, and starts with `<` and ends with `>`.
`AnnotationHtml.findViolation` checks this in the tests, not at generation time, and
commentary from `annos.jsonld` is not checked at all.

### Style classes

Every annotation has `rosa-anno`, plus:

| Collection | Classes |
|---|---|
| AoR | `aor-page` for the page header; `aor-{type}` with type `marginalia`, `underline`, `mark`, `symbol`, `numeral`, `errata`, `drawing`, `graph`, `table`, `calculation` or `link`; plus `aor-place-{head\|left_margin\|intext\|right_margin\|tail\|full_page}` when the place is known. The viewer groups annotations by the place class and shows those without one under "Other annotations". |
| DLMM | `dlmm-illustration`; `dlmm-transcription` with `dlmm-column-{n}` (the `<cb n>` value, characters other than letters, digits, `_` and `-` replaced by `_`); `dlmm-commentary` |

The viewer adds classes of its own when it renders a body
(`rosa-viewer/src/plugins/annotations/components/AnnotationBody.jsx`): a paragraph whose
only child is `<b>` gets `rosa-heading`, only `<i>` gets `rosa-translation`, only
`<small>` gets `rosa-meta`, and note markers get `rosa-note-marker`. The stylesheets in
`rosa-viewer/public/styles/{aor,dlmm}.css` rely on those as well as on the generated
classes; change the HTML conventions below and the stylesheets together.

### HTML conventions

| Content | HTML |
|---|---|
| Heading (first paragraph of every item) | `<p><b>Type</b> detail <small>extra</small></p>`, e.g. `<p><b>Mark</b> Plus sign <small>pen</small></p>`, `<p><b>Marginal note</b> <small>Latin, Italian hand, book rotated</small></p>`, `<p><b>Illustration</b></p>` |
| Quoted text from the printed book (referenced or anchor text) | `<p>“…”</p>` |
| Erratum | `<p>“copy text” → “amended text”</p>` |
| Reader's or scribe's text | `<p>…</p>` |
| Part of a marginal note in another place | `<p><small>[Right margin]</small> …</p>` |
| Text element with language or hand | `<p>… <small>(Latin, Italian hand)</small></p>` |
| Translation | `<p><i>…</i></p>` |
| Metadata line | `<p><small><b>People:</b> a, b</small></p>`; labels used: Reader, Anchored to, Topic, People, Books, Locations, Symbols, Underlined, Cross-references, Refers to, Other reader, Date, Continues from, Continues to, Columns |
| Line list (graph nodes, table rows as `a \| b`, calculation lines, physical links `source → target (relationship)`) | one `<p>` with the lines separated by `<br>` |
| Illustration field | `<p><b>Costume:</b> Ankle-length gown</p>`; fields in order: Titles, Characters, Textual elements, Costume, Initials, Objects, Landscape, Architecture, Other |
| Transcription column heading | `<p><b>Column a</b></p>` (omitted when the page has a single unnamed column) |
| Section heading (`head` outside a line) | `<p><b>…</b></p>` |
| Line group (`lg`) | one `<p>`; each line `<span>…</span>` separated by `<br>` |
| Logical line number (`l/@n`), every 5th | `<sub>` at the start of the line |
| Lecoy line number (`milestone[@ed='lecoy']` in or right after the `l`), every 5th, leading zeros dropped | `<sup>` at the end of the line |
| Initial / rubric / expansion (`hi rend="init…"`, `hi rend="rubric"`, `expan`) | `<b>M</b>` / `<b><i>…</i></b>` / `<i>…</i>` |
| Addition / deletion (`add`, `del`) | `⸌…⸍` / `⟦…⟧` |
| Figure (`figure`) | `<p><b>Miniature:</b> <i>head</i><br><small><b>Characters:</b> …</small></p>`; the label is the figure `type` humanized, `Figure` when it has none |
| Gap (`gap`) | `<p><small>[Gap: desc]</small></p>` or `[Gap]` |
| Forme work (`fw`) | `<p><small><b>Catchword:</b> …</small></p>`; `Forme work` or the humanized type for other `fw` types |
| Editorial note (`note`) | `<small>*&nbsp;note</small>` where the note occurs, prefixed `Correction: `, `Reading order: `, `Scribal annotation: ` or the humanized type; `critical` notes have no prefix; `character` notes are inline `<small>…</small>` |

## Annotation stylesheet

`--annotation-stylesheet-url` is a URL template written into every commenting
annotation's `stylesheet`. `{collection}` is replaced by the collection's first non-blank
`parents` entry, else by the collection's own id, so `rose` and `pizan` annotations
reference the `dlmm` stylesheet and `aor` annotations the `aor` one:
`https://example.org/{collection}/styles/{collection}.css` becomes
`https://example.org/dlmm/styles/dlmm.css`. Without the option, annotations have no
`stylesheet`.

## JHSearch service

With `--opensearch-url`, every collection file and manifest carries a service entry:

```json
"service": [{
  "id": "https://iiif.example.org/aor/service/jhsearch.json",
  "type": "JHSearchService2",
  "profile": "https://github.com/jhu-digital-manuscripts/rosa2/blob/main/docs/search.md"
}]
```

and the file `{collection}/service/jhsearch.json` describes how to search that
collection. Each child collection has its own file; `dlmm` has one as well, covering its
children through the `collection_id` field. The viewer's client contract is in
[search.md](search.md). The file has four keys:

```json
{
  "fields": [
    { "name": "marginalia", "label": "Marginalia", "description": "Notes written by a reader.", "has_subfields": true },
    { "name": "mark", "label": "Mark", "description": "Pen marks made on a page...", "has_subfields": true,
      "values": [ { "value": "ampersand", "label": "Ampersand" }, { "value": "arrow", "label": "Arrow" } ] },
    { "name": "method", "label": "Method", "description": "Implement used to create mark or underline.",
      "values": [ { "value": "chalk", "label": "Chalk" }, { "value": "pen", "label": "Pen" } ] }
  ],
  "categories": [
    { "name": "current_location", "label": "Current Location" },
    { "name": "num_illustrations", "label": "Number of Illustrations", "quantize-interval": 10 }
  ],
  "default-fields": ["marginalia", "mark", "method"],
  "opensearch": "https://search.example.org/_search"
}
```

| Key | Description |
|-----|-------------|
| `fields` | The searchable fields, in the order a search form should show them. Each has `name` (the Opensearch field), `label`, `description`, and optionally `has_subfields` and `values`. |
| `fields[].has_subfields` | Present and `true` when the field has language sub-fields (`.en`, `.la`, …) and possibly a `.keyword` sub-field. Query it as `name.*`. |
| `fields[].values` | The enumerated values of a keyword field, `{value, label}` pairs sorted by label. Match one with a `term` query on `name.keyword` when `has_subfields` is true, else on `name`. |
| `categories` | The facet fields of the collection's `manifest` documents: `name` and `label`, plus `quantize-interval` for numeric fields, which are bucketed with a `histogram` aggregation of that interval instead of `terms`. |
| `default-fields` | The fields searched when a query names none; today the same list as `fields`. |
| `opensearch` | The `_search` URL given to `--opensearch-url`. |

Fields and categories are fixed per collection in
`rosa-tool/src/main/java/rosa/archive/iiif/JHSearchInfoGenerator.java`. AoR has twenty
fields (the annotation types, `people`, `books`, `locations`, `language`,
`marginalia_language`, `method`, `hand`, `annotator`) and no categories, so its site shows
only the viewer's built-in Name filter. `rose`, `pizan` and `dlmm` have description,
title, repository, people or character, place and transcription fields and the categories
`current_location`, `date`, `num_illustrations` (interval 10), `num_pages` (interval
100), `origin`, `type` and `has_transcription`. The `language` and
`marginalia_language` values include `iw` (Hebrew) and `cy` (Welsh), which occur in the
data as language codes although their text is analysed as English (see
[opensearch-indexes.md](opensearch-indexes.md#text-analysis)).

## Serialization

All JSON output is:

- Compact (single line) and UTF-8 encoded
- Written with object keys sorted, so output is deterministic
- Free of `null` values, empty strings, empty arrays and empty objects at every object
  level (empty values inside arrays are kept)

Books with no images are skipped with a warning and counted in a summary line on stderr.
