# Rosa command line tool

A command-line tool for managing digital manuscript archives. rosa2 reads a local archive directory hierarchy and generates static files for IIIF Presentation API 3.0, Opensearch bulk ingest, and Archaeology of Reading (AoR) annotation statistics.

## What It Does

- **IIIF Presentation 3.0** — Generates static JSON files (collections, manifests, canvases, annotation pages) that any web server can serve directly to IIIF viewers.
- **Opensearch Bulk Ingest** — Produces NDJSON files formatted for the Opensearch Bulk API, enabling full-text and faceted search over manuscript metadata, page content, and annotations with multi-language analysis.
- **AoR Statistics** — Computes annotation counts, word frequencies, and vocabulary reports across Archaeology of Reading transcription data.
- **Shallow Copy** — Copies only metadata and transcription files (XML, TXT, HTML, CSV) from the archive, excluding large image files.
- **Archive Validation** — Lists collections and books, checks structural consistency, verifies checksums, and validates AoR XML against the schema.

## Prerequisites

- Java 25
- Maven 3.9+

## Quick Start

Build the executable JAR:

```sh
mvn clean package
```

Run the tool:

```sh
java -jar target/rosa-tool.jar --help
```

## Commands

| Command | Description |
|---------|-------------|
| `generate-iiif-pres` | Generate static IIIF Presentation API 3.0 files |
| `generate-opensearch-ingest` | Generate Opensearch bulk ingest NDJSON files |
| `shallow-copy` | Copy metadata files from the archive (no images) |
| `check` | Verify archive structural consistency and checksums |
| `list` | List collections or books in the archive |
| `validate-xml` | Validate AoR XML files against the schema |
| `aor-stats` | Generate AoR annotation statistics CSVs |

## Documentation

- [Usage](../doc/usage.md) — CLI commands, options, and examples
- [Archive Structure](doc/archive-structure.md) — Directory hierarchy and file conventions
- [IIIF Generation](../doc/iiif-generation.md) — How IIIF 3.0 output is structured
- [Search](../doc/search.md) — JHSearch service design, Opensearch indexes, and query examples
- [AoR Data Model](../doc/aor-data-model.md) — Annotation types and data structures
- [Deployment](../doc/deployment.md) — Building, releasing, and deploying the tool
