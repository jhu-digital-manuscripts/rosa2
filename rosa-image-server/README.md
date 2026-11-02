# Rosa image server

A Docker image that runs [Cantaloupe](https://cantaloupe-project.github.io/), the IIIF Image API server behind the Rosa viewer, built for deployment on Amazon ECS.

The image serves JPEG 2000 page images from an S3 bucket, decodes them with the commercially licensed Kakadu codec, encodes derivatives with libjpeg-turbo, and caches derivatives back into S3.

## What's in the image

| Component | Location | Notes |
|-----------|----------|-------|
| Cantaloupe | `/opt/cantaloupe/cantaloupe.jar` | Release jar, extracted from the GitHub release ZIP |
| Kakadu | `/opt/kakadu/lib`, `/opt/kakadu/bin` | Compiled from your licensed distribution; `kdu_*` tools are on the `PATH` |
| libjpeg-turbo | `/opt/libjpeg-turbo` | Built with `-DWITH_JAVA=1` so the TurboJPEG JNI binding works |
| Configuration | `/etc/cantaloupe/cantaloupe.properties` | Baked in; override individual keys with environment variables |
| Delegate script | `/etc/cantaloupe/delegates.rb` | Rewrites the `info.json` `@id` for legacy `/iiif/<id>` requests; see [Legacy `/iiif` URLs](#legacy-iiif-urls) |
| Base image | `amazoncorretto:25-al2023-jdk`, pinned by digest | Java 25 |

The server listens on port **8182** and runs as the non-root `cantaloupe` system user. A health check endpoint is available at `/health`, and the IIIF Image API 2 and 3 endpoints at `/iiif/2` and `/iiif/3`.

## Prerequisites

- Docker or Podman
- A **commercial Kakadu license**. The Kakadu binaries bundled with Cantaloupe releases are built under a Kakadu Evaluation License that forbids commercial use, so this image discards them and compiles your licensed distribution instead.
- The Kakadu distribution ZIP, anywhere on disk. It is passed to the build as a named build context, so it never needs to be copied into the repository. (Copying it into this directory also still works; `.gitignore` excludes `*.zip` so the licensed source is never committed.)

## Building

```sh
./build-and-push.sh --kakadu-zip /home/msp/work/kakadu/v8_7-02130L.zip
```

This builds `rosa-image-server:<cantaloupe-version>-<git-sha>` (for example `rosa-image-server:5.0.8-c1caaa2d`) for `linux/amd64`. It refuses to build from uncommitted changes in this directory, so that a tag always identifies a commit; pass `--allow-dirty` while developing, and the tag then ends in `-dirty`. `--tag` only renames the image and does not lift that check. Do not deploy a `-dirty` tag: it cannot be traced back to a commit. Run `./build-and-push.sh --help` for every option. Docker and Podman both work (`CONTAINER_CLI=podman`).

The build compiles Kakadu and libjpeg-turbo from source and downloads the Cantaloupe release, so expect roughly 2–3 minutes on a cold cache and outbound network access to GitHub. Both downloads are verified against the SHA-256 values recorded in the Dockerfile, and the base image is pinned by digest, so a rebuild of the same commit produces the same inputs.

The script runs the equivalent of:

```sh
docker build --platform linux/amd64 -t rosa-image-server:5.0.8-<git-sha> \
  --build-context kakadu_src=/dir/containing/the/zip \
  --build-arg KAKADU_ZIP=v8_7-02130L.zip \
  --build-arg KAKADU_PLATFORM=Linux-x86-64-gcc \
  --build-arg CANTALOUPE_VERSION=5.0.8 .
```

`.dockerignore` keeps ZIPs, an unpacked Kakadu tree and the tests out of the build context.

`--build-context kakadu_src=<dir>` replaces the Dockerfile's `kakadu_src` stage, which otherwise reads the ZIP from this directory. The script stages the ZIP alone in a temporary directory (by hard link where possible), so nothing else beside the ZIP enters the build.

### Pushing to AWS

```sh
aws sso login --profile <profile> && export AWS_PROFILE=<profile>
./build-and-push.sh --kakadu-zip /home/msp/work/kakadu/v8_7-02130L.zip --env stage --push
```

This pushes to the environment's ECR repository, `<account>.dkr.ecr.us-east-1.amazonaws.com/<name_prefix>-image-server`, which [jhu-rosa2-tf](../jhu-rosa2-tf/README.md#image-server) creates. Tags are immutable there, so the script checks first and fails if the tag already exists. The push does not deploy anything: it prints the `image_server_image_tag` line to put into `jhu-rosa2-tf/<env>.tfvars`, and `tofu apply` rolls the service onto it. `--dry-run` shows the commands without running them.

### Build arguments

| Argument | Default | Description |
|----------|---------|-------------|
| `KAKADU_ZIP` | `v8_7-02130L.zip` | Kakadu distribution ZIP, as a path relative to this directory. The archive's single top-level directory is detected automatically, so the name does not matter, but the Kakadu *version* does: it must be the one whose JNI bindings the Cantaloupe release bundles (8.7 for 5.0.8). See [Upgrading Cantaloupe or Kakadu](#upgrading-cantaloupe-or-kakadu). |
| `KAKADU_PLATFORM` | `Linux-x86-64-gcc` | Which Kakadu makefile to use. `build-and-push.sh` derives it from `--platform` (`linux/arm64` gives `Linux-arm-64-gcc`, for a Graviton image built on or emulating that architecture) or takes `--kakadu-platform`. The ECS task definition is `X86_64`, so an arm64 image also needs `ecs.tf` changed. |
| `CANTALOUPE_VERSION` | `5.0.8` | Release to install, set once at the top of the Dockerfile; `build-and-push.sh` reads that line for its default tag. The build downloads `cantaloupe-<VERSION>.zip` from the `v<VERSION>` GitHub release tag, checks it against `CANTALOUPE_SHA256`, and keeps only `cantaloupe-<VERSION>.jar`. Changing the version means changing the checksum too. |
| `CANTALOUPE_SHA256` | checksum of the 5.0.8 ZIP | SHA-256 the downloaded release ZIP must match. |
| `LIBJPEG_TURBO_VERSION` | `2.0.2` | The version the Cantaloupe manual specifies. Other 2.0.x releases may work but are untested here. **Do not use 3.x**: its TurboJPEG Java API differs from the classes vendored in the Cantaloupe jar. |
| `LIBJPEG_TURBO_SHA256` | checksum of the 2.0.2 tag archive | SHA-256 the downloaded source tarball must match. |
| `JAVA_IMAGE` | `amazoncorretto:25-al2023-jdk@sha256:…` | Base image for every stage, pinned to the tag's multi-arch index digest; the Dockerfile comment shows how to look up a newer digest. Must be a JDK, not a JRE: the Kakadu JNI binding compiles against `$JAVA_HOME/include/jni.h`. |

The build fails fast rather than producing a subtly broken image: it asserts that `libkdu_jni.so` and `kdu_expand` were produced, that `libturbojpeg.so` exports its JNI symbols, that the `kdu_*` tools resolve their shared libraries, and that the jar's `logback.xml` was replaced (see stage 3 below).

### Build stages

The build is multi-stage so that no compiler, no source tree, and in particular no Kakadu source reaches the published image:

0. **kakadu_src** — the Kakadu ZIP, from this directory unless `--build-context kakadu_src=<dir>` overrides it.
1. **kakadu** — unpacks the ZIP and runs `make -f Makefile-Linux-x86-64-gcc`, producing the JNI binding, the shared libraries, and the command-line tools. The makefiles compile with `-O2 -msse2` and gate the SSSE3/SSE4/AVX/AVX2 code paths behind runtime CPUID checks, so the result runs on any x86-64 host.
2. **turbojpeg** — builds libjpeg-turbo with Java support. Distribution packages omit it, and without it Cantaloupe silently falls back to the slower Image I/O JPEG writer.
3. **cantaloupe** — downloads the release ZIP, extracts the jar, and replaces the jar's `logback.xml` with the `logback.xml` in this directory. The 5.0.8 release ships `<if condition="...">` blocks in the Janino syntax that logback 1.5.37 removed; with the logback 1.6.3 bundled in 5.0.8 every condition is false, no appender is attached, and the server runs without logging anything ([cantaloupe#1007](https://github.com/cantaloupe-project/cantaloupe/issues/1007)). The replacement is the upstream fix ([PR #1008](https://github.com/cantaloupe-project/cantaloupe/pull/1008), merged for 5.0.9), identical apart from a header comment. It has to go into the jar because Cantaloupe's `LoggerUtil` reloads the classpath resource and ignores `-Dlogback.configurationFile`. The step is guarded by the version, so bumping `CANTALOUPE_VERSION` fails the build until the patch is re-evaluated or removed.
4. **runtime** — assembles the above, plus the configuration file, the delegate script, and a non-root user.

## Testing

```sh
test/smoke-test.sh                       # against the tag build-and-push.sh gives the current checkout
test/smoke-test.sh --image rosa-image-server:5.0.8-c1caaa2d --s3 --benchmark
```

No AWS access is needed. The script makes JPEG 2000 test images with the image's own `kdu_compress`, serves them with `FilesystemSource` under the identifiers `rose/Test/Test.001r` and `rose/Test/cropped/Test.001r` (the form rosa-tool uses for books with cropped images), and prints PASS/FAIL for each check:

- `/health`, Image API 2 and 3 `info.json` (`@id`/`id` built from `BASE_URI`), the base-URI 303 redirect, a 512 px tile, the 80 px thumbnail the viewer's search results use, v3 `full/max` (200, full size) and v3 `full/full` (400: Image API 3 has no `full` size, which is why rosa-tool emits `max`)
- `%2F`-encoded identifiers, the cropped identifier, the CORS header, `Cache-Control` with `max-age` and `s-maxage`, and `no-cache` on 404s
- the legacy `@id` rewrite with and without the marker header (see below), and that the v3 endpoint and image requests ignore it
- `max_pixels`, on a second server started with `MAX_PIXELS` below the test image's area: `info.json` advertises it as `maxArea`, a v2 `full/full` is refused with 403, and a v3 `full/max` is scaled down to fit
- that `kdu_expand -v` runs (and which Kakadu version it reports), that the application log is written at all (`Starting Cantaloupe <version>`: 5.0.8 as released logs nothing, see the build stages above), and that `KakaduNativeProcessor` decodes and `TurboJPEGImageWriter` encodes, with no native library errors

`--s3` additionally starts a SeaweedFS container (`docker.io/chrislusf/seaweedfs`, override with `S3_IMAGE`) on a private network, uploads the test image to it, and runs a third server with `S3Source` and `S3Cache` pointed at it: `info.json` and a tile are served from the bucket, a missing object gives 404, the derivative and info appear in the cache bucket, the repeated tile is a cache hit, and the AWS SDK's native CRT HTTP library (new in 5.0.8, which switched to the asynchronous S3 client) loads. The default run stays on `FilesystemSource` so that it needs no second image.

`--benchmark` compares `info.json` latency with the delegate script on and off. The difference is within noise, at about 2–3 ms median for each. `--keep` leaves the server running for the CloudFront Function end-to-end test in `jhu-rosa2-tf/modules/rosa2-image-server/functions/test/`.

### Benchmarking

`test/bench.py` (python3 and `httpx`) replays the request patterns rosa-viewer produces against any Image API base URL, a local container or the CloudFront URL of an environment, and prints median, p95 and throughput per scenario plus CloudFront hit/miss counts:

| scenario | requests | what in the viewer |
|----------|----------|--------------------|
| `info` | `info.json` | every page open, every thumbnail with a service |
| `thumbs` | `full/80,` | search results, the Mirador gallery view and thumbnail strip (the manifests give every canvas a static 80 px thumbnail without an image service, so Mirador never asks for another size) |
| `open` | one scale-factor-8 tile and the scale-factor-4 grid | OpenSeadragon showing a whole page in a window |
| `zoom` | a 3×2 block of full-resolution 512 px tiles | a zoomed-in viewport |
| `fullmax` | `full/max` | the painting body URL; the viewer does not request it, but crawlers and downloads do |

`--unique` shifts every region by the round number so neither CloudFront nor the derivative cache can answer, which measures decoding; without it repeated rounds measure the caches. Example, against stage with the three pages used below:

```sh
test/bench.py --base https://<id>.cloudfront.net/iiif/3 --unique --rounds 3 --concurrency 4 \
  --ids aor/FolgersHa2/FolgersHa2.012r rose/Douce195/Douce195.001r aor/PrincetonPA6452/PrincetonPA6452.001r
```

Locally, run the image with Fargate-like limits (`--cpus=2 --memory=4g`, `HTTP_MAX_THREADS=64`, `JAVA_OPTS=-XX:MaxRAMPercentage=65.0`) over a directory of JP2s. Real pages work best: fetch `full/max` JPEGs from the stage server and encode them with the image's own `kdu_compress` (`Clevels=5 Corder=RPCL Cprecincts={256,256},{256,256},{128,128} Cblk={64,64} ORGgen_plt=yes -rate 1.0` gives 2–4 MB files like the archive's).

**Results for the 5.0.8 upgrade (2026-11-02, 5.0.7 + Kakadu 8.6.1 vs 5.0.8 + Kakadu 8.7, same host, 2 CPUs, 4 concurrent requests, unique regions, medians of repeated passes):**

| source | version | thumbs | open tile | zoom tile | CPU for the same work |
|--------|---------|--------|-----------|-----------|-----------------------|
| local files | 5.0.7 | 8 ms | 111–135 ms | 100–103 ms | 40.5–41.8 s |
| local files | 5.0.8 | 7 ms | 121–124 ms | 102 ms | 41.5–42.1 s |
| local S3 (SeaweedFS) | 5.0.7 | 12–15 ms | 184–186 ms | 111–113 ms | 23.6–23.8 s |
| local S3 (SeaweedFS) | 5.0.8 | 14–17 ms | 155–181 ms | 114–145 ms | 23.5–24.0 s |

Decoding and encoding are unchanged: Kakadu 8.7 is neither faster nor slower than 8.6.1 here, and the two Cantaloupe versions burn the same CPU per tile. 5.0.8's fix for the leaked `KduImageInputStreamSource` finalizers shows up as a lower steady-state heap (about 0.85 GB against 1.8 GB resident after the same file-source run). The stage task (Fargate 2 vCPU, S3 source, CloudFront in the path) served the same cold requests at 207 ms (thumbnail), 290–310 ms (tile) and 3.9 s (`full/max` at the then-unlimited `max_pixels`) before the upgrade; the difference to the local numbers is S3 round trips, the slower vCPU and the CloudFront hop, not Cantaloupe.

Tuning experiments on 5.0.8 found nothing worth changing: `http.max_threads` 8 or 16 instead of 64 did not improve p95 under bursts of 16 or 32 concurrent tiles (throughput is bound by the two CPUs at about 24 `open` and 33 `zoom` tiles a second either way); `-XX:+UseParallelGC` was no better than the default G1; `processor.downscale_filter = triangle` was no faster than `bicubic`, because Kakadu does the bulk of the reduction and the filter only handles the residual scale. `S3Source.chunking.chunk_size` of 512K, 1M, 2M and 4M and chunking off all measured the same with 20 ms of emulated S3 delay; a 512 px tile reads about 5 chunks of a 2.6 MB file, so if the real S3 round trip dominates on Fargate, a larger chunk can only be judged there: set `S3SOURCE_CHUNKING_CHUNK_SIZE` (and `S3SOURCE_CHUNKING_CACHE_MAX_SIZE`) through `image_server_extra_environment` and compare with `bench.py --unique`.

**5.0.8 S3 client caching bug.** `S3Source.getClientInstance` caches clients under the configured `S3Source.endpoint` string but looks them up by the object's own endpoint, which `BasicLookupStrategy` never sets. With `S3Source.endpoint` set (MinIO, SeaweedFS, any non-AWS endpoint) every S3 call therefore builds a new `S3AsyncClient`, and in 5.0.8 each one brings a native AWS CRT HTTP client that is never closed: in the benchmark this doubled tile latency, added 50% CPU and killed the 4 GB container with an OOM after about 3,000 S3 calls, where 5.0.7's cheap synchronous clients were merely garbage. With an empty endpoint (AWS, as in ECS) the key is `null` on both sides, one client is reused, and the two versions are identical, as the local-S3 row above shows when `ScriptLookupStrategy` supplies the endpoint. Keep `S3Source.endpoint` empty in AWS; for a custom endpoint use the delegate `s3source_object_info` and return the endpoint from it. `test/smoke-test.sh --s3` runs the buggy path, which is harmless for its few dozen requests.

## Running locally

Against the real source images in S3, with the derivative cache off so that nothing is written:

```sh
aws sso login --profile <profile>
export AWS_PROFILE=<profile>
eval "$(aws configure export-credentials --format env)"

docker run --rm -p 8182:8182 \
  -e S3SOURCE_REGION=us-east-1 \
  -e S3SOURCE_BASICLOOKUPSTRATEGY_BUCKET_NAME=jhu-library-images \
  -e S3SOURCE_BASICLOOKUPSTRATEGY_PATH_PREFIX=iiifstore/ \
  -e CACHE_SERVER_DERIVATIVE_ENABLED=false \
  -e AWS_ACCESS_KEY_ID -e AWS_SECRET_ACCESS_KEY -e AWS_SESSION_TOKEN \
  rosa-image-server:5.0.8-c1caaa2d
```

Credentials come from the default AWS SDK chain: exported keys (as here) or a mounted `~/.aws` locally, and the task role in ECS.

To serve images from a local directory instead, bypassing S3 entirely:

```sh
docker run --rm -p 8182:8182 -v /path/to/jp2s:/images:ro \
  -e SOURCE_STATIC=FilesystemSource \
  -e FILESYSTEMSOURCE_BASICLOOKUPSTRATEGY_PATH_PREFIX=/images/ \
  -e FILESYSTEMSOURCE_BASICLOOKUPSTRATEGY_PATH_SUFFIX=.jp2 \
  -e CACHE_SERVER_DERIVATIVE_ENABLED=false \
  rosa-image-server:5.0.8-c1caaa2d
```

Then:

```sh
curl http://localhost:8182/health
curl http://localhost:8182/iiif/2/aor%2FBL531k6%2FBL531k6.001r/info.json
curl -H 'X-Rosa-Legacy-Path: 1' http://localhost:8182/iiif/2/aor%2FBL531k6%2FBL531k6.001r/info.json   # legacy @id
curl -o tile.jpg "http://localhost:8182/iiif/2/aor%2FBL531k6%2FBL531k6.001r/2000,2000,512,512/full/0/default.jpg"
```

Note that identifiers contain URL-encoded slashes (`%2F`). Any proxy in front of the server must pass them through without decoding.

## Environment variables

### Used by the entrypoint

| Variable | Default | Description |
|----------|---------|-------------|
| `JAVA_OPTS` | empty | Extra JVM arguments, appended after the fixed ones, so a repeated `-XX:` flag here wins. Use this for heap or GC tuning, e.g. `-XX:MaxRAMPercentage=65.0` (what jhu-rosa2-tf passes by default, see below) or `-XX:+UseZGC`. |
| `CANTALOUPE_CONFIG` | `/etc/cantaloupe/cantaloupe.properties` | Path passed as `-Dcantaloupe.config`. Change it if you mount a replacement configuration file. |
| `JAVA_LIBRARY_PATH` | `/opt/kakadu/lib:/opt/libjpeg-turbo/lib64:` plus the standard JVM entries | Passed as `-Djava.library.path`, which is how `System.loadLibrary()` finds Kakadu and TurboJPEG. Because this property replaces the default rather than extending it, the standard system entries are included explicitly; preserve them if you override this. |
| `CANTALOUPE_VERSION` | build-arg value | Informational only. |

The entrypoint always applies these:

- `-XX:MaxRAMPercentage=75.0` — the JVM otherwise picks about 25% of the container memory limit, which the Cantaloupe manual calls out as too conservative. In AWS the module's `java_opts` default, `-XX:MaxRAMPercentage=65.0`, comes later on the command line and overrides it, leaving room for the Kakadu and TurboJPEG native buffers outside the heap. Size the heap by setting the ECS task memory and this percentage rather than by passing `-Xmx`.
- `--enable-native-access=ALL-UNNAMED` — Kakadu and TurboJPEG load through `System.loadLibrary()`, which JDK 24+ reports as a restricted call and will block outright in a future release.
- `-Djava.awt.headless=true`

### Used by Cantaloupe

**Every** key in `cantaloupe.properties` can be overridden by an environment variable, which is the intended way to supply deployment-specific values in an ECS task definition. Environment variables take precedence over the file.

The variable name is the key uppercased with every non-alphanumeric character replaced by an underscore:

| Configuration key | Environment variable |
|-------------------|----------------------|
| `S3Source.region` | `S3SOURCE_REGION` |
| `S3Source.BasicLookupStrategy.bucket.name` | `S3SOURCE_BASICLOOKUPSTRATEGY_BUCKET_NAME` |
| `S3Source.BasicLookupStrategy.path_prefix` | `S3SOURCE_BASICLOOKUPSTRATEGY_PATH_PREFIX` |
| `S3Cache.region` | `S3CACHE_REGION` |
| `S3Cache.bucket.name` | `S3CACHE_BUCKET_NAME` |
| `log.application.level` | `LOG_APPLICATION_LEVEL` |
| `http.max_threads` | `HTTP_MAX_THREADS` |

At minimum, a deployment must set the bucket names and regions; those are intentionally left blank in the file.

In AWS, [jhu-rosa2-tf](../jhu-rosa2-tf/README.md#image-server) sets these in the ECS task definition:

| Variable | Value |
|----------|-------|
| `SOURCE_STATIC`, `S3SOURCE_REGION`, `S3SOURCE_BASICLOOKUPSTRATEGY_BUCKET_NAME`, `S3SOURCE_BASICLOOKUPSTRATEGY_PATH_PREFIX` | `S3Source`, the region, `image_server_source_bucket`, `image_server_source_prefix` |
| `CACHE_SERVER_DERIVATIVE_ENABLED`, `CACHE_SERVER_DERIVATIVE`, `S3CACHE_REGION`, `S3CACHE_BUCKET_NAME`, `S3CACHE_OBJECT_KEY_PREFIX` | `true`, `S3Cache`, the region, the environment's cache bucket, `<environment>/` |
| `CACHE_SERVER_DERIVATIVE_TTL_SECONDS` | `image_server_cache_ttl_days` × 86400; the same value sets the bucket's lifecycle expiry |
| `BASE_URI` | `https://<first domain alias, or the CloudFront hostname>` |
| `HTTP_MAX_THREADS`, `HTTP_ACCEPT_QUEUE_LIMIT`, `LOG_APPLICATION_LEVEL`, `JAVA_OPTS` | from the matching `image_server_*` variables |

Any other key can be set per environment through `image_server_extra_environment`. Everything else (processors, endpoints, limits, client cache headers, logging appenders) is baked into the image from `cantaloupe.properties`.

## Configuring cantaloupe.properties

`cantaloupe.properties` is a copy of the `cantaloupe.properties.sample` shipped with Cantaloupe 5.0.8, so every key the application knows about is present with its upstream comments. Values changed for this deployment are marked with a `ROSA:` comment explaining the reasoning — search for `ROSA:` to review all of them.

Edit the file and rebuild the image for changes that apply to every environment; use environment variables for anything that differs between environments.

### Key decisions encoded in the file

**Source.** `source.static = S3Source` with `BasicLookupStrategy`. Identifiers in the generated manifests carry no file extension (`aor/BL531k6/BL531k6.001r`) while the objects do, so `path_suffix = .jp2` appends it; this also lets the source infer the format from the key instead of issuing an extra ranged GET. Chunking stays enabled. Each request reads the object in its own 512 KB ranged GETs; for these 1–3 MB files that is usually most of the file, because the chunk cache lasts only one request. A local source cache (chunking off, `processor.stream_retrieval_strategy = CacheStrategy`, so each JP2 is downloaded once to task storage) was tried on stage and reverted: 16-tile page opens were 15–20% faster, but isolated cold requests such as thumbnails and first tiles got slower (about 230 ms to 390 ms), because the whole file is downloaded first, and Cantaloupe has no safe way to bound the cache's disk use (see the cache worker below). Cold tiles are bound by Kakadu decode CPU, about 130 ms of CPU per 512 px tile on a 2 vCPU Fargate task.

**Processor.** `ManualSelectionStrategy` maps `jp2` to `KakaduNativeProcessor` explicitly, so a missing or broken Kakadu library fails the request instead of silently falling back to a slower processor. JPEG output goes through libjpeg-turbo for every processor when the library is present. `processor.jpg.progressive = false`: on a 1-CPU task, baseline encoding served a burst of 73 distinct tiles about 9% faster than progressive, for about 14% more bytes per tile. OpenSeadragon draws a tile only once it has fully loaded, so progressive rendering buys the viewer nothing. Output stays at `processor.jpg.quality = 80`.

**Caching.** There are three tiers:

1. Browsers and CloudFront. Successful responses carry `Cache-Control: max-age=2592000, s-maxage=31536000, public, no-transform`, so browsers keep them for 30 days and CloudFront for up to a year. Error responses are `no-cache`.
2. Cantaloupe's derivative cache, `S3Cache`, in a per-environment bucket under `<environment>/`. ECS task storage is ephemeral and unshared, and CloudFront's edge caches fill independently, so this shared tier keeps edge misses off the decoder.
3. The heap-based info cache, as a level-1 cache in each task.

`resolve_first = false` skips an S3 existence check on every cache hit, which is safe because source objects are static. If an object is replaced without its identifier changing, purge its derivatives from the cache bucket and create a CloudFront invalidation.

The cache worker is **disabled**: the manual advises running it on only one node in a cluster, which ECS cannot express cleanly. It is also broken with `S3Cache` in 5.0.7 and 5.0.8: the purge (`S3Utils.walkObjects`) pages with ListObjects v1 using `nextMarker`, which S3 returns only when a delimiter is set, so once the prefix holds more than 1000 objects it re-lists the first page forever (on stage, about 5 LIST requests a second and a fifth of a vCPU). The cache bucket's lifecycle rule expires the prefix instead, after the same TTL Cantaloupe is given.

**Endpoints.** IIIF Image API 2 and 3 are enabled, API 1 is not. The Control Panel (`/admin`) and the administrative HTTP API are disabled; if you enable either, supply a secret from Secrets Manager and keep the paths off the public load balancer.

**Limits.** `max_pixels = 10000000`, which caps any single derivative below a full-size rendering of the archive's roughly 15–21 MP page images (for example 3661×5680). The limit does not apply to full-sized *unmodified* images, but a JP2 delivered as JPEG counts as modified, so an Image API 2 `full/full` of a page larger than 10 MP returns 403. Image API 3 `max` is instead scaled down to fit, and `info.json` advertises the limit as `maxArea`; this is why rosa-tool writes `full/max` into every painting body. It was lowered from 50 MP to bound peak heap per request on a 4 GiB task; `test/smoke-test.sh` exercises both behaviours. `http.max_threads` keeps concurrent decodes from exhausting the heap before the request queue pushes back; in AWS the task size and thread count are set per environment by `image_server_task_cpu`, `image_server_task_memory` and `image_server_http_max_threads` in `jhu-rosa2-tf/<env>.tfvars` (the module default is 32 threads).

**Delegate script.** Enabled, for one method only; see below. Every other method is the upstream pass-through stub.

**Error responses.** Cantaloupe's error bodies are plain text and include the message of the underlying exception. For a missing object that message names the S3 bucket and key, and `print_stack_trace_on_error_pages = false` only suppresses the stack trace, not the message; Cantaloupe 5 has no setting to replace the body. Identifiers with `.` or `..` path segments, which would make S3 reject the key and surface a 500 with the SDK's message, are refused at the edge by the legacy viewer-request function; the `/iiif/2` and `/iiif/3` behaviours run no function and still pass them to the origin (see the tf README).

**Logging.** Console only, at `info`, for the ECS log driver to forward to CloudWatch. (This depends on the `logback.xml` patch described under [Build stages](#build-stages); without it 5.0.8 logs nothing.) All file appenders are off, since task storage is ephemeral. Set `LOG_APPLICATION_LEVEL=debug` temporarily to see which processor and image writer handled each request. The access log is off to limit CloudWatch volume; enable it with `LOG_ACCESS_CONSOLEAPPENDER_ENABLED=true`.

### Reverse proxy

Behind a proxy, Cantaloupe has to be told its public URL, because it builds the `info.json` `@id` and the `Location` and `Link` headers from it. In AWS this is `BASE_URI`, set by OpenTofu to the CloudFront URL. The `X-Forwarded-*` headers are no use there: CloudFront talks to the internal ALB over HTTP, and the ALB overwrites `X-Forwarded-Proto` with `http`, which would put `http://` URLs in `info.json`.

Whatever is in front must pass `%2F` in identifiers through undecoded. CloudFront and the ALB do.

### Legacy `/iiif` URLs

The current image server answers Image API 2 at `/iiif/<id>/...` (Apache rewrites the path to `/iiif/2` and the response body back). Existing sites and manifests use that form, so the new server keeps it working alongside `/iiif/2` and `/iiif/3`.

Cantaloupe always builds Image API 2 URLs as `<base_uri>/iiif/2/<id>`, and the `X-Forwarded-*` headers and `base_uri` change only the part before `/iiif/2`. So the work is split three ways:

| Piece | Does |
|-------|------|
| CloudFront viewer-request function (`jhu-rosa2-tf/modules/rosa2-image-server/functions/legacy-request.js`) | `/iiif/<id>` → 303 to `/iiif/<id>/info.json`; `/iiif/<id>/...` → `/iiif/2/<id>/...`; on `info.json` only, adds `X-Rosa-Legacy-Path: 1` (part of the CloudFront cache key); identifiers with a `.` or `..` path segment get a 400 |
| `delegates.rb`, `extra_iiif2_information_response_keys` | When the header is present, returns `@id` = `<base_uri>/iiif/<id>`. Cantaloupe merges these keys after setting its own `@id`, so this replaces it. |
| CloudFront viewer-response function (`legacy-response.js`) | Rewrites `/iiif/2/` to `/iiif/` in `Location` and `Link` (the delegate cannot reach headers) |

Only `info.json` differs between the two forms, so tiles requested either way share one CloudFront cache entry. The marker header is stripped from client requests at the edge and is not forwarded at all on `/iiif/2` and `/iiif/3`, so it cannot be used to change cached responses for the new paths. Without the header, the delegate changes nothing, so local runs and `/iiif/2` behave exactly as stock Cantaloupe.

## Upgrading Cantaloupe or Kakadu

The two versions are coupled. The Cantaloupe jar bundles the `kdu_jni` Java classes that Kakadu's `kdu_hyperdoc` generates from one specific Kakadu release, and the `libkdu_jni.so` built in stage 1 must implement exactly those native methods. 5.0.7 bundled the 8.4 bindings, 5.0.8 bundles 8.7, and several native signatures changed in between (for example `Kdu_block_decoder.Decode` gained a `boolean` parameter and a return value, and `Kdu_global.Kdu_core_sample_alignment_checker` an argument). Because JNI resolves non-overloaded methods by name alone, a mismatch is not a clean `UnsatisfiedLinkError` but undefined behaviour at decode time. When either version moves:

1. Read the release notes and diff the upstream tags; `git diff v5.0.7 v5.0.8 -- src/main/java/kdu_jni` shows whether the bindings changed, and `dist/deps/README.md` which Kakadu release built the bundled binaries.
2. Build the Kakadu stage on its own and compare the Java sources it generates with the ones in the Cantaloupe source tree at the release tag. They should be identical:

   ```sh
   docker build --platform linux/amd64 --target kakadu -t rosa-kakadu-check \
     --build-context kakadu_src=/dir/containing/the/zip --build-arg KAKADU_ZIP=v8_7-02130L.zip .
   docker run --rm -v "$PWD/kdu_jni-generated:/out:z" rosa-kakadu-check sh -c 'cp /build/java/kdu_jni/*.java /out/'
   git -C /path/to/cantaloupe-clone diff --no-index --stat src/main/java/kdu_jni "$PWD/kdu_jni-generated"
   ```

3. Update `CANTALOUPE_VERSION`, `CANTALOUPE_SHA256` (`sha256sum` of the release ZIP) and `KAKADU_ZIP` in the Dockerfile, the examples in `build-and-push.sh`, this README and `docs/deployment.md`, the headers of `cantaloupe.properties` and `delegates.rb`, and diff the new `cantaloupe.properties.sample` and `delegates.rb.sample` against ours for new or removed keys and methods.
4. Re-evaluate the `logback.xml` patch (the build fails on a new version until you do), then build and run `test/smoke-test.sh --s3`.

## IAM

The ECS task role (created by jhu-rosa2-tf) needs:

- `s3:GetObject` on the source keys, and `s3:ListBucket` limited to the source prefix, so that a missing image is a 404 rather than a 403
- `s3:GetObject`, `s3:PutObject` and `s3:DeleteObject` on the environment's cache prefix, and `s3:ListBucket` limited to it

## Reference

- [Cantaloupe 5.0 manual](https://cantaloupe-project.github.io/manual/5.0/)
- [Deployment & tuning](https://cantaloupe-project.github.io/manual/5.0/deployment.html)
- [Caching](https://cantaloupe-project.github.io/manual/5.0/caching.html)
- [Rosa deployment notes](../docs/deployment.md)
