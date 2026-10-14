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
| Base image | `amazoncorretto:25-al2023-jdk` | Java 25 |

The server listens on port **8182** and runs as the non-root `cantaloupe` user (uid 999). A health check endpoint is available at `/health`, and the IIIF Image API 2 and 3 endpoints at `/iiif/2` and `/iiif/3`.

## Prerequisites

- Docker or Podman
- A **commercial Kakadu license**. The Kakadu binaries bundled with Cantaloupe releases are built under a Public Service License that forbids commercial use, so this image discards them and compiles your licensed distribution instead.
- The Kakadu distribution ZIP, anywhere on disk. It is passed to the build as a named build context, so it never needs to be copied into the repository. (Copying it into this directory also still works; `.gitignore` excludes `*.zip` so the licensed source is never committed.)

## Building

```sh
./build-and-push.sh --kakadu-zip /home/msp/work/kakadu/v8_6_1-02130L.zip
```

This builds `rosa-image-server:<cantaloupe-version>-<git-sha>` (for example `rosa-image-server:5.0.7-c1caaa2d`) for `linux/amd64`. It refuses to build from uncommitted changes in this directory, so that a tag always identifies a commit; pass `--allow-dirty` (the tag then ends in `-dirty`) or `--tag` while developing. Run `./build-and-push.sh --help` for every option. Docker and Podman both work (`CONTAINER_CLI=podman`).

The build compiles Kakadu and libjpeg-turbo from source and downloads the Cantaloupe release, so expect roughly 2–3 minutes on a cold cache and outbound network access to GitHub.

The script runs the equivalent of:

```sh
docker build --platform linux/amd64 -t rosa-image-server:5.0.7 \
  --build-context kakadu_src=/dir/containing/the/zip \
  --build-arg KAKADU_ZIP=v8_6_1-02130L.zip \
  --build-arg CANTALOUPE_VERSION=5.0.7 .
```

`--build-context kakadu_src=<dir>` replaces the Dockerfile's `kakadu_src` stage, which otherwise reads the ZIP from this directory. The script stages the ZIP alone in a temporary directory (by hard link where possible), so nothing else beside the ZIP enters the build.

### Pushing to AWS

```sh
aws sso login --profile <profile> && export AWS_PROFILE=<profile>
./build-and-push.sh --kakadu-zip /home/msp/work/kakadu/v8_6_1-02130L.zip --env stage --push
```

This pushes to the environment's ECR repository, `<account>.dkr.ecr.us-east-1.amazonaws.com/<name_prefix>-image-server`, which [jhu-rosa2-tf](../jhu-rosa2-tf/README.md#image-server) creates. Tags are immutable there, so the script checks first and fails if the tag already exists. The push does not deploy anything: it prints the `image_server_image_tag` line to put into `jhu-rosa2-tf/<env>.tfvars`, and `tofu apply` rolls the service onto it. `--dry-run` shows the commands without running them.

### Build arguments

| Argument | Default | Description |
|----------|---------|-------------|
| `KAKADU_ZIP` | `v8_6_1-02130L.zip` | Kakadu distribution ZIP, as a path relative to this directory. The archive's single top-level directory is detected automatically, so the version-specific name does not matter. |
| `KAKADU_PLATFORM` | `Linux-x86-64-gcc` | Which Kakadu makefile to use. Change to `Linux-arm-64-gcc` for an arm64 (Graviton) image, and build on or emulate that architecture. |
| `CANTALOUPE_VERSION` | `5.0.7` | Release to install. The build downloads `cantaloupe-<VERSION>.zip` from the `v<VERSION>` GitHub release tag and keeps only `cantaloupe-<VERSION>.jar`. |
| `LIBJPEG_TURBO_VERSION` | `2.0.2` | The version the Cantaloupe manual specifies. Other 2.0.x releases may work. **Do not use 3.x**: its TurboJPEG Java API differs from the classes vendored in the Cantaloupe jar. |
| `JAVA_IMAGE` | `amazoncorretto:25-al2023-jdk` | Base image for every stage. Must be a JDK, not a JRE: the Kakadu JNI binding compiles against `$JAVA_HOME/include/jni.h`. |

The build fails fast rather than producing a subtly broken image: it asserts that `libkdu_jni.so` and `kdu_expand` were produced, that `libturbojpeg.so` exports its JNI symbols, and that the `kdu_*` tools resolve their shared libraries.

### Build stages

The build is multi-stage so that no compiler, no source tree, and in particular no Kakadu source reaches the published image:

0. **kakadu_src** — the Kakadu ZIP, from this directory unless `--build-context kakadu_src=<dir>` overrides it.
1. **kakadu** — unpacks the ZIP and runs `make -f Makefile-Linux-x86-64-gcc`, producing the JNI binding, the shared libraries, and the command-line tools. The makefiles compile with `-O2 -msse2` and gate the SSSE3/SSE4/AVX/AVX2 code paths behind runtime CPUID checks, so the result runs on any x86-64 host.
2. **turbojpeg** — builds libjpeg-turbo with Java support. Distribution packages omit it, and without it Cantaloupe silently falls back to the slower Image I/O JPEG writer.
3. **cantaloupe** — downloads the release ZIP and extracts the jar.
4. **runtime** — assembles the above, plus the configuration file, the delegate script, and a non-root user.

## Testing

```sh
test/smoke-test.sh                       # against rosa-image-server:dev
test/smoke-test.sh --image rosa-image-server:5.0.7-c1caaa2d --benchmark
```

No AWS access is needed. The script makes a JPEG 2000 test image with the image's own `kdu_compress`, serves it with `FilesystemSource` under the identifier `rose/Test/Test.001r`, and prints PASS/FAIL for each check:

- `/health`, Image API 2 and 3 `info.json` (`@id`/`id` built from `BASE_URI`), the base-URI 303 redirect, a 512 px tile, the 80 px thumbnail the viewer's search results use, `full/max`
- `%2F`-encoded identifiers, the CORS header, `Cache-Control` with `max-age` and `s-maxage`, and `no-cache` on 404s
- the legacy `@id` rewrite with and without the marker header (see below), and that the v3 endpoint and image requests ignore it
- that `KakaduNativeProcessor` decodes and `TurboJPEGImageWriter` encodes, with no native library errors

`--benchmark` compares `info.json` latency with the delegate script on and off. The difference is within noise, at about 2–3 ms median for each. `--keep` leaves the server running for the CloudFront Function end-to-end test in `jhu-rosa2-tf/modules/rosa2-image-server/functions/test/`.

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
  rosa-image-server:5.0.7-c1caaa2d
```

Credentials come from the default AWS SDK chain: exported keys (as here) or a mounted `~/.aws` locally, and the task role in ECS.

To serve images from a local directory instead, bypassing S3 entirely:

```sh
docker run --rm -p 8182:8182 -v /path/to/jp2s:/images:ro \
  -e SOURCE_STATIC=FilesystemSource \
  -e FILESYSTEMSOURCE_BASICLOOKUPSTRATEGY_PATH_PREFIX=/images/ \
  -e FILESYSTEMSOURCE_BASICLOOKUPSTRATEGY_PATH_SUFFIX=.jp2 \
  -e CACHE_SERVER_DERIVATIVE_ENABLED=false \
  rosa-image-server:5.0.7
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
| `JAVA_OPTS` | empty | Extra JVM arguments, appended to the fixed ones. Use this for heap or GC tuning, e.g. `-Xmx3g` or `-XX:+UseZGC`. |
| `CANTALOUPE_CONFIG` | `/etc/cantaloupe/cantaloupe.properties` | Path passed as `-Dcantaloupe.config`. Change it if you mount a replacement configuration file. |
| `JAVA_LIBRARY_PATH` | `/opt/kakadu/lib:/opt/libjpeg-turbo/lib64:` plus the standard JVM entries | Passed as `-Djava.library.path`, which is how `System.loadLibrary()` finds Kakadu and TurboJPEG. Because this property replaces the default rather than extending it, the standard system entries are included explicitly; preserve them if you override this. |
| `CANTALOUPE_VERSION` | build-arg value | Informational only. |

The entrypoint always applies these, which are not configurable by design:

- `-XX:MaxRAMPercentage=75.0` — the JVM otherwise picks about 25% of the container memory limit, which the Cantaloupe manual calls out as too conservative. Size the heap by setting the ECS task memory rather than by passing `-Xmx`.
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

`cantaloupe.properties` is a copy of the `cantaloupe.properties.sample` shipped with Cantaloupe 5.0.7, so every key the application knows about is present with its upstream comments. Values changed for this deployment are marked with a `ROSA:` comment explaining the reasoning — search for `ROSA:` to review all of them.

Edit the file and rebuild the image for changes that apply to every environment; use environment variables for anything that differs between environments.

### Key decisions encoded in the file

**Source.** `source.static = S3Source` with `BasicLookupStrategy`. Identifiers in the generated manifests carry no file extension (`aor/BL531k6/BL531k6.001r`) while the objects do, so `path_suffix = .jp2` appends it; this also lets the source infer the format from the key instead of issuing an extra ranged GET. Chunking stays enabled. Each request reads the object in its own 512 KB ranged GETs; for these 1–3 MB files that is usually most of the file, because the chunk cache lasts only one request. A local source cache (chunking off, `processor.stream_retrieval_strategy = CacheStrategy`, so each JP2 is downloaded once to task storage) was tried on stage and reverted: 16-tile page opens were 15–20% faster, but isolated cold requests such as thumbnails and first tiles got slower (about 230 ms to 390 ms), because the whole file is downloaded first, and Cantaloupe has no safe way to bound the cache's disk use (see the cache worker below). Cold tiles are bound by Kakadu decode CPU, about 130 ms of CPU per 512 px tile on a 2 vCPU Fargate task.

**Processor.** `ManualSelectionStrategy` maps `jp2` to `KakaduNativeProcessor` explicitly, so a missing or broken Kakadu library fails the request instead of silently falling back to a slower processor. JPEG output goes through libjpeg-turbo for every processor when the library is present. `processor.jpg.progressive = false`: on a 1-CPU task, baseline encoding served a burst of 73 distinct tiles about 9% faster than progressive, for about 14% more bytes per tile. OpenSeadragon draws a tile only once it has fully loaded, so progressive rendering buys the viewer nothing. Output stays at `processor.jpg.quality = 80`.

**Caching.** There are three tiers:

1. Browsers and CloudFront. Successful responses carry `Cache-Control: max-age=2592000, s-maxage=31536000, public, no-transform`, so browsers keep them for 30 days and CloudFront for up to a year. Error responses are `no-cache`.
2. Cantaloupe's derivative cache, `S3Cache`, in a per-environment bucket under `<environment>/`. ECS task storage is ephemeral and unshared, and CloudFront's edge caches fill independently, so this shared tier keeps edge misses off the decoder.
3. The heap-based info cache, as a level-1 cache in each task.

`resolve_first = false` skips an S3 existence check on every cache hit, which is safe because source objects are static. If an object is replaced without its identifier changing, purge its derivatives from the cache bucket and create a CloudFront invalidation.

The cache worker is **disabled**: the manual advises running it on only one node in a cluster, which ECS cannot express cleanly. It is also broken with `S3Cache` in 5.0.7: the purge pages with ListObjects v1 using `nextMarker`, which S3 returns only when a delimiter is set, so once the prefix holds more than 1000 objects it re-lists the first page forever (on stage, about 5 LIST requests a second and a fifth of a vCPU). The cache bucket's lifecycle rule expires the prefix instead, after the same TTL Cantaloupe is given.

**Endpoints.** IIIF Image API 2 and 3 are enabled, API 1 is not. The Control Panel (`/admin`) and the administrative HTTP API are disabled; if you enable either, supply a secret from Secrets Manager and keep the paths off the public load balancer.

**Limits.** `max_pixels = 50000000`, which accommodates full-size renderings of the archive's roughly 33 MP page images while still capping runaway requests. It does not apply to requests for full-sized unmodified images. `http.max_threads = 64` keeps concurrent decodes from exhausting the heap before the request queue pushes back. In AWS it is set per environment (`image_server_http_max_threads`: 32 for a 4 GiB stage task, 64 for an 8 GiB prod task).

**Delegate script.** Enabled, for one method only; see below. Every other method is the upstream pass-through stub.

**Logging.** Console only, at `info`, for the ECS log driver to forward to CloudWatch. All file appenders are off, since task storage is ephemeral. Set `LOG_APPLICATION_LEVEL=debug` temporarily to see which processor and image writer handled each request. The access log is off to limit CloudWatch volume; enable it with `LOG_ACCESS_CONSOLEAPPENDER_ENABLED=true`.

### Reverse proxy

Behind a proxy, Cantaloupe has to be told its public URL, because it builds the `info.json` `@id` and the `Location` and `Link` headers from it. In AWS this is `BASE_URI`, set by OpenTofu to the CloudFront URL. The `X-Forwarded-*` headers are no use there: CloudFront talks to the internal ALB over HTTP, and the ALB overwrites `X-Forwarded-Proto` with `http`, which would put `http://` URLs in `info.json`.

Whatever is in front must pass `%2F` in identifiers through undecoded. CloudFront and the ALB do.

### Legacy `/iiif` URLs

The current image server answers Image API 2 at `/iiif/<id>/...` (Apache rewrites the path to `/iiif/2` and the response body back). Existing sites and manifests use that form, so the new server keeps it working alongside `/iiif/2` and `/iiif/3`.

Cantaloupe always builds Image API 2 URLs as `<base_uri>/iiif/2/<id>`, and the `X-Forwarded-*` headers and `base_uri` change only the part before `/iiif/2`. So the work is split three ways:

| Piece | Does |
|-------|------|
| CloudFront viewer-request function (`jhu-rosa2-tf/modules/rosa2-image-server/functions/legacy-request.js`) | `/iiif/<id>` → 303 to `/iiif/<id>/info.json`; `/iiif/<id>/...` → `/iiif/2/<id>/...`; on `info.json` only, adds `X-Rosa-Legacy-Path: 1` (part of the CloudFront cache key) |
| `delegates.rb`, `extra_iiif2_information_response_keys` | When the header is present, returns `@id` = `<base_uri>/iiif/<id>`. Cantaloupe merges these keys after setting its own `@id`, so this replaces it. |
| CloudFront viewer-response function (`legacy-response.js`) | Rewrites `/iiif/2/` to `/iiif/` in `Location` and `Link` (the delegate cannot reach headers) |

Only `info.json` differs between the two forms, so tiles requested either way share one CloudFront cache entry. The marker header is stripped from client requests at the edge and is not forwarded at all on `/iiif/2` and `/iiif/3`, so it cannot be used to change cached responses for the new paths. Without the header, the delegate changes nothing, so local runs and `/iiif/2` behave exactly as stock Cantaloupe.

## IAM

The ECS task role (created by jhu-rosa2-tf) needs:

- `s3:GetObject` on the source keys, and `s3:ListBucket` limited to the source prefix, so that a missing image is a 404 rather than a 403
- `s3:GetObject`, `s3:PutObject` and `s3:DeleteObject` on the environment's cache prefix, and `s3:ListBucket` limited to it

## Reference

- [Cantaloupe 5.0 manual](https://cantaloupe-project.github.io/manual/5.0/)
- [Deployment & tuning](https://cantaloupe-project.github.io/manual/5.0/deployment.html)
- [Caching](https://cantaloupe-project.github.io/manual/5.0/caching.html)
- [Rosa deployment notes](../docs/deployment.md)
