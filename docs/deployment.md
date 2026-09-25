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
- Opensearch 3.x cluster (for search functionality)

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

The viewer is published as two sites, AoR and DLMM, defined in
`rosa-viewer/sites/`. Each is a self-contained static site (Mirador and React
bundled) whose deployment-specific URLs come from `VITE_ROSA_*` variables
(defaults in `rosa-viewer/.env`, see [rosa-viewer/README.md](../rosa-viewer/README.md#sites)).

```bash
cd rosa-viewer
npm install
npm run build:sites            # dist/sites/aor/, dist/sites/dlmm/, dist/sites/index.html
VITE_ROSA_IIIF_BASE_URL=https://example.org/viewer/iiif npm run build:sites -- aor
npm run build                  # library build only: dist/rosa-viewer.{es,umd}.js
```

## Running rosa-tool

```bash
java -jar rosa-tool/target/rosa-tool.jar <command> [options]
```

Verify the build works:

```bash
java -jar rosa-tool/target/rosa-tool.jar --help
```

## Environments

| Environment | Viewer | IIIF files | Search | How |
|-------------|--------|------------|--------|-----|
| local-dev | Vite dev server on :3001 (`/`, `/aor/`, `/dlmm/`) | `local-dev/site/iiif` on :3000 | Docker Opensearch on :9200 | `local-dev/start.sh` + `npm run dev` |
| stage | GitHub Pages: `<pages-url>/aor/`, `<pages-url>/dlmm/` | `<pages-url>/iiif/` (shared) | stage API Gateway | `Deploy Stage` workflow |
| prod | Uploaded to the AoR and DLMM WordPress portals | `<site-url>/iiif/` inside each site package | prod API Gateway | `Release` workflow → `aor-site.tar.gz`, `dlmm-site.tar.gz` |

The same viewer build serves every environment; only the `VITE_ROSA_*` URLs, the
IIIF `--base-url` and the Opensearch URL written to `jhsearch.json` change.

## Packaging a Site

`rosa-viewer/scripts/package-site.sh` builds everything one site needs for a
static host: the viewer built for its public URL plus the IIIF Presentation
files for its collections (AoR: `aor`; DLMM: `dlmm`, `rose`, `pizan`), with
all ids generated under `<site-url>/iiif/`.

```bash
cd rosa-tool && mvn package -DskipTests && cd ..
cd rosa-viewer && npm ci && cd ..

rosa-viewer/scripts/package-site.sh \
  --site dlmm \
  --site-url https://dlmm.library.jhu.edu/viewer \
  --opensearch-url https://<api-id>.execute-api.us-east-1.amazonaws.com/_search \
  --image-base-url https://image.library.jhu.edu/iiif \
  --image-api-version 2 \
  --tarball dlmm-site.tar.gz
```

Run it with `--help` for all options (`--archive`, `--output`, `--jar`). Unpack
the result at the site URL so that `<site-url>/index.html` and
`<site-url>/iiif/dlmm/collection.json` resolve. Nothing else is needed on the
host: the pages use relative asset paths, and the only runtime dependencies are
the IIIF Image API server and the Opensearch endpoint, whose CORS configuration
must allow the site's origin.

## Typical Deployment Pipeline

The manual equivalent of `package-site.sh`, for hosting the viewer and IIIF
files yourself:

### 1. Build Components

```bash
# Build rosa-tool
cd rosa-tool && mvn clean package && cd ..

# Build rosa-viewer sites for the URL they will be served from
cd rosa-viewer && npm install && \
  VITE_ROSA_IIIF_BASE_URL=https://viewer.example.org/iiif npm run build:sites && cd ..
```

### 2. Generate IIIF Files

```bash
java -jar rosa-tool/target/rosa-tool.jar generate-iiif-pres \
  --archive /data/archive \
  --output /deploy/iiif \
  --base-url https://viewer.example.org/iiif/
```

### 3. Create Opensearch Indexes

The steps below apply to an Opensearch cluster you can reach and authenticate to
directly, such as a self-hosted one. For the AWS managed deployment, skip to
[Deploying to AWS managed Opensearch](#deploying-to-aws-managed-opensearch) —
that domain has no public endpoint and the index definitions need rewriting, so
these `curl` commands do not apply to it.

Using the index definition files in `rosa-search/opensearch/`:

```bash
curl -X PUT "https://search.example.org:9200/manifest" \
  -H "Content-Type: application/json" \
  -d @rosa-search/opensearch/manifest.json

curl -X PUT "https://search.example.org:9200/canvas" \
  -H "Content-Type: application/json" \
  -d @rosa-search/opensearch/canvas.json
```

Check the response of each request. Index creation fails if the Latin analyzer
resource files are not readable by Opensearch, and the failure is easy to miss:
the Bulk API in the next step will happily auto-create the indexes with dynamic
mappings instead. Dynamic mappings type the facet fields as `text` rather than
`keyword`, which breaks faceting and sorting. A successful request returns
`{"acknowledged":true,...}` with HTTP 200.

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

Serve the IIIF files and the built sites via a static web server (nginx, Apache,
S3, etc.). If the IIIF files live on a different origin than the sites, they
need CORS headers.

Example nginx configuration serving both sites and the IIIF files from one host:

```nginx
server {
    listen 443 ssl;
    server_name viewer.example.org;

    # IIIF Presentation API files
    location /iiif/ {
        alias /deploy/iiif/;
        add_header Access-Control-Allow-Origin *;
        add_header Access-Control-Allow-Methods "GET, OPTIONS";
    }

    # Sites (each contains index.html, assets/ and logo/)
    location /aor/  { alias /deploy/rosa-viewer/dist/sites/aor/; }
    location /dlmm/ { alias /deploy/rosa-viewer/dist/sites/dlmm/; }
}
```

## GitHub Actions

Workflows live in `.github/workflows/`. The manual ones (`Deploy Stage`,
`Release`) run against the branch chosen in the "Use workflow from" dropdown of
the Run workflow dialog; the optional `branch` input builds a different branch
than the one the workflow file was taken from.

### ci.yml (CI)

Runs on every pull request and on pushes to master:
- Builds and tests rosa-tool (Java 25)
- Lints, format-checks, tests and builds rosa-viewer (Node.js 20), including
  both site builds

### deploy-stage.yml (Deploy Stage)

Publishes the stage sites to this repository's GitHub Pages site. Repository
Settings → Pages → Source must be set to **GitHub Actions** once.

Inputs:

| Input | Description | Default |
|-------|-------------|---------|
| `branch` | Branch to build | the selected workflow branch |
| `opensearch_url` | Opensearch `_search` URL (stage API Gateway endpoint) | required |
| `image_base_url` | IIIF Image API base URL | `https://image.library.jhu.edu/iiif` |
| `image_api_version` | IIIF Image API version | `2` |

The workflow builds rosa-tool, generates the IIIF files from the in-repo
`archive/` with ids under `<pages-url>/iiif/`, builds both sites with
`VITE_ROSA_IIIF_BASE_URL=<pages-url>/iiif`, and deploys:

```
<pages-url>/            chooser page
<pages-url>/aor/        The Archaeology of Reading
<pages-url>/dlmm/       Digital Library of Medieval Manuscripts
<pages-url>/iiif/       IIIF Presentation files (all collections)
```

The Pages origin (`https://<owner>.github.io`) must be in
`cors_allowed_origins` in `jhu-rosa2-tf/stage.tfvars`.

### release.yml (Release)

Creates a GitHub release and bumps development versions.

Inputs:

| Input | Description | Default |
|-------|-------------|---------|
| `release_version` | Release version (e.g. `2.0.0`) | required |
| `next_version` | Next development version (e.g. `2.1.0-SNAPSHOT`) | required |
| `branch` | Branch to release | the selected workflow branch |
| `opensearch_url` | Opensearch `_search` URL written into the sites (prod API Gateway endpoint) | required |
| `aor_site_url` | URL the AoR site will be served from | `https://archaeologyofreading.org/viewer` |
| `dlmm_site_url` | URL the DLMM site will be served from | `https://dlmm.library.jhu.edu/viewer` |
| `image_base_url` | IIIF Image API base URL | `https://image.library.jhu.edu/iiif` |
| `image_api_version` | IIIF Image API version | `2` |

Steps:

1. Sets the release version in rosa-tool (pom.xml) and rosa-viewer (package.json)
2. Builds and tests both components
3. Packages each site with `package-site.sh` for its site URL
4. Creates a GitHub release with:
   - `rosa-tool.jar` — Executable Java CLI
   - `rosa-viewer-{version}.tar.gz` — Viewer library build
   - `aor-site.tar.gz`, `dlmm-site.tar.gz` — Deployable sites (viewer + IIIF files).
     Unpack at the site URL; see [Packaging a Site](#packaging-a-site).
5. Updates versions to the next development snapshot and commits

To create a release: Actions → Release → Run workflow, pick the branch, fill in
the inputs and click "Run workflow".

## Deploying to AWS managed Opensearch

The production search backend is an AWS managed Opensearch domain with no public
endpoint, fronted by an API Gateway HTTP API and a request-signing Lambda.
Infrastructure is in [`jhu-rosa2-tf`](../jhu-rosa2-tf/); the index setup and data
load are driven by `rosa-search/init-opensearch.sh`.

Apply the infrastructure, then load the data:

```bash
cd jhu-rosa2-tf
tofu init -backend-config=prod.s3.tfbackend
tofu apply -var-file=prod.tfvars

cd ../rosa-search
./init-opensearch.sh --env prod
```

The script prints the `_search` URL to pass to `generate-iiif-pres
--opensearch-url`. That URL is the API Gateway endpoint, not the domain itself:

```bash
java -jar rosa-tool/target/rosa-tool.jar generate-iiif-pres \
  --archive /data/archive \
  --output /deploy/iiif \
  --base-url https://iiif.example.org \
  --opensearch-url https://<api-id>.execute-api.us-east-1.amazonaws.com/_search
```

Three differences from a directly reachable cluster are worth noting:

- **The endpoint is read-only by construction.** The signing Lambda accepts only
  `POST` to `/_search`, `/manifest/_search`, and `/canvas/_search`, and its IAM
  credentials are scoped to those same paths. Index creation and ingest go
  through a separate administration Lambda inside the VPC.
- **The index definitions are rewritten at deploy time.** They reference the Latin
  dictionaries by filename, which only works for the local-dev bind mount. AWS
  addresses them as custom packages by ID, and `init-opensearch.sh` substitutes
  the IDs. The checked-in files are left alone.
- **Queries are validated and capped.** `size` is limited to 100, `from + size` to
  10000, and request bodies to 16 KB; scripts, `query_string`, `regexp`, and
  wildcard queries are rejected. See `jhu-rosa2-tf/README.md` for the full list.

For per-environment details, the access-control model, and the Latin analyzer
package lifecycle, see [`jhu-rosa2-tf/README.md`](../jhu-rosa2-tf/README.md) and
[`rosa-search/README.md`](../rosa-search/README.md).

## Local Development

For local development and testing, use the local-dev environment:

```bash
cd local-dev
cp config.env.example config.env
# Edit config.env to set ROSA_ARCHIVE

./start.sh

# In another terminal
cd rosa-viewer
npm run dev
```

Open http://localhost:3001 and choose a site, or go to http://localhost:3001/aor/
or http://localhost:3001/dlmm/. Run `./start.sh --help` for the available options.

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
