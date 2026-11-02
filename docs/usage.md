# Tool Usage

## Purpose

This document is the reference for every command and option of the rosa-tool CLI. The tool
reads a rosa archive (see [Archive Structure](archive-structure.md)), generates IIIF
Presentation API 3.0 files (see [IIIF Generation](iiif-generation.md)) and Opensearch bulk
ingest files (see [Opensearch Indexes](opensearch-indexes.md)), checks archive data, and
maintains archive files.

## Audience

Developers, content administrators, and system administrators who work with the rosa archive.

## General usage

```
java -jar rosa-tool.jar <command> [options] [arguments]
```

Build the jar with `mvn clean package` in `rosa-tool/`; it is written to `rosa-tool/target/rosa-tool.jar`.

- `--help` (`-h`) on the tool or on any command prints its usage. `--version` (`-V`) prints
  `rosa-tool <version>`, the version from the jar manifest (`(development build)` when run
  from an IDE).
- Exit code is 0 on success and 1 on error. Unknown options or missing required options
  print `Error: ...` and the usage to stderr. Runtime failures print `Error: <message>` to
  stderr. The checking commands (`check`, `validate-xml`, `check-aor`) print their findings
  on stdout and exit 1 when they found errors; warnings alone exit 0.
- Commands that take `--archive` first verify that the directory exists. Collections are
  the archive's subdirectories and books the subdirectories of a collection. Directories
  whose name contains `.ignore` are skipped at both levels, and a top-level directory named
  `biblehistoriale` (a legacy collection) is never treated as a collection.
- Output directories (`--output`) are created when missing and are never cleaned: files
  from an earlier run that the current run does not write survive.

## Generating output

### generate-iiif-pres

Generates the static IIIF Presentation API 3.0 files. What is written, and the id and URL
conventions, are described in [IIIF Generation](iiif-generation.md).

```
java -jar rosa-tool.jar generate-iiif-pres --archive <dir> --output <dir> [--base-url <url>] [--image-base-url <url>] [--image-api-version <2|3>] [--opensearch-url <url>] [--annotation-stylesheet-url <url>]
```

| Option | Required | Description |
|--------|----------|-------------|
| `--archive` | Yes | Path to the archive directory |
| `--output` | Yes | Output directory |
| `--base-url` | No | Prefix of every resource `id`. If omitted, ids are relative paths. |
| `--image-base-url` | No | Base URL of the IIIF Image API services. Defaults to `--base-url`. |
| `--image-api-version` | No | `2` (default) or `3`. Any other value is an error. Selects `ImageService2` or `ImageService3`; see [IIIF Generation](iiif-generation.md). |
| `--opensearch-url` | No | Opensearch `_search` endpoint URL. When given, each collection gets a `service/jhsearch.json` and collections and manifests reference it; see [Search](search.md). |
| `--annotation-stylesheet-url` | No | URL of the stylesheet referenced by every commenting annotation. `{collection}` in it is replaced per collection; see [Annotation stylesheet](iiif-generation.md#annotation-stylesheet). If omitted, annotations have no `stylesheet`. |

Books without an image list, or with an empty one, are skipped with a warning on stderr
and a final `N book(s) skipped` line; the exit code stays 0.

### generate-opensearch-ingest

Generates Opensearch bulk ingest files. The documents and their fields are described in
[Opensearch Indexes](opensearch-indexes.md).

```
java -jar rosa-tool.jar generate-opensearch-ingest --archive <dir> --output <dir>
```

| Option | Required | Description |
|--------|----------|-------------|
| `--archive` | Yes | Path to the archive directory |
| `--output` | Yes | Output directory |

Writes one `<collection-id>.bulk.json` per collection that has books. A collection without
book directories, such as `dlmm`, produces no file.

### shallow-copy

Copies the archive without its images, so that a small copy (like the `archive/` directory
in this repository) regenerates exactly the same IIIF and Opensearch output as the full one.

```
java -jar rosa-tool.jar shallow-copy --archive <dir> --output <dir>
```

| Option | Required | Description |
|--------|----------|-------------|
| `--archive` | Yes | Path to the archive directory |
| `--output` | Yes | Destination directory |

Copies files whose extension is `xml`, `txt`, `html`, `csv`, `jsonld` or `properties`
(case-insensitive) and files whose name ends in `SHA1SUM`. Everything else, including every
image, is left out, as are `.ignore` directories and `biblehistoriale`. Files that already
exist in the destination are overwritten. Run `check --skip-images` against the copy, since
its image lists name files that are not there.

## Inspecting an archive

### list

```
java -jar rosa-tool.jar list --archive <dir> [--collection <id>]
```

| Option | Required | Description |
|--------|----------|-------------|
| `--archive` | Yes | Path to the archive directory |
| `--collection` | No | Lists the books of this collection. Without it, lists the collections. |

One name per line on stdout, sorted.

### check

Checks that the archive data is consistent.

```
java -jar rosa-tool.jar check --archive <dir> [--check-bits] [--skip-images]
```

| Option | Required | Description |
|--------|----------|-------------|
| `--archive` | Yes | Path to the archive directory |
| `--check-bits` | No | Also verify SHA1 checksums |
| `--skip-images` | No | Do not report listed images that are absent on disk (for shallow copies) |

For each collection it reports files that could not be read or parsed, and warns when the
collection checksum file `<collection>collection.SHA1SUM` is missing. For each book it
reports:

- files that could not be read or parsed (image lists, metadata, descriptions, crop file,
  AoR transcriptions);
- an image list that is missing (error);
- listed images that are not on disk, and images marked missing (`*`) that are on disk
  (errors, skipped with `--skip-images`);
- entries of `<book>.images.crop.csv` that are not in `<book>.images.csv` (warning: such
  pages are never shown, because canvases follow the image list);
- a missing `<book>.metadata.xml` (warning);
- with `--check-bits`: a mismatching SHA1 for every file that has an entry in
  `<book>.SHA1SUM`, files listed in the checksum file that are not on disk, and a missing
  checksum file. Files without an entry are not checked.

Findings are printed on stdout under a `[collection]` or `[collection/book]` heading as
`ERROR:` and `WARN:` lines. The exit code is 1 if any error was found.

### validate-xml

Checks that the AoR transcription files (`<book>.aor.<page>.xml`) of a collection are
well-formed XML, and valid against a schema when one is given.

```
java -jar rosa-tool.jar validate-xml --archive <dir> --collection <id> [--schema <xsd>]
```

| Option | Required | Description |
|--------|----------|-------------|
| `--archive` | Yes | Path to the archive directory |
| `--collection` | Yes | Collection to validate |
| `--schema` | No | A local XML Schema file to validate against |

Documents with a DOCTYPE are rejected, and nothing is ever fetched from the network: the
`xsi:noNamespaceSchemaLocation` the files carry (the AoR schema published at
livesandletters.ac.uk, which is not part of this repository) is ignored. To validate against
it, download the XSD and pass it with `--schema`. Findings are printed like `check`'s; the
exit code is 1 if any file is not well-formed or not valid.

### check-aor

Checks AoR transcriptions against the collection's reference sheets.

```
java -jar rosa-tool.jar check-aor --archive <dir> --collection <id> [--book <id>] [--spreadsheet-dir <dir>]
```

| Option | Required | Description |
|--------|----------|-------------|
| `--archive` | Yes | Path to the archive directory |
| `--collection` | Yes | Collection to check |
| `--book` | No | Check only this book. Internal references are then only resolved within it. |
| `--spreadsheet-dir` | No | Directory holding `people.csv`, `books.csv` and `locations.csv`. Defaults to the collection directory. |

Checks that every person, book and location referenced by an annotation is in the
reference sheet (each missing sheet is an error), that internal references target existing
annotation ids, that annotation ids are unique within the collection, that transcription
file names follow the `<book>.aor.<page>.xml` convention and agree with the page's
`filename` attribute. Errors are printed on stdout; the exit code is 1 if there are any.

## Maintaining an archive

These commands modify files in the archive. Formats are defined in
[Archive Structure](archive-structure.md).

### update

Recomputes SHA1 checksums.

```
java -jar rosa-tool.jar update --archive <dir> [--collection <id>] [--book <id>] [--force]
```

| Option | Required | Description |
|--------|----------|-------------|
| `--archive` | Yes | Path to the archive directory |
| `--collection` | No | Collection to update. Without it, every collection and book is updated. |
| `--book` | No | Book to update (requires `--collection`). Without it, the collection file and every book of the collection are updated. |
| `--force` | No | Recompute every checksum |

The collection file `<collection>collection.SHA1SUM` covers the regular files of the
collection directory (not dotfiles, not book directories). The book file `<book>.SHA1SUM`
covers every file in the book directory, images included. Without `--force`, a file keeps
its stored hash when it has not been modified since the checksum file was written; new and
modified files are hashed, and entries for files that no longer exist are dropped. Files are
written in `sha1sum` format (`<hash>  <name>`).

### update-image-list

Generates a book's image list `<book>.images.csv` from the image files in its directory.

```
java -jar rosa-tool.jar update-image-list --archive <dir> --collection <id> --book <id> [--force]
```

| Option | Required | Description |
|--------|----------|-------------|
| `--archive` | Yes | Path to the archive directory |
| `--collection` | Yes | Collection ID |
| `--book` | Yes | Book ID |
| `--force` | No | Rewrite an existing list |

Image files are those with extension `tif`, `tiff`, `jpg`, `jpeg` or `png` in any case;
dimensions are read from the file headers. Without `--force` an existing list is left
alone. With it, the list is rewritten keeping each image's label and `##` marker, and listed
images that are no longer on disk stay listed, marked missing (`*`). Rows are sorted by file
name.

### crop-images

Crops a book's images and writes the cropped image list.

```
java -jar rosa-tool.jar crop-images --archive <dir> --collection <id> [--book <id>] [--force]
```

| Option | Required | Description |
|--------|----------|-------------|
| `--archive` | Yes | Path to the archive directory |
| `--collection` | Yes | Collection ID |
| `--book` | No | Book ID. Without it, every book of the collection. |
| `--force` | No | Overwrite existing cropped images and an existing cropped image list |

Reads `<book>.crop.txt`, writes each cropped image under `<book>/cropped/` with the same
file name, then writes `<book>.images.crop.csv` listing the files in `cropped/` with their
dimensions, in the order and with the labels of `<book>.images.csv` (cropped files that the
image list does not name are appended). A book without a crop file is an error; so are
malformed crop lines and crop lines naming images that are not on disk.

### decorate-image-list

Copies page labels from AoR transcriptions into the image list.

```
java -jar rosa-tool.jar decorate-image-list --archive <dir> --collection <id> [--book <id>]
```

| Option | Required | Description |
|--------|----------|-------------|
| `--archive` | Yes | Path to the archive directory |
| `--collection` | Yes | Collection ID |
| `--book` | No | Book ID. Without it, every book of the collection. |

For every transcription, the `pagination` attribute of its `<page>` (or `signature` when
there is no pagination) becomes the label of the image named by `filename`. Images that get
no label this way and have no hand-written label keep their derived name, marked
auto-generated (`##`). This lets page labels live in the image list, so a page needs no
transcription to be labelled.

### generate-tei

Builds a book's TEI transcription from per-page text files.

```
java -jar rosa-tool.jar generate-tei --archive <dir> --collection <id> --book <id> [--force]
```

| Option | Required | Description |
|--------|----------|-------------|
| `--archive` | Yes | Path to the archive directory |
| `--collection` | Yes | Collection ID |
| `--book` | Yes | Book ID |
| `--force` | No | Replace an existing `<book>.transcription.xml` |

Combines every `<book>.transcription.<page>.txt` into `<book>.transcription.xml`. Without
`--force` an existing transcription is never replaced (error, exit 1); a book without text
files is an error too. Conversion warnings are printed on stdout. In the current archive
only `CoxMacro`, `Douce332`, `LudwigXV7` and `SeldenSupra57` have text sources, and all four
already have the XML.

## Ingest helpers

The renaming commands are used when new books are brought into the archive with their
original file names. They work on `.tif` files only.

### file-map

Interactively generates `filemap.csv`, the rename map for a book.

```
java -jar rosa-tool.jar file-map --archive <dir> --collection <id> --book <id> [--force]
```

| Option | Required | Description |
|--------|----------|-------------|
| `--archive` | Yes | Path to the archive directory |
| `--collection` | Yes | Collection ID |
| `--book` | Yes | Book ID |
| `--force` | No | Replace an existing `filemap.csv` |

The command prompts for: a front cover and front pastedown (`true`/`false`), a back cover
and back pastedown (`true`/`false`), the number of frontmatter flyleaf images, of endmatter
flyleaf images and of misc images, and the book ID to use in the new names. Invalid answers
are asked again. The `.tif` files, sorted by name, are then allocated in this order: front
cover, front pastedown, frontmatter flyleaves (`frontmatter.flyleaf.001r`, `001v`, ...),
body pages (`001r`, `001v`, ...), endmatter flyleaves, back pastedown, back cover, misc. The
map is written to `filemap.csv` in the book directory as `old,new` rows sorted by old name.

### rename-images

Renames a book's image files.

```
java -jar rosa-tool.jar rename-images --archive <dir> --collection <id> --book <id> [--reverse | --change-id]
```

| Option | Required | Description |
|--------|----------|-------------|
| `--archive` | Yes | Path to the archive directory |
| `--collection` | Yes | Collection ID |
| `--book` | Yes | Book ID |
| `--reverse` | No | Apply `filemap.csv` in reverse (new to old) |
| `--change-id` | No | Instead of applying `filemap.csv`, replace the part of every `.tif` name before the first `.` with the book directory name |

By default the book's `filemap.csv` is applied. Blank lines and lines starting with `#` are
skipped and inline `#` comments are stripped. Nothing is renamed when the map has duplicate
targets, and a single rename is refused when the source is missing or the target exists.
`--reverse` and `--change-id` exclude each other.

### rename-transcriptions

Renames a book's AoR transcription files following `filemap.csv`.

```
java -jar rosa-tool.jar rename-transcriptions --archive <dir> --collection <id> --book <id> [--reverse]
```

| Option | Required | Description |
|--------|----------|-------------|
| `--archive` | Yes | Path to the archive directory |
| `--collection` | Yes | Collection ID |
| `--book` | Yes | Book ID |
| `--reverse` | No | Apply the map in reverse (new to old) |

Each image name in the map is turned into its transcription name (`X.001r.tif` to
`X.aor.001r.xml`); the file is renamed and the `filename` attribute of its `<page>` element
is updated to the new image name.

### rename-files

Renames files in any directory according to a CSV map. It does not read the archive.

```
java -jar rosa-tool.jar rename-files <directory> <csv-file>
```

| Argument | Required | Description |
|----------|----------|-------------|
| `<directory>` | Yes | Directory containing the files |
| `<csv-file>` | Yes | File with `old,new` rows |

Every row must have two non-empty fields (no comments or blank lines); a row whose source
does not exist is an error. Renames are applied in order and are not undone on failure.
