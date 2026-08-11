# Deployment

## Purpose

This document describes how to build, package, and deploy Rosa components, including running them in production environments.

## Audience

Developers building the project and system administrators deploying the tools.

## Prerequisites

- Java 25 or later (JDK)
- Maven 3.9+
- Node.js 20+
- npm 9+
- Access to the archive data directory
- Opensearch cluster (for search functionality)

## Building

### rosa-tool

Build the executable JAR from the rosa-tool directory:

```bash
cd rosa-tool
mvn clean package
```

This produces a fat JAR at `target/rosa-tool.jar` containing all runtime dependencies. The JAR includes a `Main-Class` manifest entry, so it can be executed directly.

To skip tests during development builds:

```bash
mvn clean package -DskipTests
```

### rosa-viewer

Build the viewer from the rosa-viewer directory:

```bash
cd rosa-viewer
npm install
npm run build
```

This produces optimized static files in the `dist/` directory.

## Running rosa-tool

```bash
java -jar rosa-tool/target/rosa-tool.jar <command> [options]
```

Verify the build works:

```bash
java -jar rosa-tool/target/rosa-tool.jar --help
```

## Typical Deployment Pipeline

A standard deployment workflow for generating and publishing content:

### 1. Build Components

```bash
# Build rosa-tool
cd rosa-tool && mvn clean package && cd ..

# Build rosa-viewer
cd rosa-viewer && npm install && npm run build && cd ..
```

### 2. Generate IIIF Files

Use the convenience script or run rosa-tool directly:

```bash
# Using the script
./deploy/index-archive.sh \
  --archive /data/archive \
  --output /deploy/iiif \
  --base-url https://iiif.example.org \
  --image-base-url https://image.example.org/iiif

# Or directly
java -jar rosa-tool/target/rosa-tool.jar generate-iiif-pres \
  --archive /data/archive \
  --output /deploy/iiif \
  --base-url https://iiif.example.org
```

### 3. Create Opensearch Indexes

Using the index definition files in `deploy/opensearch/`:

```bash
curl -X PUT "https://search.example.org:9200/manifest" \
  -H "Content-Type: application/json" \
  -d @deploy/opensearch/manifest.json

curl -X PUT "https://search.example.org:9200/canvas" \
  -H "Content-Type: application/json" \
  -d @deploy/opensearch/canvas.json
```

### 4. Generate and Ingest Bulk Data

```bash
java -jar rosa-tool/target/rosa-tool.jar generate-opensearch-ingest \
  --archive /data/archive --output /deploy/bulk

for f in /deploy/bulk/*.bulk.json; do
  curl -X POST "https://search.example.org:9200/_bulk" \
    -H "Content-Type: application/x-ndjson" --data-binary @"$f"
done
```

### 5. Deploy Static Files

Serve IIIF files and rosa-viewer via a static web server (nginx, Apache, S3, etc.) with CORS headers enabled for IIIF viewer access.

Example nginx configuration:

```nginx
server {
    listen 443 ssl;
    server_name iiif.example.org;

    # IIIF Presentation API files
    location /iiif/ {
        alias /deploy/iiif/;
        add_header Access-Control-Allow-Origin *;
        add_header Access-Control-Allow-Methods "GET, OPTIONS";
    }

    # rosa-viewer application
    location / {
        root /deploy/rosa-viewer/dist;
        try_files $uri $uri/ /index.html;
    }

    # Logos
    location /logo/ {
        alias /deploy/logos/;
        add_header Access-Control-Allow-Origin *;
    }
}
```

## GitHub Actions Release

The project includes GitHub Actions workflows:

### build.yml

Runs on pull requests and pushes to main:
- Builds and tests rosa-tool (Java 25)
- Builds and tests rosa-viewer (Node.js 20, including lint and format checks)

### release.yml

Triggered manually via `workflow_dispatch` with version inputs:

1. Sets the release version in rosa-tool (pom.xml) and rosa-viewer (package.json)
2. Builds and tests both components
3. Creates a GitHub release with:
   - `rosa-tool.jar` — Executable Java CLI
   - `rosa-viewer-{version}.tar.gz` — Built viewer files
4. Updates versions to the next development snapshot and commits

To create a release:

1. Go to Actions → Release → Run workflow
2. Enter the release version (e.g., `2.0.0`)
3. Enter the next development version (e.g., `2.1.0-SNAPSHOT`)
4. Click "Run workflow"

## Local Development

For local development and testing, use the local-dev environment:

```bash
cd deploy/local-dev
cp config.env.example config.env
# Edit config.env to set ROSA_ARCHIVE

./start.sh

# In another terminal
cd rosa-viewer
npm run dev
```

See [deploy/README.md](../deploy/README.md) for detailed instructions.

## Validation

After deployment, verify data integrity:

```bash
java -jar rosa-tool.jar check --archive /data/archive
java -jar rosa-tool.jar validate-xml --archive /data/archive --collection aor
```

Verify IIIF endpoints:

```bash
curl https://iiif.example.org/iiif/collection.json
```

Verify Opensearch:

```bash
curl https://search.example.org:9200/manifest/_count
curl https://search.example.org:9200/canvas/_count
```
