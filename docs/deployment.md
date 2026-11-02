# Deployment

## Purpose

How to build, package and deploy the Rosa components, and how each environment
is put together. Command options are documented in [Tool Usage](usage.md); what
the generated files contain is in [IIIF Generation](iiif-generation.md) and
[Opensearch Indexes](opensearch-indexes.md). This page is the one place that
holds the index creation and ingest steps and the image server base URLs.

## Audience

Developers building the project and system administrators deploying it.

## Prerequisites

- Java 25 (JDK) and Maven 3.9+
- Node.js 22.12+ and npm 9+
- Docker with `docker compose` (local Opensearch)
- Access to the archive data directory
- For AWS: OpenTofu 1.10+, AWS CLI v2 and `jq`

## Building

### rosa-tool

```bash
cd rosa-tool
mvn clean package              # build + test -> target/rosa-tool.jar
mvn clean package -DskipTests  # quick build
```

`target/rosa-tool.jar` is a shaded fat jar with a `Main-Class` entry, so
`java -jar rosa-tool/target/rosa-tool.jar --help` lists the commands and
`--version` prints the version from the jar manifest.

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

## Environments

| Environment | Viewer | IIIF files | Search | Image server | How |
|-------------|--------|------------|--------|--------------|-----|
| local-dev | Vite dev server on :3001 (`/`, `/aor/`, `/dlmm/`) | `local-dev/site/iiif` on :3000 | Docker Opensearch 3.1 on :9200 | stage CloudFront (see `local-dev/config.env.example`) | `local-dev/start.sh` + `npm run dev` |
| stage | GitHub Pages: `<pages-url>/aor/`, `<pages-url>/dlmm/` | `<pages-url>/iiif/` (shared) | stage API Gateway | stage CloudFront | `Deploy Stage` workflow |
| prod | Uploaded to the AoR and DLMM WordPress portals | `<site-url>/iiif/` inside each site package | prod API Gateway | `image.library.jhu.edu` | `Release` workflow → `aor-site.tar.gz`, `dlmm-site.tar.gz` |

The same viewer build serves every environment; only the `VITE_ROSA_*` URLs, the
IIIF `--base-url`, the image server URL and the Opensearch URL written to
`jhsearch.json` change.

## Packaging a site

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
  --image-base-url https://image.library.jhu.edu/iiif/3 \
  --image-api-version 3 \
  --tarball dlmm-site.tar.gz
```

Run it with `--help` for all options (`--archive`, `--output`, `--jar`). Unpack
the result at the site URL so that `<site-url>/index.html`,
`<site-url>/styles/dlmm.css` and `<site-url>/iiif/dlmm/collection.json` resolve.
The IIIF annotations reference the stylesheet in `styles/` (IIIF cookbook recipe
0045); it is copied from `rosa-viewer/public/styles/`. Nothing else is needed on the
host: the pages use relative asset paths, and the only runtime dependencies are
the IIIF Image API server and the Opensearch endpoint, whose CORS configuration
must allow the site's origin.

## Manual pipeline

The manual equivalent of `package-site.sh`, for hosting the viewer and IIIF
files yourself against an Opensearch cluster you can reach directly.

### 1. Build the components

```bash
cd rosa-tool && mvn clean package && cd ..
cd rosa-viewer && npm install && \
  VITE_ROSA_IIIF_BASE_URL=https://viewer.example.org/iiif npm run build:sites && cd ..
```

### 2. Generate the IIIF files

```bash
java -jar rosa-tool/target/rosa-tool.jar generate-iiif-pres \
  --archive /data/archive \
  --output /deploy/iiif \
  --base-url https://viewer.example.org/iiif/ \
  --image-base-url https://image.example.org/iiif/3 \
  --image-api-version 3 \
  --opensearch-url https://search.example.org:9200/_search \
  --annotation-stylesheet-url 'https://viewer.example.org/{collection}/styles/{collection}.css'
```

The options are described under
[`generate-iiif-pres`](usage.md#generate-iiif-pres). `{collection}` becomes
`aor` or `dlmm`, which are also the site directories, so the URL names each
site's copy of `public/styles/`. The output directory is not emptied first, so
delete stale output yourself (`package-site.sh` does).

### 3. Create the indexes and ingest

See [Creating indexes and ingesting](#creating-indexes-and-ingesting) below.

### 4. Serve the static files

Serve the IIIF files and the built sites from any static web server (nginx,
Apache, S3, ...). If the IIIF files live on a different origin than the sites,
they need CORS headers.

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

    # Sites (each contains index.html, assets/, logo/ and styles/)
    location /aor/  { alias /deploy/rosa-viewer/dist/sites/aor/; }
    location /dlmm/ { alias /deploy/rosa-viewer/dist/sites/dlmm/; }
}
```

## Creating indexes and ingesting

These steps apply to an Opensearch 3.x cluster you can reach and authenticate to
directly, such as `local-dev` or a self-hosted one. The AWS managed domain has no
public endpoint and needs its index definitions rewritten, so there
[`init-opensearch.sh`](#deploying-to-aws-managed-opensearch) does all of this.

### Create the indexes

Create both indexes from the definitions in `rosa-search/opensearch/` before
ingesting anything:

```bash
curl -X PUT "https://search.example.org:9200/manifest" \
  -H "Content-Type: application/json" \
  -d @rosa-search/opensearch/manifest.json

curl -X PUT "https://search.example.org:9200/canvas" \
  -H "Content-Type: application/json" \
  -d @rosa-search/opensearch/canvas.json
```

Check the response of each request: a success is HTTP 200 with
`{"acknowledged":true,...}`. Creation fails if the Latin analyzer dictionaries
(`latin-stemmer-rules.txt`, `latin-charmap.txt`) are not installed where the
definitions expect them (`local-dev/docker-compose.yml` bind-mounts them into the
container's `config/analyzers/`). Never ignore a failed creation: the Bulk API in
the next step would auto-create the indexes with dynamic mappings, which type the
facet fields as `text` rather than `keyword` and silently break faceting and
sorting. Both mappings are `dynamic: strict`, so a bulk file that carries a field
the mapping lacks is rejected rather than mapped on the fly.

### Generate and ingest the bulk data

```bash
java -jar rosa-tool/target/rosa-tool.jar generate-opensearch-ingest \
  --archive /data/archive --output /deploy/bulk

for f in /deploy/bulk/*.bulk.json; do
  curl -X POST "https://search.example.org:9200/_bulk" \
    -H "Content-Type: application/x-ndjson" --data-binary @"$f"
done
```

One `<collection>.bulk.json` is produced per collection that has books (the
book-less `dlmm` parent has none). The Bulk API returns HTTP 200 even when
documents fail, so check each response body for `"errors":true` and inspect the
`items` entries that carry an `error`.

### Verify

```bash
curl https://search.example.org:9200/manifest/_count
curl https://search.example.org:9200/canvas/_count
```

The counts must equal the number of `{"index":...}` action lines across the bulk
files for each index. Through the search proxy `_count` is not available; use
the equivalent search instead, which also works locally:

```bash
curl -X POST "<search-url>/manifest/_search" -H "Content-Type: application/json" \
  -d '{"size":0,"track_total_hits":true}'
```

Mapping and analyzer changes take effect only after the indexes are dropped and
recreated. Locally that is `local-dev/start.sh`; on AWS it is
`init-opensearch.sh`.

## Local development

```bash
cd local-dev
cp config.env.example config.env
# Edit config.env to set ROSA_ARCHIVE (absolute, or relative to local-dev/)

./start.sh                 # generate IIIF + bulk, start Opensearch 3.1 in Docker, create indexes, ingest, serve IIIF on :3000
./start.sh --skip-generate # reuse generated files
./start.sh --skip-ingest   # skip index creation and ingest
./status.sh
./stop.sh                  # stop IIIF server and Opensearch
./stop.sh --clean          # also delete site/ and opensearch-data/

# In another terminal
cd rosa-viewer
npm run dev
```

`start.sh` needs `rosa-tool/target/rosa-tool.jar`, so build it first. Open
http://localhost:3001 and choose a site, or go to http://localhost:3001/aor/ or
http://localhost:3001/dlmm/. The dev server proxies `/iiif` to :3000 and the
three `_search` paths to :9200 (`rosa-viewer/vite.config.js`). Run
`./start.sh --help` for the available options.

## GitHub Actions

Workflows live in `.github/workflows/`. The manual ones (`Deploy Stage`,
`Release`) run against the branch chosen in the "Use workflow from" dropdown of
the Run workflow dialog.

### ci.yml (CI)

Runs on every pull request and on pushes to `main`, as four jobs:

| Job | What it runs |
|-----|--------------|
| Build rosa-tool | `mvn verify` (Java 25) |
| Build rosa-viewer | lint, format check, tests, library build and both site builds (Node.js 22) |
| Test rosa-search Lambdas | `python3 -m unittest discover -s tests -v` in `rosa-search/lambda` (Python 3.13, no dependencies) |
| Test CloudFront Functions | `node --test 'jhu-rosa2-tf/modules/rosa2-image-server/functions/test/*.test.mjs'` (Node.js 22) |

### deploy-stage.yml (Deploy Stage)

Publishes the stage sites to this repository's GitHub Pages site. Repository
Settings → Pages → Source must be set to **GitHub Actions** once.

Inputs:

| Input | Description | Default |
|-------|-------------|---------|
| `opensearch_url` | Opensearch `_search` URL (stage API Gateway endpoint) | `https://zdnj8lukh6.execute-api.us-east-1.amazonaws.com/_search` |
| `image_base_url` | IIIF Image API base URL | `https://d32j4c172m91zy.cloudfront.net/iiif/3` (stage image server) |
| `image_api_version` | IIIF Image API version | `3` |

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
| `opensearch_url` | Opensearch `_search` URL written into the sites (prod API Gateway endpoint) | required |
| `aor_site_url` | URL the AoR site will be served from | `https://archaeologyofreading.org/viewer` |
| `dlmm_site_url` | URL the DLMM site will be served from | `https://dlmm.library.jhu.edu/viewer` |
| `image_base_url` | IIIF Image API base URL | `https://image.library.jhu.edu/iiif/3` |
| `image_api_version` | IIIF Image API version | `3` |

Steps:

1. Sets the release version in rosa-tool (pom.xml) and rosa-viewer (package.json)
2. Builds and tests both components
3. Packages each site with `package-site.sh` for its site URL
4. Creates a GitHub release with:
   - `rosa-tool.jar` — Executable Java CLI
   - `rosa-viewer-{version}.tar.gz` — Viewer library build
   - `aor-site.tar.gz`, `dlmm-site.tar.gz` — Deployable sites (viewer + IIIF files).
     Unpack at the site URL; see [Packaging a site](#packaging-a-site).
5. Updates versions to the next development snapshot and commits

To create a release: Actions → Release → Run workflow, pick the branch, fill in
the inputs and click "Run workflow". Before accepting the default
`image_base_url`, confirm that the server at `image.library.jhu.edu` answers on
`/iiif/3` (see [Deploying the image server](#deploying-the-image-server)).

## Deploying to AWS managed Opensearch

The production search backend is an AWS managed Opensearch domain with no public
endpoint, fronted by an API Gateway HTTP API and a request-signing Lambda.
Infrastructure is in [`jhu-rosa2-tf`](../jhu-rosa2-tf/); the index setup and data
load are driven by `rosa-search/init-opensearch.sh`.

Apply the infrastructure, then load the data:

```bash
cd jhu-rosa2-tf
tofu init -backend-config=prod.s3.tfbackend   # add -reconfigure when switching envs
tofu apply -var-file=prod.tfvars

cd ../rosa-search
./init-opensearch.sh --env prod               # --help for --skip-*, --archive, --jar, --yes
```

Use `stage.s3.tfbackend` / `stage.tfvars` for stage. `init-opensearch.sh` deletes
and recreates both indexes before ingesting, so it is also how any change to a
setting that is fixed at index creation takes effect: mappings, `number_of_shards`
and the Latin analyzer definitions among them. A change to
`rosa-search/opensearch/{manifest,canvas}.json` is not live until this has run.
The script exits non-zero if an index cannot be created, if any bulk document is
rejected, or if an index ends up with fewer documents than its bulk files
contain, and it checks that the Latin analyzer resolves `divido`, `dividunt` and
`dīvidunt` to the same stem. `--skip-generate` requires existing `*.bulk.json`
files in `build/opensearch-data/`; `--jar` points it at a rosa-tool jar other than
`../rosa-tool/target/rosa-tool.jar`.

The script prints the `_search` URL to pass to `generate-iiif-pres
--opensearch-url`. That URL is the API Gateway endpoint, not the domain itself.
Three differences from a directly reachable cluster:

- **The endpoint is read-only by construction.** The signing Lambda accepts only
  `POST` to `/_search`, `/manifest/_search`, and `/canvas/_search`, and its IAM
  credentials are scoped to those same paths. Index creation and ingest go
  through a separate administration Lambda inside the VPC.
- **The index definitions are rewritten at deploy time.** They reference the Latin
  dictionaries by filename, which only works for the local-dev bind mount. AWS
  addresses them as custom packages by ID, and `init-opensearch.sh` substitutes
  the IDs into copies under `build/index-defs/`. The checked-in files are left
  alone.
- **Queries are validated and capped.** See [Search](search.md#limits) for the
  client-facing limits and `jhu-rosa2-tf/README.md` for the full list of what the
  proxy rejects.

For per-environment details, the access-control model and the Latin analyzer
package lifecycle, see [`jhu-rosa2-tf/README.md`](../jhu-rosa2-tf/README.md) and
[`rosa-search/README.md`](../rosa-search/README.md).

## Deploying the image server

The IIIF Image API server ([`rosa-image-server`](../rosa-image-server/),
Cantaloupe with Kakadu) runs on ECS Fargate behind CloudFront. Its
infrastructure is the `rosa2-image-server` module in
[`jhu-rosa2-tf`](../jhu-rosa2-tf/README.md#image-server), in the same state and
tfvars as search. Both stage and prod are HTTPS: stage on its generated
`*.cloudfront.net` hostname, and prod on its own until it takes over
`image.library.jhu.edu`.

```bash
# Build the image and push it to the environment's ECR repository
cd rosa-image-server
./build-and-push.sh --kakadu-zip /path/to/v8_6_1-02130L.zip --env stage --push

# Deploy it: set the printed image_server_image_tag in stage.tfvars, then
cd ../jhu-rosa2-tf
tofu apply -var-file=stage.tfvars
tofu output image_server_iiif2_base_url     # https://<id>.cloudfront.net/iiif/2
```

The first apply for an environment comes before any image exists: run it once
with `image_server_image_tag = ""`, which creates the ECR repository with the
service scaled to zero, then push and apply again. See
[First deployment](../jhu-rosa2-tf/README.md#first-deployment). Build options,
checksums and the smoke test are in
[rosa-image-server/README.md](../rosa-image-server/README.md).

### Base URLs and API versions

The server answers on `/iiif/2/<id>` (Image API 2), `/iiif/3/<id>` (Image API
3) and the legacy `/iiif/<id>` (Image API 2, as served by the current
`image.library.jhu.edu`). Use one of these as `--image-base-url` with the
matching `--image-api-version`. rosa-tool writes every painting body as
`{service}/full/max/0/default.jpg`, which both API versions accept (Image API 3
has no `full` size).

| Where | Image base URL | API version |
|-------|----------------|-------------|
| `Deploy Stage` default | `https://d32j4c172m91zy.cloudfront.net/iiif/3` (stage image server) | 3 |
| `Release` default | `https://image.library.jhu.edu/iiif/3` | 3 |
| `package-site.sh` default | `https://image.library.jhu.edu/iiif` (legacy path) | 2 |
| `rosa-viewer/.env` default (`VITE_ROSA_IMAGE_BASE_URL`) | `https://image.library.jhu.edu/iiif` | - |

Until the prod cutover described in
[`jhu-rosa2-tf/README.md`](../jhu-rosa2-tf/README.md#image-server),
`image.library.jhu.edu` is the old image server. Confirm that it serves `/iiif/3`
(for example `curl -I https://image.library.jhu.edu/iiif/3/<id>/info.json`)
before running `Release` with its default `image_base_url`; otherwise pass the
legacy base URL with `image_api_version` `2`.

## Validation

After a deployment, check the data:

```bash
java -jar rosa-tool/target/rosa-tool.jar check --archive /data/archive
java -jar rosa-tool/target/rosa-tool.jar check --archive archive --skip-images   # shallow copy without images
java -jar rosa-tool/target/rosa-tool.jar validate-xml --archive /data/archive --collection aor
```

Verify the IIIF files resolve:

```bash
curl https://viewer.example.org/iiif/collection.json
curl https://viewer.example.org/iiif/rose/collection.json
```

Verify the search endpoint through the proxy:

```bash
curl -X POST "https://<api-id>.execute-api.us-east-1.amazonaws.com/manifest/_search" \
  -H "Content-Type: application/json" -d '{"size":0,"track_total_hits":true}'
curl -X POST "https://<api-id>.execute-api.us-east-1.amazonaws.com/canvas/_search" \
  -H "Content-Type: application/json" -d '{"size":0,"track_total_hits":true}'
```

`hits.total.value` must match the bulk files (250 manifests and 86,253 canvases
for the in-repo archive at the time of writing). Verify an image:

```bash
curl -I "https://<image-host>/iiif/3/rose%2FDouce195%2Fcropped%2FDouce195.001r/info.json"
```
