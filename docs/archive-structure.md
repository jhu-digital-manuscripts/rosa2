# Archive Directory Structure

## Purpose

This document is the reference for the file-system archive that rosa-tool reads: the
directory hierarchy, every file the tool uses and its format, and the naming conventions
the formats rely on. Files that occur in the archive but are not read are listed at the
end.

## Audience

Content administrators who manage archive data and developers working on the rosa2 codebase.

## Hierarchy

An archive is a directory of collections; a collection is a directory of books; a book is
a directory of images and the files describing them. The in-repository `archive/` is a
shallow copy (no images) of the real archive, made with `shallow-copy`.

```
<archive>/
  <collection-id>/                        aor, dlmm, pizan, rose
    config.properties                     collection metadata (required)
    <collection-id>collection.SHA1SUM     checksums of the collection-level files
    character_names.csv                   lookup tables for illustration tagging
    illustration_titles.csv
    narrative_sections.csv
    people.csv  books.csv  locations.csv  AoR reference sheets (aor only)
    annos.jsonld                          HTML commentary per image (rose only)
    missing_image.tif                     placeholder for missing pages (rose only)
    <book-id>/
      <book-id>.metadata.xml              book metadata (title, dates, texts, bibliography)
      <book-id>.images.csv                image list: page order, dimensions, labels
      <book-id>.images.crop.csv           image list of cropped/ when the book is cropped
      <book-id>.crop.txt                  crop margins per image
      cropped/<image>                     cropped images (not in the shallow copy)
      <book-id>.description_<lang>.xml    TEI description, one per collection language
      <book-id>.permission_<lang>.html    attribution statement, one per language
      <book-id>.imagetag.csv              illustration tagging
      <book-id>.transcription.xml         TEI transcription
      <book-id>.transcription.<page>.txt  per-page transcription sources
      <book-id>.aor.<page>.xml            AoR annotation transcriptions, one per page
      filemap.csv                         rename map used when the book was ingested
      <book-id>.SHA1SUM                   checksums of every file in the book
      <book-id>.<page>.tif                page images (not in the shallow copy)
```

Collection and book identifiers are their directory names (`rose`, `Douce195`,
`FolgersHa2`). A directory whose name contains `.ignore` is not a collection or book, and a
top-level `biblehistoriale` directory is ignored too. A file whose name starts with `*` in
an image list, and a label starting with `##`, have the meanings given under
[Image lists](#image-lists).

## Collections

### `config.properties`

A Java properties file. Every collection has one.

| Key | Description |
|-----|-------------|
| `label` | Display name, e.g. `Roman de la Rose Digital Library` |
| `description` | Prose description |
| `languages` | Comma-separated language codes (`en` or `en,fr`). The first is the collection's primary language; descriptions and permissions are read for each. |
| `logo` | Logo URL (empty in the current archive) |
| `parents` | Comma-separated parent collection ids, or empty |
| `children` | Comma-separated child collection ids, or empty |
| `missing_image.width`, `missing_image.height` | Dimensions of `missing_image.tif`, the placeholder painted for pages marked missing |

`dlmm` is a parent collection with `children=rose,pizan` and no books; `rose` and `pizan`
declare `parents=dlmm`. The IIIF output lists `rose` and `pizan` inside `dlmm`'s collection
and the Opensearch documents of their books carry both collection ids
(see [IIIF Generation](iiif-generation.md) and [Opensearch Indexes](opensearch-indexes.md)).

### Collection files

| File | Format |
|------|--------|
| `<collection-id>collection.SHA1SUM` | `sha1sum` format, `<hash>  <file>`, one line per regular file of the collection directory. Written by `update`. |
| `character_names.csv` | Header `ID,Site name,French variant,English name` (any number of name columns; each header is the name's key), then one row per character: id and its name variants. Used to resolve numeric character references in `imagetag.csv`. |
| `illustration_titles.csv` | Header `Id,Title`, then one row per illustration title. Used to resolve numeric title references in `imagetag.csv`. |
| `narrative_sections.csv` | Header `Section,Lines,Lecoy,Description`, then one row per section of the Roman de la Rose: id, relative line range (`1-20`), Lecoy edition line range, description. |
| `people.csv`, `books.csv`, `locations.csv` | AoR reference sheets, checked by `check-aor` and used by the Opensearch generator to add alternate names. `people.csv`: header `sci,Alternative name (1),...,ISNI,Extra info`, then the standard name followed by its alternates and ISNI. `locations.csv`: no header, standard name then alternates. `books.csv`: header `Standard ID,...` (skipped), then the standard title followed by bibliographic columns. |
| `annos.jsonld` | A JSON array of Web Annotations with `target` `rosa:<image-id>` (e.g. `rosa:Douce195.001r.tif`) and `body.value` holding HTML. Each becomes a commentary annotation on that page. |
| `missing_image.tif` | The image painted for pages marked missing; its size comes from `config.properties`. |

`character_names.csv`, `illustration_titles.csv` and `narrative_sections.csv` exist in every
collection but are empty outside `rose`.

## Books

Every book has `<book-id>.metadata.xml`, `<book-id>.images.csv` and `<book-id>.SHA1SUM`. The
other files are present when the book has that kind of data. All of a book's files, and its
AoR transcriptions, are prefixed with the book id.

| File | Description |
|------|-------------|
| `<book-id>.metadata.xml` | Book metadata, see [Metadata](#metadata) |
| `<book-id>.images.csv` | The image list, see [Image lists](#image-lists). Defines the page order. |
| `<book-id>.images.crop.csv` | Image list of the cropped images in `cropped/`, same format. A canvas paints the cropped image when its id is listed here. |
| `<book-id>.crop.txt` | Crop margins, see [Cropping](#cropping) |
| `<book-id>.description_<lang>.xml` | TEI P5 manuscript description. Only the `teiHeader/fileDesc/notesStmt/note` elements are read: each `note[@rend]` (e.g. `IDENTIFICATION`, `MATERIAL`, `SCRIPT`, `DECORATION`, `BINDING`, `ORIGIN`, `FOLIATION`, `DATE`, `SELECT BIBLIOGRAPHY`) becomes a titled section, notes without `rend` are collected under `OTHER`. The English one is published with the manifest (see [Book descriptions](iiif-generation.md#book-descriptions)). Only DLMM books have descriptions; 14 rose books have none. |
| `<book-id>.permission_<lang>.html` | An HTML fragment with the attribution statement, read for each collection language; lines are concatenated. Becomes the manifest's `requiredStatement`. |
| `<book-id>.imagetag.csv` | Illustration tagging, see [Illustration tagging](#illustration-tagging) |
| `<book-id>.transcription.xml` | TEI P5 transcription with `<pb n>` page breaks and `<cb n>` column breaks. Hand-authored (Douce195, Morgan948, Walters143) or generated from the per-page text files by `generate-tei`. |
| `<book-id>.transcription.<page>.txt` | Per-page transcription sources in a custom text format: a `[<book-id>]` line, a `[<folio> <column>]` line (e.g. `[001r a]`), then lines of verse with inline tags such as `<rubric>`, `<illustration>`, `<catchphrase>`, `<initial/>`. Converted by `generate-tei`. |
| `<book-id>.aor.<page>.xml` | One AoR annotation transcription per annotated page, see [AoR Data Model](aor-data-model.md). Any file whose name contains `.aor.` and ends in `.xml` is one. |
| `filemap.csv` | `old,new` file-name pairs (`#` comments allowed) written by `file-map` and applied by `rename-images` and `rename-transcriptions`. 41 books keep the map from their ingest. |
| `<book-id>.SHA1SUM` | `sha1sum` format, one line per file in the book directory, images included. Written by `update`. |
| Images | `.tif` files named as described under [Image names](#image-names). Only the shallow copy lacks them. |

### Metadata

`<book-id>.metadata.xml` is a custom XML format with a `<book>` root:

```xml
<book>
  <license><url>http://creativecommons.org/licenses/by/4.0/</url><logo>...</logo></license>
  <illustrations>125</illustrations>
  <totalPages>156</totalPages>
  <dimensions units="mm"><width>235</width><height>345</height></dimensions>
  <dates><startDate>1450</startDate><endDate>1500</endDate></dates>
  <texts>
    <text id="rose">
      <language>fr</language>
      <title>Roman de la Rose</title>
      <textId/>
      <pages end="156v" start="1r">156</pages>
      <illustrations>125</illustrations>
      <linesPerColumn>38</linesPerColumn>
      <leavesPerGathering>8</leavesPerGathering>
      <columnsPerPage>2</columnsPerPage>
    </text>
  </texts>
  <bibliographies>
    <bibliography lang="en">
      <title>Roman de la Rose</title>
      <commonName>Douce 195</commonName>
      <dateLabel>15th century</dateLabel>
      <type>manuscript</type>
      <material>Parchment</material>
      <origin>France</origin>
      <currentLocation>Oxford</currentLocation>
      <repository>Bodleian Library</repository>
      <shelfmark>MS Douce 195</shelfmark>
      <author><name>Domenichi, Lodovico</name><id>http://www.isni.org/...</id></author>
      <reader>...</reader>
      <website>...</website>
      <detail>...</detail>
      <note>...</note>
    </bibliography>
  </bibliographies>
</book>
```

`license/url` becomes the manifest's `rights` (a Creative Commons or RightsStatements.org
URI is expected); `totalPages`, `illustrations`, `dimensions` and `dates` become metadata;
each `text` gives a title (and a Range from `pages/@start` to `@end`); there is one
`bibliography` per language, whose `commonName` is the book's label and whose `author`,
`reader`, `website`, `detail` and `note` elements may repeat. Elements may be empty.

### Image lists

`<book-id>.images.csv` and `<book-id>.images.crop.csv` have no header and one row per
image, in page order:

```
[*]<image-file>[,<width>,<height>[,<label>]]
```

- A leading `*` marks an image that is missing from the archive; the placeholder is
  painted in its place. Rows of missing images may consist of the file name only.
- `width` and `height` are pixel dimensions; both are given or neither.
- The optional `label` replaces the page label derived from the file name. A label
  starting with `##` was generated automatically (by `decorate-image-list`) rather than
  written by hand; the viewer shows it without the prefix.

```
*Douce195.binding.frontcover.tif
Douce195.frontmatter.pastedown.tif,3518,5367
Douce195.001r.tif,3518,5367,[title page]
Douce195.001v.tif,3518,5367,##1v
```

`update-image-list`, `crop-images` and `decorate-image-list` write this format and keep
existing labels and missing markers.

### Image names

Image files are named `<book-id>.<tokens>.tif`. The tokens between the book id and the
extension give the image's location, page and role:

| Pattern | Location | Example |
|---------|----------|---------|
| `<book>.<page>.tif` | body matter | `Douce195.001r.tif`, `FolgersHa2.A1r.tif` |
| `<book>.frontmatter.<role or page>.tif` | front matter | `X.frontmatter.pastedown.tif`, `X.frontmatter.flyleaf.01r.tif` |
| `<book>.endmatter.<role or page>.tif` | end matter | `X.endmatter.flyleaf.02v.tif`, `X.endmatter.pastedown.tif` |
| `<book>.binding.<role>.tif` | binding | `X.binding.frontcover.tif`, `X.binding.spine.tif` |
| `<book>.misc.<role>[.<n>].tif` | misc | `X.misc.colorbar.tif`, `X.misc.insert.01.tif` |

A page token is `<letters><digits>r|v` or digits only (`001r`, `A1r`, `12`). Roles are
`frontcover`, `backcover`, `pastedown`, `insert`, `spine`, `gutter`, `tail`, `head`,
`colorbar` and `misc`; `insert` may be followed by a number (`X.012r.insert.tif`,
`X.misc.insert.05.tif`). The derived page label is `<location> <page> <role> <n>` with
leading zeros dropped (`front matter 1r`, `binding front cover`, `1r`). Locations group the
canvases into IIIF Ranges. The archive also contains `binding.foredge` and `misc.color`
images, which match no role and get no role in their label.

### Cropping

`<book-id>.crop.txt` has one whitespace-separated row per image:

```
<image-file> <left> <right> <top> <bottom>
```

Each value is the fraction (0 to 1) of the image width or height to remove from that side,
e.g. `Douce195.001r.tif 0.039261 0.115473 0.054167 0.063333`. `crop-images` writes the
results to `cropped/` and lists them in `<book-id>.images.crop.csv`. 29 rose books have a
crop file and 25 a cropped image list.

### Illustration tagging

`<book-id>.imagetag.csv` describes the illustrations of a book, one per row, with the
columns `id`, `Folio` (or `Folio #`), `Illustration title`, `Textual elements`, `Initials`,
`Characters`, `Costume`, `Objects`, `Landscape`, `Architecture` and `Other`. Columns are
matched by header name, so order may vary and `Initials` may be absent.

The `Illustration title` and `Characters` columns hold entries separated by commas or
semicolons. A purely numeric entry is a reference into the collection's
`illustration_titles.csv` or `character_names.csv`; anything else is a literal value. Both
forms may be mixed in one cell, as in `57, Jalous' wife`. Because commas and semicolons
separate entries, a literal value cannot contain either. Generation fails if a numeric
reference has no matching row in the lookup table.

## Present but not read

These files occur in the archive and are kept, but no command reads them:

| File | Content |
|------|---------|
| `<book-id>.bnf.filemap.csv`, `<book-id>.bnf.foliation.xml`, `<book-id>.bnf.MD5SUM` | Delivery manifests of the Bibliothèque nationale de France digitizations (pizan and 117 rose books) |
| `<book-id>.bibliography.xml` | Bibliography of 14 rose books |
| `<book-id>.nartag.csv`, `<book-id>.nartag.txt`, `<book-id>.redtag.txt` | Narrative section and rubric tagging of some rose books |
| `<book-id>.filemap.csv` | Older, prefixed rename maps (the tool reads only `filemap.csv`) |
| `aor/id_locations.csv` | Map of AoR annotation ids to collection, book and page |
