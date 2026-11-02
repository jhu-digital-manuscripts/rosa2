# rosa-tool

The command-line tool of rosa2. It reads a rosa archive (a file-system hierarchy of
collections and books with metadata, image lists and transcriptions) and generates the
static IIIF Presentation API 3.0 files and the Opensearch bulk ingest files the viewer and
search use. It also checks and maintains the archive.

## Build and run

Requires Java 25 and Maven 3.9+.

```sh
mvn clean package                       # build and test -> target/rosa-tool.jar
java -jar target/rosa-tool.jar --help
java -jar target/rosa-tool.jar <command> --help
```

## Commands

| Command | Description |
|---------|-------------|
| `generate-iiif-pres` | Generate the static IIIF Presentation API 3.0 files |
| `generate-opensearch-ingest` | Generate the Opensearch bulk ingest files |
| `shallow-copy` | Copy the archive without its images |
| `list` | List collections or books |
| `check` | Check archive consistency, optionally checksums |
| `validate-xml` | Check AoR transcription XML for well-formedness, optionally against a local schema |
| `check-aor` | Check AoR transcriptions against the reference sheets |
| `update` | Recompute SHA1 checksum files |
| `update-image-list` | Generate a book's image list from its image files |
| `crop-images` | Crop a book's images and write the cropped image list |
| `decorate-image-list` | Copy page labels from AoR transcriptions into the image list |
| `generate-tei` | Build a TEI transcription from per-page text files |
| `file-map` | Interactively write a rename map for a newly ingested book |
| `rename-images` | Rename image files using the rename map |
| `rename-transcriptions` | Rename AoR transcription files using the rename map |
| `rename-files` | Rename files in any directory from a CSV map |

## Documentation

- [Usage](../docs/usage.md): every command and option
- [Archive Structure](../docs/archive-structure.md): the files the tool reads and their formats
- [IIIF Generation](../docs/iiif-generation.md): what `generate-iiif-pres` writes
- [Opensearch Indexes](../docs/opensearch-indexes.md): the documents `generate-opensearch-ingest` writes
- [Search](../docs/search.md): how clients query the search service
- [AoR Data Model](../docs/aor-data-model.md): the AoR XML and its Java model
- [Deployment](../docs/deployment.md): building, releasing and deploying
