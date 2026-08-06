# Tool Usage

## Purpose

This document describes all commands and options for the rosa2 CLI tool. The tool manages rosa digital manuscript archives, generates IIIF Presentation API 3.0 files, produces Opensearch bulk ingest files, and computes AoR annotation statistics.

## Audience

Developers, content administrators, and system administrators who work with the rosa2 archive.

## General Usage

```
java -jar rosa2-2.0.0-SNAPSHOT.jar <command> [options] [arguments]
```

Use `--help` on any command for detailed usage. All commands exit with code 0 on success and code 1 on error.

## Commands

### generate-iiif-pres

Generates static IIIF Presentation API 3.0 JSON files from archive data.

```
java -jar rosa2.jar generate-iiif-pres --archive <dir> --output <dir> [--base-url <url>] [--image-api-version <2|3>]
```

| Option | Required | Description |
|--------|----------|-------------|
| `--archive` | Yes | Path to the archive directory |
| `--output` | Yes | Path to the output directory (created if missing) |
| `--base-url` | No | Base URL prefix for all resource `id` values. If omitted, relative paths are used. |
| `--image-api-version` | No | IIIF Image API version: `2` (default) or `3` |

### generate-opensearch-ingest

Generates Opensearch bulk ingest JSON files from archive data.

```
java -jar rosa2.jar generate-opensearch-ingest --archive <dir> --output <dir>
```

| Option | Required | Description |
|--------|----------|-------------|
| `--archive` | Yes | Path to the archive directory |
| `--output` | Yes | Path to the output directory (created if missing) |

Produces one `<collection-id>.bulk.json` file per collection in the output directory.

### shallow-copy

Copies metadata files (XML, TXT, HTML, CSV) from the archive, excluding images.

```
java -jar rosa2.jar shallow-copy --archive <dir> --output <dir>
```

| Option | Required | Description |
|--------|----------|-------------|
| `--archive` | Yes | Path to the archive directory |
| `--output` | Yes | Path to the output directory (created if missing) |

Image files (TIF, TIFF, JPG, JPEG, PNG) are excluded regardless of filename case.

### check

Verifies structural data consistency of all collections and books.

```
java -jar rosa2.jar check --archive <dir> [--check-bits]
```

| Option | Required | Description |
|--------|----------|-------------|
| `--archive` | Yes | Path to the archive directory |
| `--check-bits` | No | Also verify bit-level integrity via SHA1 checksums |

Reports errors and warnings to stdout. Exits with code 1 if any errors are found.

### list

Lists collections or books in the archive.

```
java -jar rosa2.jar list --archive <dir> [--collection <id>]
```

| Option | Required | Description |
|--------|----------|-------------|
| `--archive` | Yes | Path to the archive directory |
| `--collection` | No | If provided, lists books in this collection. Otherwise lists all collections. |

Output is one name per line to stdout.

### validate-xml

Validates AoR transcription XML files against the schema.

```
java -jar rosa2.jar validate-xml --archive <dir> --collection <id>
```

| Option | Required | Description |
|--------|----------|-------------|
| `--archive` | Yes | Path to the archive directory |
| `--collection` | Yes | Collection ID to validate |

Reports errors and warnings to stdout.

### aor-stats

Generates statistics about AoR annotations for one or more books.

```
java -jar rosa2.jar aor-stats <book-dir> [<book-dir> ...]
```

| Argument | Required | Description |
|----------|----------|-------------|
| `<book-dir>` | Yes (one or more) | Path(s) to book directories containing AoR XML files |

Writes output CSV files (`book_totals.csv`, `{book_id}.csv`, `vocab_*.csv`) to the current working directory. Invalid directories are skipped with a warning.

### update

Recomputes SHA1 checksums for files in collections and books.

```
java -jar rosa2.jar update --archive <dir> [--collection <id>] [--book <id>] [--force]
```

| Option | Required | Description |
|--------|----------|-------------|
| `--archive` | Yes | Path to the archive directory |
| `--collection` | No | Collection ID. If omitted, updates all collections. |
| `--book` | No | Book ID (requires `--collection`). If omitted, updates all books in the collection. |
| `--force` | No | Recompute all checksums regardless of file modification date. Without this flag, only files newer than the checksum file are updated. |

### update-image-list

Scans a book's directory for image files and generates the images CSV file.

```
java -jar rosa2.jar update-image-list --archive <dir> --collection <id> --book <id> [--force]
```

| Option | Required | Description |
|--------|----------|-------------|
| `--archive` | Yes | Path to the archive directory |
| `--collection` | Yes | Collection ID |
| `--book` | Yes | Book ID |
| `--force` | No | Overwrite existing image list if present |

### crop-images

Crops images based on crop data stored in the archive and generates the cropped image list.

```
java -jar rosa2.jar crop-images --archive <dir> --collection <id> [--book <id>] [--force]
```

| Option | Required | Description |
|--------|----------|-------------|
| `--archive` | Yes | Path to the archive directory |
| `--collection` | Yes | Collection ID |
| `--book` | No | Book ID. If omitted, crops images for all books in the collection. |
| `--force` | No | Overwrite existing cropped images if present |

### file-map

Interactively generates a file renaming map (filemap.csv) for a book.

```
java -jar rosa2.jar file-map --archive <dir> --collection <id> --book <id>
```

| Option | Required | Description |
|--------|----------|-------------|
| `--archive` | Yes | Path to the archive directory |
| `--collection` | Yes | Collection ID |
| `--book` | Yes | Book ID |

The command will prompt for:
- Front cover presence (true/false)
- Back cover presence (true/false)
- Number of frontmatter flyleaves
- Number of endmatter flyleaves
- Number of misc images
- Book ID

Writes a `filemap.csv` in the book directory mapping old filenames to new standardized names.

### rename-images

Renames image files in a book using the file map (filemap.csv).

```
java -jar rosa2.jar rename-images --archive <dir> --collection <id> --book <id> [--change-id] [--reverse]
```

| Option | Required | Description |
|--------|----------|-------------|
| `--archive` | Yes | Path to the archive directory |
| `--collection` | Yes | Collection ID |
| `--book` | Yes | Book ID |
| `--change-id` | No | Also change the ID prefix in filenames to match the book directory name |
| `--reverse` | No | Reverse the mapping direction (new→old), restoring original filenames |

### rename-files

Generic file renaming using a CSV mapping.

```
java -jar rosa2.jar rename-files <directory> <csv-file>
```

| Argument | Required | Description |
|----------|----------|-------------|
| `<directory>` | Yes | Path to the directory containing files to rename |
| `<csv-file>` | Yes | Path to a CSV file with old,new filename pairs |

### rename-transcriptions

Renames AoR transcription XML files using the file map (filemap.csv).

```
java -jar rosa2.jar rename-transcriptions --archive <dir> --collection <id> --book <id> [--reverse]
```

| Option | Required | Description |
|--------|----------|-------------|
| `--archive` | Yes | Path to the archive directory |
| `--collection` | Yes | Collection ID |
| `--book` | Yes | Book ID |
| `--reverse` | No | Reverse the mapping direction (new→old), restoring original filenames |

### generate-tei

Generates a single TEI P5 XML transcription file from per-page text transcription source files for a specific book.

```
java -jar rosa2.jar generate-tei --archive <dir> --collection <id> --book <id>
```

This finds all `{book-id}.transcription.{page}.txt` files in the specified book directory, parses their custom text format, and combines them into `{book-id}.transcription.xml`.

Books that already have a hand-authored `.transcription.xml` (like Douce195) or books that only have AoR annotation XML do not need this command.

| Option | Required | Description |
|--------|----------|-------------|
| `--archive` | Yes | Path to the archive directory |
| `--collection` | Yes | Collection ID |
| `--book` | Yes | Book ID |

### check-aor

Validates AoR transcription data against reference spreadsheets.

```
java -jar rosa2.jar check-aor --archive <dir> --collection <id> [--book <id>] [--spreadsheet-dir <dir>]
```

| Option | Required | Description |
|--------|----------|-------------|
| `--archive` | Yes | Path to the archive directory |
| `--collection` | Yes | Collection ID |
| `--book` | No | Book ID. If omitted, checks all books in the collection. |
| `--spreadsheet-dir` | No | Directory containing reference spreadsheets (Books.xlsx, People.xlsx, Locations.xlsx). Defaults to the collection directory. |

Checks performed:
- People, book, and location references match entries in reference spreadsheets
- Internal references between annotations target valid IDs
- Transcription filenames match associated image file names
- Duplicate annotation IDs

### generate-annotation-map

Generates a mapping of annotation IDs to their location in the corpus.

```
java -jar rosa2.jar generate-annotation-map --archive <dir> --collection <id>
```

| Option | Required | Description |
|--------|----------|-------------|
| `--archive` | Yes | Path to the archive directory |
| `--collection` | Yes | Collection ID |

Writes `id_locations.csv` to the collection directory, mapping each annotation ID to its collection, book, page, and annotation location. This mapping can be used to construct IIIF URIs.

### migrate-tei-metadata

Migrates book metadata from TEI description files into the custom XML metadata format.

```
java -jar rosa2.jar migrate-tei-metadata --archive <dir> --collection <id> [--book <id>]
```

| Option | Required | Description |
|--------|----------|-------------|
| `--archive` | Yes | Path to the archive directory |
| `--collection` | Yes | Collection ID |
| `--book` | No | Book ID. If omitted, processes all books in the collection. |

Reads language-specific TEI description files (`description_{lang}.xml`) and consolidates them into the custom metadata format. Skips books that already have a metadata file.

### decorate-image-list

Reads page labels from AoR transcription files and writes them into the image list CSV.

```
java -jar rosa2.jar decorate-image-list --archive <dir> --collection <id> [--book <id>]
```

| Option | Required | Description |
|--------|----------|-------------|
| `--archive` | Yes | Path to the archive directory |
| `--collection` | Yes | Collection ID |
| `--book` | No | Book ID. If omitted, processes all books in the collection. |

Extracts the pagination attribute (preferred) or signature attribute from each AoR transcription file and writes it as the page label in the book's image list CSV. Pages without a transcription file are marked as having auto-generated labels. This allows page labels to be managed centrally in the image list rather than requiring transcription files for labeling.
