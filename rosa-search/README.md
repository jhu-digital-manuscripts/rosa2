# rosa-search

Opensearch index definitions, Latin analyzer resources, and the AWS deployment
pieces for rosa2 search.

## Contents

| Path | Purpose |
|---|---|
| `opensearch/manifest.json` | Index definition: one document per book. |
| `opensearch/canvas.json` | Index definition: one document per page, annotations merged in. |
| `opensearch/latin-stemmer-rules.txt` | 29,760 lemmatization rules for the `stemmer_override` filter. |
| `opensearch/latin-charmap.txt` | Character normalization: v/u, j/i, ligatures, long s. |
| `opensearch/latin-stopwords.txt` | Stop words for the fallback analyzer configuration. |
| `opensearch/generate_latin_stemmer_rules.py` | Regenerates the rules file from the lemma table. |
| `opensearch/LATIN-ANALYZER.md` | How the Latin analyzers work and how to deploy them. |
| `lambda/search_proxy/` | Validates and SigV4-signs browser queries. |
| `lambda/admin/` | Creates indexes and ingests bulk data from inside the VPC. |
| `lambda/tests/` | Unit tests for both functions. |
| `init-opensearch.sh` | Builds the indexes and loads the data into an AWS environment. |

Infrastructure lives in [`../jhu-rosa2-tf`](../jhu-rosa2-tf). See its README for
the architecture and access-control model.

## Deploying to AWS

Apply the infrastructure first, then load the data:

```sh
cd ../jhu-rosa2-tf
tofu init -backend-config=prod.s3.tfbackend
tofu apply -var-file=prod.tfvars

cd ../rosa-search
./init-opensearch.sh --env prod
```

`init-opensearch.sh --help` lists the options. It requires Java 25+, the AWS CLI
v2, `jq`, and OpenTofu, plus a built `rosa-tool/target/rosa-tool.jar`.

What it does:

1. Reads the deployed domain, Lambda, bucket, and package IDs from the OpenTofu
   outputs, and refuses to run if the workspace is initialized for a different
   environment than `--env`.
2. Generates bulk NDJSON from `archive/` with `generate-opensearch-ingest`.
3. Rewrites the index definitions to reference the Latin analyzer packages.
4. Stages the definitions and bulk files in S3.
5. Drops and recreates `manifest` and `canvas`, prompting first.
6. Ingests the bulk data, resuming across invocations as needed.
7. Verifies the document counts and that the Latin analyzer lemmatizes.

Steps 5 through 7 go through the admin Lambda rather than talking to the domain,
because the domain has no public endpoint. Nothing needs a bastion host or a VPN.

### Index definitions are rewritten, not edited

`manifest.json` and `canvas.json` reference the Latin dictionaries by filename:

```json
"rules_path": "analyzers/latin-stemmer-rules.txt"
```

That is the path local-dev bind-mounts into the container. AWS exposes the same
files as custom packages addressed by ID, so `init-opensearch.sh` substitutes
them at deploy time:

```json
"rules_path": "analyzers/F123456789"
```

The checked-in files keep the local-dev paths so `local-dev/start.sh` continues
to work unchanged. The rewritten copies are written to `build/index-defs/`.

If index creation fails with `IOException while reading mappings_path`, the
analyzer packages are not readable by the domain. Check the associations:

```sh
aws opensearch list-packages-for-domain --domain-name jhu-rosa2 --region us-east-1
```

A failed index creation must never be ignored: the bulk ingest that follows would
auto-create the index with dynamic mappings, typing every facet field as `text`
instead of `keyword` and silently breaking faceting and sorting in the viewer.
Both `init-opensearch.sh` and the admin Lambda treat it as fatal.

## Local development

For local Opensearch in Docker, use `local-dev/start.sh` instead. It mounts the
dictionary files into the container and posts index definitions and bulk data
directly, with no signing or proxy involved.

## The Lambda functions

Both are plain Python with no third-party dependencies. Signing uses `botocore`,
which the Lambda Python runtimes already bundle, so there is no build step and no
layer — the deployment zip is the source directory, packaged by OpenTofu.

### `search_proxy`

Sits behind API Gateway. Validates each request, signs it, and forwards it. Only
`POST` to `/_search`, `/manifest/_search`, and `/canvas/_search` is accepted —
the three paths `src/plugins/jhsearch/services/opensearch.js` uses. A bare
`/_search` is rewritten to `/manifest,canvas/_search` so it cannot reach system
indexes.

The validator rejects code execution, unbounded clause types, cross-index reads,
mapping disclosure, and oversized work. See the module docstring for the full
list and the tunable environment variables.

### `admin`

Invoked directly with `aws lambda invoke`. Reads bulk payloads from S3 rather
than from the invocation, which keeps the request small and sidesteps the
payload size limits. Actions: `health`, `cluster_settings`, `recreate_index`,
`bulk_ingest`, `refresh`, `count`, `analyze`.

`bulk_ingest` is resumable. Each invocation works through as much of an object as
its time budget allows, then returns the byte offset to continue from; the caller
loops until `done`. Batches always contain whole action/document line pairs.

### Tests

```sh
cd lambda
python3 -m unittest discover -s tests -v
```

No AWS credentials or network access needed; the SDKs are stubbed. The accept
cases in `test_search_proxy.py` are the query shapes
`rosa-viewer/src/plugins/jhsearch/utils/queryBuilder.js` emits, so a failure
there means the viewer is about to break.

## Regenerating the stemmer rules

```sh
cd opensearch
python3 generate_latin_stemmer_rules.py
```

Reads `latin-lemma-table.jsonl` and rewrites `latin-stemmer-rules.txt`. After
regenerating, re-apply the OpenTofu workspace to update the custom package, then
rerun `init-opensearch.sh` to rebuild the indexes — the analyzers are not
declared `updateable`, so existing indexes keep using the old dictionary until
they are recreated.
