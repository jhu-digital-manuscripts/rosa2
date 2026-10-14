# Rosa

Rosa is a framework for creating a web application that allows users to interact with digitized books and complex metadata. Raw data is stored in a file system archive, managed by a command-line tool that generates static IIIF Presentation API 3.0 files and Opensearch bulk ingest data. The website uses the Mirador IIIF viewer with custom plugins for search and faceted browsing.

## Components

| Component | Description |
|-----------|-------------|
| [rosa-tool](rosa-tool/) | Java CLI for archive management, IIIF generation, and Opensearch data export |
| [rosa-viewer](rosa-viewer/) | Mirador 4-based IIIF viewer with JHSearch plugin |
| [rosa-search](rosa-search/) | Opensearch index definitions, Latin analyzer resources, and search Lambdas |
| [rosa-image-server](rosa-image-server/) | Cantaloupe + Kakadu IIIF Image API server (Docker image for ECS) |
| [jhu-rosa2-tf](jhu-rosa2-tf/) | OpenTofu configuration for AWS: Opensearch behind API Gateway, and the image server behind CloudFront |
| [local-dev](local-dev/) | Local development environment scripts |

## Quick Start

### Prerequisites

- Java 25+ (JDK)
- Node.js 20+
- Docker (for local Opensearch 3)
- Maven 3.9+

### Building

Build rosa-tool:

```bash
cd rosa-tool
mvn package
```

Build rosa-viewer:

```bash
cd rosa-viewer
npm install
npm run build
```

### Local Development

1. Configure the local environment:

```bash
cd local-dev
cp config.env.example config.env
# Edit config.env and set ROSA_ARCHIVE to your archive path
```

2. Start the environment:

```bash
./start.sh
```

3. Run rosa-viewer in development mode:

```bash
cd ../rosa-viewer
npm run dev
```

4. Open http://localhost:3001 in your browser.

See [docs/deployment.md](docs/deployment.md) for detailed instructions.

## Documentation

- [Tool Usage](docs/usage.md) — CLI commands and options
- [Archive Structure](docs/archive-structure.md) — Directory hierarchy and file conventions
- [IIIF Generation](docs/iiif-generation.md) — How IIIF 3.0 output is structured
- [Opensearch Indexes](docs/opensearch-indexes.md) — Index definitions and field mappings
- [Search](docs/search.md) — JHSearch service design and query examples
- [AoR Data Model](docs/aor-data-model.md) — Annotation types and data structures
- [Deployment](docs/deployment.md) — Building, releasing, and deploying

## Architecture

```
┌─────────────────┐     ┌──────────────────┐     ┌─────────────────┐
│  Archive (fs)   │────▶│    rosa-tool     │────▶│  IIIF JSON      │
│  metadata, XML  │     │  (Java CLI)      │     │  (static files) │
└─────────────────┘     └──────────────────┘     └────────┬────────┘
                               │                          │ manifests
                               ▼                          ▼
                        ┌──────────────────┐     ┌─────────────────┐
                        │ Opensearch Bulk  │     │  rosa-viewer    │
                        │ (NDJSON files)   │     │  (Mirador + UI) │
                        └────────┬─────────┘     └──┬───────────┬──┘
                                 │        search    │           │ tiles, thumbnails,
                                 ▼                  │           │ info.json (IIIF Image API)
                        ┌──────────────────┐        │           ▼
                        │   Opensearch     │◀───────┘  ┌─────────────────────┐
                        │   (search API)   │           │  rosa-image-server  │
                        └──────────────────┘           │  CloudFront → ECS   │
                                                       │  Cantaloupe+Kakadu  │
                                                       └──────────┬──────────┘
                                                                  │
                                                                  ▼
                                                       ┌─────────────────────┐
                                                       │ S3: JPEG 2000 pages │
                                                       │ + derivative cache  │
                                                       └─────────────────────┘
```

Manifests reference images by IIIF Image API service URL
(`--image-base-url`); the viewer fetches `info.json`, tiles and thumbnails
directly from the image server. See [rosa-image-server](rosa-image-server/README.md)
and [jhu-rosa2-tf](jhu-rosa2-tf/README.md#image-server).

## License

See the LICENSE file in the repository root.
