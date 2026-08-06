# Archive Directory Structure

## Purpose

This document describes the archive directory hierarchy that the rosa2 tool reads and processes. Understanding this structure is necessary for administering archive content and troubleshooting data issues.

## Audience

Content administrators who manage archive data and developers working on the rosa2 codebase.

## Hierarchy

An archive is a root directory containing one or more collections. Each collection contains one or more books. Each book contains metadata, image references, and transcription files.

```
<archive>/
  <collection-id>/
    collection_metadata.xml
    character_names.csv
    illustration_titles.csv
    narrative_sections.csv
    <book-id>/
      <book-id>.description_en.xml
      <book-id>.images.csv
      <book-id>.images.crop.csv
      <book-id>.permission_en.html
      <book-id>.SHA1SUM
      <book-id>.transcription.xml       (TEI XML transcription)
      <book-id>.transcription.<page>.txt (per-page text transcription source files)
      <book-id>.aor.<page>.xml          (AoR annotation files)
      <page>.tif                         (page images)
      ...
```

## Collections

A collection groups related books. Each collection directory contains:

| File | Description |
|------|-------------|
| `collection_metadata.xml` | Collection-level metadata (languages, label, description) |
| `character_names.csv` | Character name mappings for this collection |
| `illustration_titles.csv` | Illustration title reference data |
| `narrative_sections.csv` | Narrative section/scene definitions |

Collection identifiers (directory names) are typically short labels like `rose`, `pizan`, `dlmm`, or `aor`.

## Books

A book represents a single digitized manuscript or printed work. Each book directory contains:

| File Pattern | Description |
|--------------|-------------|
| `{book-id}.description_en.xml` | Book metadata (title, repository, shelfmark, date, dimensions, authors) |
| `{book-id}.images.csv` | Image list with filenames, dimensions, and roles |
| `{book-id}.images.crop.csv` | Cropped image data for display |
| `{book-id}.permission_en.html` | Usage permission and license text |
| `{book-id}.SHA1SUM` | SHA1 checksums for integrity verification |
| `{book-id}.transcription.xml` | TEI P5 XML transcription (hand-authored or generated from .txt files) |
| `{book-id}.transcription.{page}.txt` | Per-page text transcription source files (custom format) |
| `{book-id}.aor.{page}.xml` | AoR annotation transcription XML for each annotated page |
| Image files (`.tif`, `.jpg`) | Page images (excluded by shallow-copy) |

## Transcription Types

Transcription files come in three forms:

1. **TEI XML** (`{book-id}.transcription.xml`) — A single TEI P5 document containing the full
   manuscript transcription with `<pb>` page breaks. May be hand-authored (e.g., Douce195)
   or generated from per-page text files via `generate-tei` (e.g., Douce332).

2. **Per-page text** (`{book-id}.transcription.{page}.txt`) — Source files in a custom text format.
   Each file contains a `[folio col]` header followed by lines of poetry, rubrics, illustrations,
   annotations, and catchphrases in a simple markup. These are converted to the TEI XML file
   using the `generate-tei` command.

3. **AoR annotation XML** (`{book-id}.aor.{page}.xml`) — Archaeology of Reading transcription files.
   These record reader annotations (marginalia, underlines, marks, symbols, errata, drawings,
   graphs, tables, calculations, physical links) and are NOT converted to TEI. They are processed
   separately for IIIF annotation pages and Opensearch annotation documents.

## File Naming Conventions

- Book IDs typically include an identifier like `Ha2`, `2862`, or a descriptive name.
- Page-level XML files follow the pattern `{book-id}.{folio}{recto/verso}.xml` (e.g., `Ha2.001r.xml`).
- Image files mirror the page naming but with image extensions.
- The image list CSV defines the canonical ordering of pages within a book.

## Image Locations

Images within a book are categorized by location:

- **Front matter** — binding, flyleaves, title pages
- **Body matter** — main content pages
- **End matter** — back flyleaves, binding
- **Miscellaneous** — unclassified images

These locations are encoded in the images CSV and used to generate IIIF Ranges.
