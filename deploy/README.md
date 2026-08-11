# Rosa Deployment

This directory contains deployment scripts, configuration files, and a local development environment for Rosa.

## Directory Structure

```
deploy/
├── index-archive.sh      # Generate IIIF files from archive
├── opensearch/           # Opensearch index definitions
│   ├── manifest.json     # Manifest index mapping
│   └── canvas.json       # Canvas index mapping
└── local-dev/            # Local development environment
    ├── config.env        # Environment configuration
    ├── start.sh          # Start all services
    ├── stop.sh           # Stop all services
    ├── status.sh         # Check service status
    ├── docker-compose.yml
    └── opensearch.yml
```

## Scripts

### index-archive.sh

Generates IIIF Presentation API 3.0 files from a rosa archive using rosa-tool.

```bash
./index-archive.sh --archive /path/to/archive --output /path/to/output [options]
```

Options:
- `--archive <path>` — Path to the rosa archive directory (required)
- `--output <path>` — Output directory for IIIF files (required)
- `--base-url <url>` — Base URL for IIIF resource IDs
- `--image-api-version <2|3>` — IIIF Image API version (default: 2)
- `--image-base-url <url>` — Base URL for image server
- `--opensearch-url <url>` — Opensearch URL to embed in manifests
- `--jar <path>` — Path to rosa-tool.jar (default: rosa-tool/target/rosa-tool.jar)

Prerequisites:
- Java 25+
- rosa-tool.jar built (`mvn package` in rosa-tool/)

## Local Development Environment

The `local-dev/` directory provides a complete local environment for developing and testing rosa-viewer against real archive data.

### What It Does

1. Generates IIIF Presentation API files from your archive
2. Generates Opensearch bulk ingest files
3. Runs Opensearch in Docker
4. Creates indexes and ingests data
5. Serves IIIF files via a static file server

### Prerequisites

- Java 25+
- Docker and Docker Compose
- Node.js 20+ (for rosa-viewer)
- rosa-tool.jar built
- A rosa archive on the local filesystem

### Setup

1. Create a configuration file:

```bash
cd deploy/local-dev
cp config.env config.env.local
```

2. Edit `config.env` and set required values:

```bash
# Path to your rosa archive (required)
ROSA_ARCHIVE=/path/to/your/rosa-archive

# IIIF Image API configuration
IMAGE_API_VERSION=2
IMAGE_BASE_URL=https://image.library.jhu.edu/iiif
```

### Starting the Environment

```bash
./start.sh
```

This will:
- Generate IIIF and Opensearch files (if not already present)
- Start Opensearch in Docker
- Wait for Opensearch to be healthy
- Create indexes and ingest data
- Start a static file server on port 3000

Options:
- `--skip-generate` — Skip file generation (use existing files)
- `--skip-ingest` — Skip index creation and data ingest

### Running rosa-viewer

After starting the local environment, run rosa-viewer in development mode:

```bash
cd rosa-viewer
npm install  # First time only
npm run dev
```

Open http://localhost:3001 in your browser.

The rosa-viewer dev server proxies requests:
- `/iiif` → http://localhost:3000 (IIIF files)
- `/logo` → http://localhost:3000 (logos from rosa-viewer/public/)

### Checking Status

```bash
./status.sh
```

Shows the status of all services and generated files.

### Stopping the Environment

```bash
./stop.sh
```

Options:
- `--clean` — Also remove generated files (site/, opensearch-data/)

## Opensearch Index Definitions

The `opensearch/` directory contains index mapping definitions:

- `manifest.json` — Index for book/manuscript metadata
- `canvas.json` — Index for page-level content and annotations

These files define the field mappings, analyzers (for multi-language text), and settings for each index. See [doc/opensearch-indexes.md](../doc/opensearch-indexes.md) for detailed field documentation.

To create indexes manually:

```bash
curl -X PUT "http://localhost:9200/manifest" \
  -H "Content-Type: application/json" \
  -d @opensearch/manifest.json

curl -X PUT "http://localhost:9200/canvas" \
  -H "Content-Type: application/json" \
  -d @opensearch/canvas.json
```

## Production Deployment

For production deployment, see [doc/deployment.md](../doc/deployment.md).
