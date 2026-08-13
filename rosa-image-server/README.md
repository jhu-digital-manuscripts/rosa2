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
| Base image | `amazoncorretto:25-al2023-jdk` | Java 25 |

The server listens on port **8182** and runs as the non-root `cantaloupe` user (uid 999). A health check endpoint is available at `/health`, and the IIIF Image API 2 and 3 endpoints at `/iiif/2` and `/iiif/3`.

## Prerequisites

- Docker or Podman
- A **commercial Kakadu license**. The Kakadu binaries bundled with Cantaloupe releases are built under a Public Service License that forbids commercial use, so this image discards them and compiles your licensed distribution instead.
- The Kakadu distribution ZIP copied into this directory. It must be inside the build context, since Docker cannot read files outside it. `.gitignore` excludes `*.zip` so the licensed source is never committed.

## Building

```sh
docker build --platform linux/amd64 -t rosa-image-server:5.0.7 .
```

The build compiles Kakadu and libjpeg-turbo from source and downloads the Cantaloupe release, so expect roughly 2–3 minutes on a cold cache and outbound network access to GitHub.

To override versions:

```sh
docker build --platform linux/amd64 -t rosa-image-server:5.0.7 \
  --build-arg KAKADU_ZIP=v8_6_1-02130L.zip \
  --build-arg CANTALOUPE_VERSION=5.0.7 \
  --build-arg LIBJPEG_TURBO_VERSION=2.0.2 .
```

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

1. **kakadu** — unpacks the ZIP and runs `make -f Makefile-Linux-x86-64-gcc`, producing the JNI binding, the shared libraries, and the command-line tools. The makefiles compile with `-O2 -msse2` and gate the SSSE3/SSE4/AVX/AVX2 code paths behind runtime CPUID checks, so the result runs on any x86-64 host.
2. **turbojpeg** — builds libjpeg-turbo with Java support. Distribution packages omit it, and without it Cantaloupe silently falls back to the slower Image I/O JPEG writer.
3. **cantaloupe** — downloads the release ZIP and extracts the jar.
4. **runtime** — assembles the above, plus the configuration file and a non-root user.

## Running locally

Against S3, with the values your deployment uses:

```sh
docker run --rm -p 8182:8182 \
  -e S3SOURCE_REGION=us-east-1 \
  -e S3SOURCE_BASICLOOKUPSTRATEGY_BUCKET_NAME=my-bucket \
  -e S3SOURCE_BASICLOOKUPSTRATEGY_PATH_PREFIX=images/ \
  -e S3CACHE_REGION=us-east-1 \
  -e S3CACHE_BUCKET_NAME=my-bucket \
  rosa-image-server:5.0.7
```

Credentials come from the default AWS SDK chain, so this needs `AWS_ACCESS_KEY_ID`/`AWS_SECRET_ACCESS_KEY` (or a mounted `~/.aws`) locally, and nothing at all in ECS, where the task role is picked up automatically.

If you are loggin in with an sso on the command line:
```sh
aws sso login --profile name
export AWS_PROFILE=blah
eval $(aws configure export-credentials --format env)
```

docker run --rm -p 8182:8182   -e S3SOURCE_REGION=us-east-1   -e S3SOURCE_BASICLOOKUPSTRATEGY_BUCKET_NAME=jhu-library-images   -e S3SOURCE_BASICLOOKUPSTRATEGY_PATH_PREFIX=iiifstore/   -e S3CACHE_REGION=us-east-1   -e S3CACHE_BUCKET_NAME=jhu-library-images   -e AWS_SECRET_ACCESS_KEY -e AWS_ACCESS_KEY_ID -e AWS_PROFILE -e AWS_CREDENTIAL_EXPIRATION -e AWS_SESSION_TOKEN  localhost/test/cantaloupe 

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

## Configuring cantaloupe.properties

`cantaloupe.properties` is a copy of the `cantaloupe.properties.sample` shipped with Cantaloupe 5.0.7, so every key the application knows about is present with its upstream comments. Values changed for this deployment are marked with a `ROSA:` comment explaining the reasoning — search for `ROSA:` to review all of them.

Edit the file and rebuild the image for changes that apply to every environment; use environment variables for anything that differs between environments.

### Key decisions encoded in the file

**Source.** `source.static = S3Source` with `BasicLookupStrategy`. Identifiers in the generated manifests carry no file extension (`aor/BL531k6/BL531k6.001r`) while the objects do, so `path_suffix = .jp2` appends it; this also lets the source infer the format from the key instead of issuing an extra ranged GET. Chunking stays enabled: with JPEG 2000 and Kakadu, Cantaloupe fetches only the byte ranges a request needs, and this bypasses the source cache entirely.

**Processor.** `ManualSelectionStrategy` maps `jp2` to `KakaduNativeProcessor` explicitly, so a missing or broken Kakadu library fails the request instead of silently falling back to a slower processor. JPEG output goes through libjpeg-turbo for every processor when the library is present.

**Caching.** The derivative cache is enabled and set to `S3Cache` under the `cache/` prefix of the same bucket, because ECS task storage is ephemeral and unshared, and there is no CDN in front of the service. The heap-based info cache stays on as a level-1 cache. `resolve_first = false` skips an S3 existence check on every cache hit, which is safe because source objects are static — if an object is replaced without its identifier changing, purge its cached derivatives and info.

The cache worker is **disabled**: the manual advises running it on only one node in a cluster, which ECS cannot express cleanly. Expire the `cache/` prefix with an S3 lifecycle rule matching `cache.server.derivative.ttl_seconds` (30 days by default) instead.

**Endpoints.** IIIF Image API 2 and 3 are enabled, API 1 is not. The Control Panel (`/admin`) and the administrative HTTP API are disabled; if you enable either, supply a secret from Secrets Manager and keep the paths off the public load balancer.

**Limits.** `max_pixels = 50000000`, which accommodates full-size renderings of the archive's roughly 33 MP page images while still capping runaway requests. It does not apply to requests for full-sized unmodified images. `http.max_threads = 64` keeps concurrent decodes from exhausting the heap before the request queue pushes back; raise or lower it with the task size.

**Logging.** Console only, at `info`, for the ECS log driver to forward to CloudWatch. All file appenders are off, since task storage is ephemeral. Set `LOG_APPLICATION_LEVEL=debug` temporarily to see which processor and image writer handled each request. The access log is off to limit CloudWatch volume; enable it with `LOG_ACCESS_CONSOLEAPPENDER_ENABLED=true`.

### Reverse proxy

`base_uri` is blank, on the assumption that the load balancer or proxy sends `X-Forwarded-Proto`, `X-Forwarded-Host`, `X-Forwarded-Port` and `X-Forwarded-Path`. Production serves the Image API 2 endpoint as `/iiif/` rather than `/iiif/2/` by rewriting in the proxy; if the AWS deployment does the same, set `X-Forwarded-Path` to `/iiif` or set `base_uri` so that `@id` values in `info.json` match the public URLs.

## IAM

The ECS task role needs, on the image bucket:

- `s3:GetObject` on the source keys
- `s3:GetObject`, `s3:PutObject`, `s3:DeleteObject` and `s3:ListBucket` on the `cache/` prefix

## Reference

- [Cantaloupe 5.0 manual](https://cantaloupe-project.github.io/manual/5.0/)
- [Deployment & tuning](https://cantaloupe-project.github.io/manual/5.0/deployment.html)
- [Caching](https://cantaloupe-project.github.io/manual/5.0/caching.html)
- [Rosa deployment notes](../doc/deployment.md)
