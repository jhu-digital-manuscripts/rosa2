# rosa2 search infrastructure (OpenTofu)

OpenTofu configuration for the rosa2 search backend: an AWS managed OpenSearch
domain with no public endpoint, the Latin analyzer dictionaries as custom
packages, a request-signing proxy Lambda behind an HTTP API, and an
administration Lambda that creates the indexes and ingests bulk data.

## Why there is a Lambda in front of OpenSearch

rosa-viewer runs in the browser and posts OpenSearch query DSL directly to a
`_search` endpoint. A browser cannot satisfy what a managed domain requires:

- **SigV4 signing.** The domain authenticates with IAM. There is no credential a
  public web page could hold.
- **Network reachability.** The domain is VPC-only, so it has no address the
  internet can route to.

The proxy Lambda supplies both. It is also the place to reject abusive queries,
because it is the only code path to the cluster.

```
browser ──POST /_search──▶ HTTP API ──▶ search Lambda ──SigV4──▶ OpenSearch
                           (throttle)   (validate, VPC)          (VPC only)

operator ──aws lambda invoke──▶ admin Lambda ──▶ OpenSearch
                                (VPC)     └──reads bulk data from S3
```

## Layout

The root module holds the backend, provider and per-environment inputs, and
calls a single child module, `modules/rosa2-search`, that declares every
resource.

| Root file | Purpose |
|---|---|
| `versions.tf` | Required OpenTofu/provider versions and the partial S3 backend. |
| `providers.tf` | AWS provider with `default_tags`. |
| `locals.tf` | The tag set (`Owner`, `Environment`, `System`) applied to every taggable resource through `default_tags`. Add more with the `tags` variable. |
| `variables.tf` | All environment-parameterized inputs. |
| `main.tf` | The `rosa2_search` module call. Also supplies the paths to the Lambda sources in `../rosa-search/lambda`. |
| `outputs.tf` | Values consumed by `rosa-search/init-opensearch.sh` and the viewer build, re-exported from the module. |
| `prod.tfvars` / `stage.tfvars` | Per-environment variable values. |
| `prod.s3.tfbackend` / `stage.s3.tfbackend` | Per-environment backend (state key) config. |

| `modules/rosa2-search/` file | Purpose |
|---|---|
| `versions.tf` | Required providers. The provider itself is configured by the root module. |
| `variables.tf` | Module inputs, with the validation rules. |
| `locals.tf` | Caller identity and derived values (domain name and ARN, S3 keys, route list). |
| `security-groups.tf` | Lambda and domain security groups. Domain ingress is limited to the Lambda group. Lambda egress is 443, plus 53 to `dns_server_cidrs`. |
| `opensearch.tf` | The domain, its access policy, and its CloudWatch log group. |
| `packages.tf` | Latin dictionary uploads, `TXT-DICTIONARY` packages, and domain associations. |
| `iam.tf` | Lambda roles and policies, plus an operator policy for running the ingest script. |
| `lambda.tf` | Zip packaging (into the root's `.build/`), log groups, and both Lambda functions. |
| `apigateway.tf` | HTTP API, routes, CORS, stage throttling, and access logs. |
| `vpc-endpoints.tf` | Optional S3 gateway endpoint, off by default. |
| `outputs.tf` | Resource attributes re-exported by the root `outputs.tf`. |

To add an input, declare it in both `variables.tf` files and pass it through in
`main.tf`. To add an output that `init-opensearch.sh` reads, add it to the
module's `outputs.tf` and re-export it under the same name from the root.

Environment-specific values live in the `*.tfvars` files. Because the S3 backend
block cannot reference variables, each environment also has its own
`*.s3.tfbackend` file selecting a distinct state key.

## Usage

Pick the environment, init the matching backend, then plan/apply with the
matching tfvars.

### prod

```sh
tofu init -backend-config=prod.s3.tfbackend
tofu plan  -var-file=prod.tfvars
tofu apply -var-file=prod.tfvars
```

### stage

```sh
# -reconfigure when switching backends in the same working copy
tofu init -reconfigure -backend-config=stage.s3.tfbackend
tofu plan  -var-file=stage.tfvars
tofu apply -var-file=stage.tfvars
```

Then create the indexes and load the data:

```sh
cd ../rosa-search
./init-opensearch.sh --env prod
```

The script reads this workspace's outputs, so the working copy must still be
initialized for the environment being loaded. It refuses to run if the
`environment` output does not match `--env`.

## Access control

Reaching the cluster requires clearing three independent layers. Widening one
does not widen the others.

| Layer | Control |
|---|---|
| Network | Domain has no public endpoint. Its security group admits only the Lambda security group (plus `vpc_cidr_blocks`, if set). The Lambda group can reach only 443 and, when `dns_server_cidrs` is set, DNS on 53 to those servers. The VPC uses JHU DNS servers, which security groups filter, so without `dns_server_cidrs` the Lambdas cannot resolve the domain endpoint (reported as `[Errno 16] Device or resource busy`). |
| Domain access policy | Names the two Lambda roles. The search role is granted `es:ESHttpPost` on exactly three `_search` paths. |
| Identity policy | The search role's own policy grants the same three paths and nothing else. |

`es:ESHttpPost` is also what `_bulk` and `_delete_by_query` require, so
restricting the *action* would not make the proxy read-only. The resource paths
are what do. Even with arbitrary code running in the search Lambda, its
credentials cannot write to, delete from, or read outside the two rosa indexes.

The HTTP API is deliberately unauthenticated — the production viewer is public
and anonymous. Abuse protection is the stage throttle
(`throttle_rate_limit`/`throttle_burst_limit`), which rejects excess requests
before any Lambda runs, plus the validation in the proxy.

### What the proxy rejects

`rosa-search/lambda/search_proxy/app.py` validates every request before signing
it. It requires `POST` to one of the three known paths and a body that parses as
a query rosa-viewer could have produced. It rejects, among other things:

- code execution (`script`, `script_score`, `script_fields`, `runtime_mappings`)
- unbounded clause types (`query_string`, `regexp`, `wildcard`, `fuzzy`, `more_like_this`)
- cross-index reads (`terms` lookups) and cross-document joins
- mapping disclosure (`profile`, `explain`, and upstream error bodies)
- oversized work: body > 16 KB, `size` > 100, `from + size` > 10000, aggregation
  buckets > 500, and caps on JSON depth, node count, and clause count

A bare `POST /_search` is rewritten to `/manifest,canvas/_search` so it cannot
reach the security plugin's own system indexes. Every forwarded query gets a
`timeout` and `terminate_after` injected as a server-side safety net.

Rejections are logged to the `search_log_group` output with the offending body.

## The Latin analyzer

The `manifest` and `canvas` indexes use a `stemmer_override` filter and a
`mapping` char_filter that read dictionary files from the OpenSearch config
directory. local-dev bind-mounts those files into the container; AWS has no
equivalent, so each file is uploaded to S3, imported as a `TXT-DICTIONARY`
package, and associated with the domain. The index definition then addresses it
as `analyzers/<package-id>` instead of by filename.

Package IDs are only known after apply, so the checked-in definitions in
`rosa-search/opensearch/` keep their local-dev paths and `init-opensearch.sh`
substitutes the IDs at deploy time using the `latin_stemmer_rules_path` and
`latin_charmap_path` outputs.

Two constraints worth knowing:

- The uploads use SSE-S3 (`AES256`). OpenSearch cannot read objects encrypted
  with a KMS key, and the failure only surfaces later as an unreadable package.
- `latin-stemmer-rules.txt` is 18 MB and consumes comparable heap on every index
  that loads it. `t3.small.search` (2 GiB, ~1 GiB heap) technically fits but was
  not enough in practice: query latency swung between tens of milliseconds warm
  and many seconds under light concurrency, which reached the viewer as 502, 503
  and 504 responses. Both environments now run `t3.medium.search` (4 GiB). Watch
  `JVMMemoryPressure` and `CPUCreditBalance`; T3 is burstable, so if credits
  trend to zero the next step is a non-burstable family, not a larger T3.

To update a dictionary, edit the file in `rosa-search/opensearch/` and re-apply.
The index analyzers are not declared `updateable`, so the indexes must then be
rebuilt with `init-opensearch.sh`.

## Notes

- **The domain is intentionally not publicly reachable**, which is why the ingest
  path goes through a Lambda instead of `curl`. `init-opensearch.sh` stages data
  in S3 and drives the admin Lambda with `aws lambda invoke`, so it needs no
  bastion host and no VPN. Ingest is resumable, so a corpus that takes longer
  than the 15-minute Lambda timeout still completes.
- **`create_s3_vpc_endpoint` defaults to false.** The admin Lambda needs a VPC
  route to S3, but `jhu-countertable-tf` already creates an S3 gateway endpoint
  in `vpc-07f503e3b0f8d5f8a`, and AWS permits only one per route table. Enable
  this only in a VPC where nothing else owns the endpoint.
- **`cors_allowed_origins` defaults to `["*"]`** in both tfvars files. That is
  not a data exposure risk, since the API is read-only over public content, but
  it does let other sites consume the domain's capacity. Narrow it to the viewer
  origin once the hostname is known.
- **Auto-Tune is explicitly disabled.** T3 instance types do not support it, and
  leaving it unset lets AWS enable it and the apply fails.
- **The search proxy fails fast and retries once.** `upstream_timeout` is 8 s,
  well below the 30 s Lambda timeout, because a request waiting on the domain
  holds a Lambda concurrency slot for the whole wait: with
  `search_lambda_reserved_concurrency` as a ceiling, a long timeout is how one
  slow query turns into throttling for every other caller. The proxy retries once
  on an unreachable domain or an upstream 502/503/504, and never retries a
  timeout, so a single request holds its slot for at most about 16 s. Tune with
  `upstream_timeout`, `upstream_attempts` and `upstream_backoff`.
- **`search_timeout` must stay below `upstream_timeout`** (6 s against 8 s). Both
  cap a slow search, but they end it differently. OpenSearch honouring
  `search_timeout` returns HTTP 200 with whatever the shards found and
  `timed_out: true`, which the viewer shows as a warning beside those partial
  results. The proxy's socket expiring first returns a 504 and nothing. Whichever
  value is smaller decides which of those the user sees.
- **Lambda throttling surfaces as HTTP 503,** not 429. When concurrent requests
  exceed `search_lambda_reserved_concurrency`, API Gateway answers
  `{"message":"Service Unavailable"}`. A 429 with `{"message":"Too Many
  Requests"}` is the stage throttle instead. Raising the reserved concurrency
  without first making the domain faster only moves the failure from Lambda
  throttling to OpenSearch timeouts.
- **`t3.small.search` and `t3.medium.search` are the only T3 sizes** available in
  OpenSearch Service. T3 also caps the domain at 10 instances and rules out
  UltraWarm and cold storage.
- Both Lambda functions are plain Python with no third-party dependencies.
  Signing uses `botocore`, which the runtime already bundles, so there is no
  build step, no layer, and no vendored wheels — the zip is the source directory.
- S3-native state locking is enabled via `use_lockfile = true` in the
  `*.s3.tfbackend` files (OpenTofu >= 1.10). No DynamoDB table required.
- The account ID is derived from the deploying identity
  (`aws_caller_identity`) rather than hardcoded.

## Cost

The domain dominates: one `t3.medium.search` on-demand plus a 20 GiB gp3 volume.
The HTTP API, both Lambdas, and CloudWatch Logs are usage-priced and negligible
at this traffic level. For current figures use the
[AWS Pricing Calculator](https://calculator.aws/).
