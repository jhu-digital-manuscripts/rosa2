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
      <page>.xml              (AoR transcription files)
      <page>.tif              (page images)
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
| `{page-id}.xml` | AoR annotation transcription XML for each annotated page |
| Image files (`.tif`, `.jpg`) | Page images (excluded by shallow-copy) |

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
