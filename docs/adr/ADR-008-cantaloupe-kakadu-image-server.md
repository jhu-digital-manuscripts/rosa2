# ADR-008: Cantaloupe + Kakadu IIIF Image server on ECS Fargate behind CloudFront

**Date:** 2026-10-14
**Status:** Accepted
**Deciders:** rosa2 maintainers
**Supersedes:** N/A

> Retroactive record of a decision already embodied in the codebase.

## Context and Problem Statement

The viewer requests page images through the IIIF Image API. Source images are
JPEG 2000 in S3, and the deployment must decode them, serve IIIF-compliant
derivatives, support both Image API 2 and 3 plus legacy `/iiif/<id>` URLs, and
scale down to zero cost when idle. We needed to choose an image server, a JPEG 2000
codec, and a runtime.

## Decision Drivers

- IIIF Image API 2 and 3 support, plus the legacy `/iiif/<id>` paths served today by `image.library.jhu.edu`.
- Fast, correct JPEG 2000 decoding.
- Managed, scalable runtime with derivative caching and the ability to scale to zero.
- Keep the licensed codec out of the repository.

## Considered Options

1. Cantaloupe with the commercially licensed Kakadu codec and libjpeg-turbo, packaged as a Docker image on ECS Fargate behind CloudFront, with CloudFront Functions + `delegates.rb` for legacy URLs.
2. Cantaloupe with its bundled open-source JPEG 2000 decoder.
3. A different IIIF image server (e.g. IIPImage) or a hosted/third-party image service.

## Decision Outcome

**Chosen option:** Option 1. The image (`rosa-image-server/`) runs Cantaloupe on
`amazoncorretto:25`, decodes JPEG 2000 with compiled Kakadu, and encodes
derivatives with libjpeg-turbo, caching derivatives back to S3. It runs on ECS
Fargate behind CloudFront (module `rosa2-image-server` in `jhu-rosa2-tf`), serving
`/iiif/2`, `/iiif/3`, and legacy `/iiif/<id>` (via CloudFront Functions plus
`delegates.rb`). The Kakadu ZIP is passed as a named build context and never copied
into the repo; `.gitignore` excludes `*.zip`.

### Positive Consequences

- Fast, correct JPEG 2000 decoding via Kakadu; good-quality JPEG derivatives via libjpeg-turbo.
- Managed autoscaling with scale-to-zero and S3 derivative caching behind CloudFront.
- Image API 2, 3, and legacy URLs all served from one deployment.

### Negative Consequences

- Requires a commercial Kakadu license and a build that compiles it from source (~2–3 min cold).
- libjpeg-turbo is pinned: it must stay on 2.x because 3.x is incompatible with Cantaloupe 5.
- Legacy URL support spans CloudFront Functions and `delegates.rb`, which must change together.

## Options Analysis

### Option 1: Cantaloupe + Kakadu on ECS Fargate + CloudFront
Pros: fast JP2 decode; managed + scale-to-zero; full URL compatibility. | Cons: commercial license; source build; version pins.

### Option 2: Cantaloupe + bundled open-source JP2 decoder
Pros: no license, no source build. | Cons: slower/lower-quality JP2 decode; the bundled Kakadu is Public-Service-License only and forbids commercial use.

### Option 3: Different server or hosted service
Pros: possibly less to build. | Cons: migration from the existing Cantaloupe/IIIF setup; third-party cost/control; may not match legacy URL behavior.

## Links and References
- Relates to: [ADR-001](./ADR-001-static-iiif-presentation-generation.md)
- Implementation: `rosa-image-server/` (`Dockerfile`, `build-and-push.sh`, `delegates.rb`), `jhu-rosa2-tf/modules/rosa2-image-server/`, `docs/deployment.md`
