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
- **Metadata** — title, repository, shelfmark, date, dimensions, authors, readers, license, etc.
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

### Annotation Pages

Annotation pages contain textual and descriptive annotations. The generator produces annotation pages for:
- **AoR annotations** — marginalia (with HTML rendering, people/books/locations, multi-language), underline, mark, symbol, numeral, errata, drawing, graph, table, calculation, physical_link
- **Illustrations** — titles, characters, textual elements, costume, objects, landscapes, architecture
- **Transcriptions** — TEI transcription text (from `{book-id}.transcription.xml`) rendered as HTML with poetry lines, rubrics, and catchphrases, split per-page at `<pb>` elements
- **HTML annotations** — collection-level HTML annotation content

## Serialization

All JSON output is:
- Compact (single-line)
- UTF-8 encoded
- Deterministic property ordering
- No null values, empty strings, empty arrays, or empty objects

Books with no images are skipped with a warning message.
