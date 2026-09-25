# AGENTS.md

Onboarding guide for agents and developers working in the rosa2 repository.

Rosa turns a file-system archive of digitized books (metadata, transcriptions,
Archaeology of Reading annotations) into static IIIF Presentation 3.0 JSON and
Opensearch bulk-ingest NDJSON. A Mirador 4 viewer with a custom search plugin
(JHSearch) serves the result. Detailed docs are in `docs/` and in each
component's `README.md`. This file covers what you need to work safely.

## Project layout

```
rosa2/
├── archive/              Shallow copy of the real archive (metadata only, no images). Collections: aor, dlmm, pizan, rose
├── rosa-tool/            Java 25 CLI (Maven, picocli). Builds target/rosa-tool.jar
│   └── src/main/java/rosa/archive/
│       ├── cli/          One *Command.java per subcommand, registered in Rosa2Cli.java
│       ├── core/         Archive reading, checking, serialize/, util/
│       ├── model/        Data model; model/aor/ = AoR annotation types (sealed Annotation interface)
│       ├── iiif/         IIIF Presentation 3.0 generation
│       ├── opensearch/   Bulk-ingest document generation
│       └── aor/          AoR statistics
│   └── src/test/resources/archive/   Test fixture archive (rose, aor, dlmm, pizan)
├── rosa-viewer/          Mirador 4 + React 18 + MUI 7, Vite 5, Vitest 2
│   ├── .env              Committed local-dev defaults for VITE_ROSA_* (override in .env.local)
│   ├── sites/            index.html chooser; aor/ and dlmm/ each: index.html, main.js (site definition), site.css
│   ├── scripts/          build-sites.mjs (npm run build:sites), package-site.sh (viewer + IIIF -> tarball)
│   └── src/
│       ├── index.js, config.js
│       ├── site/               createSiteViewer, resolveSiteConfig, shared site.css
│       ├── plugins/jhsearch/   components/ hooks/ services/ state/ utils/
│       ├── i18n/, styles/
│       └── test/               setup.js, mocks.js
├── rosa-search/
│   ├── opensearch/       Index definitions (manifest.json, canvas.json) + Latin analyzer dictionaries
│   ├── lambda/           search_proxy/ and admin/ (plain Python, no deps), tests/
│   └── init-opensearch.sh   Creates indexes and loads data into AWS
├── jhu-rosa2-tf/         OpenTofu: AWS managed Opensearch, Lambdas, HTTP API (prod/stage)
├── rosa-image-server/    Cantaloupe + Kakadu Docker image (IIIF Image API) for ECS
├── local-dev/            Docker Opensearch 3 + static IIIF server scripts
├── docs/                 usage, archive-structure, iiif-generation, opensearch-indexes, search, aor-data-model, deployment
└── .github/workflows/    ci.yml (PRs + main), deploy-stage.yml (GitHub Pages), release.yml (manual release)
```

Data flow: `archive/` → `rosa-tool` → (IIIF JSON static files + `<collection>.bulk.json`)
→ Opensearch → `rosa-viewer`, which queries `/_search`, `/manifest/_search` and `/canvas/_search`.

The viewer ships as two sites built from the same code: **AoR** (`sites/aor`,
collection `aor`, no facets) and **DLMM** (`sites/dlmm`, collection `dlmm` with
children `rose` and `pizan`). Site identity and styling are fixed in
`sites/<site>/main.js` and `site.css`; only deployment URLs vary, via
`VITE_ROSA_IIIF_BASE_URL` and `VITE_ROSA_IMAGE_BASE_URL`. Environments: local-dev
(Vite + `local-dev/`), stage (GitHub Pages via `Deploy Stage`), prod (tarballs
from `Release`, uploaded to WordPress portals). See `docs/deployment.md`.

## Prerequisites

Java 25 JDK, Maven 3.9+, Node.js 20+ (CI uses 20), npm 9+, Docker with `docker compose`.
Deploying to AWS also needs OpenTofu ≥ 1.10, AWS CLI v2 and `jq`. Lambda tests need `python3`.

## Commands

### rosa-tool (run from `rosa-tool/`)

```sh
mvn clean package                  # build + test -> target/rosa-tool.jar (shaded fat jar)
mvn clean package -DskipTests      # quick build
mvn verify --no-transfer-progress  # what CI runs
mvn test -Dtest=ClassName          # single test class (JUnit 5 + jqwik)
java -jar target/rosa-tool.jar --help
java -jar target/rosa-tool.jar <command> --help
```

Subcommands (see `docs/usage.md` for options): `generate-iiif-pres`,
`generate-opensearch-ingest`, `shallow-copy`, `check`, `list`, `validate-xml`,
`aor-stats`, `update`, `update-image-list`, `crop-images`, `file-map`,
`rename-images`, `rename-files`, `rename-transcriptions`, `generate-tei`,
`check-aor`, `generate-annotation-map`, `migrate-tei-metadata`, `decorate-image-list`.

Common invocations against the in-repo archive:

```sh
java -jar rosa-tool/target/rosa-tool.jar check --archive archive
java -jar rosa-tool/target/rosa-tool.jar validate-xml --archive archive --collection aor
java -jar rosa-tool/target/rosa-tool.jar generate-iiif-pres --archive archive --output /tmp/iiif \
  [--base-url URL] [--image-base-url URL] [--image-api-version 2|3] [--opensearch-url URL]
java -jar rosa-tool/target/rosa-tool.jar generate-opensearch-ingest --archive archive --output /tmp/bulk
```

To add a subcommand, create `cli/<Name>Command.java` and add it to the
`subcommands` list in `cli/Rosa2Cli.java`, then document it in `docs/usage.md`.

### rosa-viewer (run from `rosa-viewer/`)

```sh
npm ci                  # install (CI); npm install locally
npm run dev             # Vite dev server on http://localhost:3001 (/ chooser, /aor/, /dlmm/)
npm test                # vitest run
npm run test:watch
npm run test:coverage
npm run lint            # eslint src/
npm run lint:fix
npm run format          # prettier --write
npm run format:check    # CI enforces this
npm run build           # -> dist/ (library build: es + umd, react/mirador external)
npm run build:sites     # -> dist/sites/{aor,dlmm}/ + dist/sites/index.html (self-contained sites)
npm run build:sites -- aor                       # one site
VITE_ROSA_IIIF_BASE_URL=https://x/iiif npm run build:sites   # env override (defaults in .env)
scripts/package-site.sh --site aor --site-url URL --opensearch-url URL --tarball aor-site.tar.gz
                        # viewer + pruned IIIF files for one site (needs rosa-tool.jar)
```

Before finishing viewer changes, run CI's checks: `npm run lint && npm run format:check && npm test && npm run build && npm run build:sites`.

The dev server proxies `/iiif` to `localhost:3000` and `/_search`,
`/manifest/_search` and `/canvas/_search` to `localhost:9200` (`vite.config.js`).

### rosa-search

```sh
cd rosa-search/lambda && python3 -m unittest discover -s tests -v   # no AWS or network needed
cd rosa-search/opensearch && python3 generate_latin_stemmer_rules.py  # regenerate latin-stemmer-rules.txt
```

### Local development environment (run from `local-dev/`)

```sh
cp config.env.example config.env   # set ROSA_ARCHIVE (absolute, or relative to local-dev/)
./start.sh                         # generate IIIF + bulk, start Opensearch in Docker, create indexes, ingest, serve IIIF on :3000
./start.sh --skip-generate         # reuse generated files
./start.sh --skip-ingest           # skip index creation and ingest
./status.sh
./stop.sh                          # stop IIIF server and Opensearch
./stop.sh --clean                  # also delete site/ and opensearch-data/
```

Then run `npm run dev` in `rosa-viewer/` and open http://localhost:3001 to choose
the AoR or DLMM site. `start.sh` needs `rosa-tool/target/rosa-tool.jar` to exist, so build it first.

### Infrastructure and deployment (run from `jhu-rosa2-tf/`)

```sh
tofu init -backend-config=prod.s3.tfbackend            # add -reconfigure when switching envs
tofu plan  -var-file=prod.tfvars
tofu apply -var-file=prod.tfvars
cd ../rosa-search && ./init-opensearch.sh --env prod   # --help for --skip-* / --yes options
```

Use `stage.s3.tfbackend` / `stage.tfvars` for stage. Image server build:
`cd rosa-image-server && docker build --platform linux/amd64 -t rosa-image-server:5.0.7 .`
(requires a licensed Kakadu ZIP in that directory).

## Boundaries

**Always**
- Run the relevant component's tests and lint before calling a change done.
- Keep `docs/` and component READMEs in sync when you change CLI options, index fields or config.
- When changing search query shapes in `rosa-viewer/src/plugins/jhsearch/utils/queryBuilder.js`
  or `services/opensearch.js`, run the Lambda tests too. `test_search_proxy.py` encodes the
  query shapes the viewer is allowed to send, and the production proxy rejects anything else.
- When changing an index field, update `rosa-search/opensearch/{manifest,canvas}.json`,
  the generator in `rosa-tool/.../opensearch/`, and `docs/opensearch-indexes.md` together.

**Ask first**
- Any `tofu apply`, `init-opensearch.sh`, `aws` command, or Docker push. These touch
  shared AWS environments. `init-opensearch.sh` drops and recreates the live indexes.
- Editing files under `archive/`, or running archive-mutating commands (`update`,
  `update-image-list`, `crop-images`, `rename-*`, `generate-tei`, `migrate-tei-metadata`,
  `decorate-image-list`, `generate-annotation-map`) against it. These rewrite archive data in place.
- Changes to `jhu-rosa2-tf/modules/rosa2-search/iam.tf`, `security-groups.tf`, `opensearch.tf` access policy, or
  the validation in `rosa-search/lambda/search_proxy/app.py`. These are security boundaries.
- Adding dependencies to either Lambda. They are deliberately dependency-free, and the zip is the source directory.
- Changing versions in `pom.xml` / `package.json`. `release.yml` manages them.

**Never**
- Commit a Kakadu ZIP or other licensed source (`rosa-image-server/.gitignore` excludes `*.zip`), credentials,
  `.terraform/`, or `local-dev/config.env` values that contain secrets.
- Hand-edit the Latin analyzer paths in `manifest.json`/`canvas.json` to package IDs.
  The checked-in files keep local-dev paths, and `init-opensearch.sh` rewrites copies into `build/index-defs/`.
- Ignore a failed index creation. The bulk ingest would then auto-create dynamically mapped
  indexes, typing facet fields as `text` and silently breaking faceting and sorting.
- Upgrade libjpeg-turbo in the image server to 3.x, which is incompatible with Cantaloupe 5.

## Conventions and gotchas

- IDs: manifest `{collection}.{book}` (e.g. `rose.Douce195`); canvas `{collection}.{image-id-no-ext}`
  (e.g. `aor.Ha2.001r`). Canvas URIs use a 0-based index `…/canvas/{page_num}` that matches the Opensearch `page_num` field.
- IIIF image identifiers contain slashes and are percent-encoded as a single segment (`rose%2FDouce195%2F…`).
- IIIF JSON output is compact, deterministic, and omits nulls and empty values. Keep it that way.
- Text fields use per-language sub-fields (`.en .fr .ofr .la .it .el .es .de`); unknown languages fall back to `.en`.
  Mixed fields (`mark`, `symbol`, `drawing`, `calculation`, `graph`, `table`) also have `.keyword`.
- The Opensearch Bulk API returns HTTP 200 even when documents fail. Check for `"errors":true`.
- Treat `_score` only as a relative ranking. Add a secondary sort such as `id` when you need stable pagination.
- Opensearch analyzers are not `updateable`. After a dictionary change, rebuild the indexes.
- Java style: records, sealed interfaces and pattern matching (Java 25) are in use. Match the surrounding code.
- Viewer formatting is Prettier (`.prettierrc`) plus ESLint flat config (`eslint.config.js`).
- Viewer tests are colocated with the code as `src/**/*.test.{js,jsx}`; shared setup is in `src/test/`.
- Adding a viewer site = a new `sites/<id>/` with `index.html`, `main.js`, `site.css`; the build
  scripts discover sites by directory. Keep collection ids and `enableFacets` in `main.js`, never in env vars.
- `VITE_ROSA_*` values are substituted into `sites/<site>/index.html` and remain editable in the
  built page. `rosa-viewer/.env` is committed on purpose; put personal overrides in `.env.local`.
