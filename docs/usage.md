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
  stderr. The checking commands (`check`, `check-aor`) print their findings
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
java -jar rosa-tool.jar generate-iiif-pres --archive <dir> --output <dir> [--base-url <url>] [--image-base-url <url>] [--image-api-version <2|3>] [--opensearch-url <url>] [--annotation-stylesheet-url <url>] [--viewer-url <url>] [--collection <id>]...
```

| Option | Required | Description |
|--------|----------|-------------|
| `--archive` | Yes | Path to the archive directory |
| `--output` | Yes | Output directory |
| `--base-url` | No | Prefix of every resource `id`. If omitted, ids are relative paths. |
| `--image-base-url` | No | Base URL of the IIIF Image API services. Defaults to `--base-url`. |
| `--image-api-version` | No | `2` (default) or `3`. Any other value is an error. Selects `ImageService2` or `ImageService3`; see [IIIF Generation](iiif-generation.md). |
| `--opensearch-url` | No | Opensearch `_search` endpoint URL. When given, each collection gets a `service/jhsearch.json` and collections and manifests reference it; see [Search](search.md). |
| `--annotation-stylesheet-url` | No | URL of the stylesheet referenced by every commenting annotation. `{collection}` in it is replaced per collection; see [Annotation stylesheet](iiif-generation.md#annotation-stylesheet). If omitted, annotations have no `stylesheet`. The template may contain no braces other than `{collection}`; a mangled one such as `{collection/}` is an error. |
| `--viewer-url` | No | URL of the viewer site that the people, books and locations named in AoR annotations link to (their advanced search). `{collection}` in it is replaced per collection like the stylesheet URL; see [Viewer URL](iiif-generation.md#viewer-url). If omitted, the names are plain text. |
| `--collection` | No | Generate only this collection; repeat the option for several. The top-level `collection.json` lists only the generated collections, so include a parent (`dlmm`) with the children (`rose`, `pizan`) it should list. An unknown id is an error. Default: every collection. |

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

### check-aor

Checks the AoR transcription files (`<book>.aor.<page>.xml`) of a collection. Every file
must be well-formed XML, and valid against a schema when one is given. Well-formed files
are then checked against the collection's reference sheets: people, books, locations and
the person and title of cross-references must be in `people.csv`, `books.csv` and
`locations.csv` (each missing sheet is an error); annotation ids must be unique within the
collection; every internal reference target must resolve to a page through
`id_locations.csv` (reported as `Internal reference target not resolved: '<ref>' (<reason>)`,
plus a message when an annotation id sits on a different page than the sheet says); and
file names must follow the `<book>.aor.<page>.xml` convention, agree with the page's
`filename` attribute and name an image of `<book>.images.csv` (the generators attach
annotations to canvases by that name, so a transcription without an image is otherwise
silently dropped). The sheets must not list a name twice (`Duplicate key '<name>' in
people.csv`). Without an `id_locations.csv`, targets are only checked against the
annotation ids found.

Names are compared exactly, after trimming surrounding whitespace, to the first column of
the sheet. When an unknown name is one of an entry's alternates the message says so
(`Unknown book reference: Bucolics (alternate of 'Eclogues'; use the standard name)`), since
only the standard name gets the sheet's alternates indexed for search.

```
java -jar rosa-tool.jar check-aor --archive <dir> --collection <id> [--book <id>] [--spreadsheet-dir <dir>] [--schema <xsd>]
```

| Option | Required | Description |
|--------|----------|-------------|
| `--archive` | Yes | Path to the archive directory |
| `--collection` | Yes | Collection to check |
| `--book` | No | Check only this book. Internal references are then only resolved within it. |
| `--spreadsheet-dir` | No | Directory holding `people.csv`, `books.csv`, `locations.csv` and `id_locations.csv`. Defaults to the collection directory. |
| `--schema` | No | A local XML Schema file to validate the transcriptions against |

Documents with a DOCTYPE are rejected, and nothing is ever fetched from the network: the
`xsi:noNamespaceSchemaLocation` the files carry (the AoR schema published at
livesandletters.ac.uk, which is not part of this repository) is ignored. To validate against
it, download the XSD and pass it with `--schema`. A file that is not well-formed is reported
once, with the line and column of the problem, and not checked further.

Findings are printed on stdout as `ERROR:` and `WARN:` lines, each starting with the
`[book/file]` it concerns. Only the XML parser and schema validator produce warnings. The
exit code is 1 if there is any error.

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

### generate-tei

Builds a book's TEI transcription from per-page text files.

```
java -jar rosa-tool.jar generate-tei --archive <dir> --collection <id> --book <id> [--force] [--encoding <charset>]
```

| Option | Required | Description |
|--------|----------|-------------|
| `--archive` | Yes | Path to the archive directory |
| `--collection` | Yes | Collection ID |
| `--book` | Yes | Book ID |
| `--force` | No | Replace an existing `<book>.transcription.xml` |
| `--encoding` | No | Character encoding of the per-page text files. Default `windows-1252`, which is what the archive's text files are. |

Combines every `<book>.transcription.<page>.txt` into `<book>.transcription.xml`. Without
`--force` an existing transcription is never replaced (error, exit 1); a book without text
files is an error too. Conversion warnings are printed on stdout. A line in a text file that
starts with `#` is a comment and is ignored. In the current archive
only `CoxMacro`, `Douce332`, `LudwigXV7` and `SeldenSupra57` have text sources, and all four
already have the XML. The text files are read as Windows-1252 unless `--encoding` says
otherwise (they contain `œ`, `Ÿ` and `…`, which ISO-8859-1 would turn into control
characters); the XML is always written as UTF-8.
