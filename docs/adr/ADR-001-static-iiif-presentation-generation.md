# ADR-001: Static IIIF Presentation 3.0 generation as the publishing format

**Date:** 2026-10-14
**Status:** Accepted
**Deciders:** rosa2 maintainers
**Supersedes:** N/A

> Retroactive record of a decision already embodied in the codebase. Captured to
> make the rationale explicit for future contributors.

## Context and Problem Statement

Rosa must turn a file-system archive of digitized books (metadata, transcriptions,
Archaeology of Reading annotations) into a form a web viewer can consume. The
publishing format determines hosting cost, operational complexity, and how the
viewer, search, and image server fit together. We needed a format that is open,
viewer-agnostic, and cheap to serve.

## Decision Drivers

- Interoperability with existing IIIF viewers (Mirador) and the wider IIIF ecosystem.
- Minimal runtime infrastructure for serving presentation data.
- Deterministic, reproducible output that can be regenerated from the archive at any time.
- Clean separation between presentation data, search, and image delivery.

## Considered Options

1. Generate static IIIF Presentation 3.0 JSON ahead of time with a CLI (`rosa-tool`).
2. Serve IIIF Presentation JSON dynamically from an application server backed by a database.
3. A bespoke, non-IIIF JSON API tailored to the viewer.

## Decision Outcome

**Chosen option:** Option 1, because static generation keeps the presentation tier
free of running servers (any static host works), makes output fully reproducible
from the archive, and conforms to IIIF Presentation 3.0 so standard viewers work
without custom glue. The `generate-iiif-pres` command produces a file hierarchy
(`collection.json`, `<collection>/collection.json`, `<collection>/<book>/manifest.json`)
that maps directly to IIIF resource URLs.

### Positive Consequences

- No application server or database in the presentation path; hosting is a static web server, S3, or GitHub Pages.
- Output is deterministic and compact (nulls and empty values omitted), so changes are reviewable and reproducible.
- Standard IIIF viewers consume the output directly; search and image delivery stay decoupled.

### Negative Consequences

- Any change to presentation data requires regenerating and redeploying files; there is no live edit path.
- Large archives produce many files that must be deployed as a unit.

## Options Analysis

### Option 1: Static IIIF Presentation 3.0 generation
Pros: zero runtime for the presentation tier; reproducible; standards-compliant; decoupled. | Cons: regeneration/redeploy on every change.

### Option 2: Dynamic IIIF server
Pros: live edits; no regeneration step. | Cons: running server + datastore to operate and secure; non-reproducible by inspection; more moving parts.

### Option 3: Bespoke non-IIIF API
Pros: tailored exactly to the viewer. | Cons: loses the entire IIIF viewer and tooling ecosystem; custom client and server to maintain.

## Links and References
- Relates to: [ADR-006](./ADR-006-single-viewer-codebase-per-site-builds.md), [ADR-002](./ADR-002-opensearch-two-index-design.md), [ADR-007](./ADR-007-annotation-bodies-iiif-cookbook-html.md)
- Implementation: `rosa-tool/src/main/java/rosa/archive/iiif/`, `docs/iiif-generation.md`
