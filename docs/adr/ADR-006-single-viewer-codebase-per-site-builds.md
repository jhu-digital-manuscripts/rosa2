# ADR-006: Single viewer codebase built into per-site bundles

**Date:** 2026-10-14
**Status:** Accepted
**Deciders:** rosa2 maintainers
**Supersedes:** N/A

> Retroactive record of a decision already embodied in the codebase.

## Context and Problem Statement

Rosa serves two public viewers — Archaeology of Reading (AoR) and the Digital
Library of Medieval Manuscripts (DLMM) — with different collections, facets, and
styling, deployed to different hosts (GitHub Pages for stage, WordPress portals
for prod). We needed to serve both without maintaining two divergent codebases.

## Decision Drivers

- One Mirador + React + JHSearch codebase, so fixes and features reach both sites.
- Per-site identity (collections, facets, styling) that does not leak into shared code.
- Deployment URLs that vary per environment without rebuilding site identity.
- A build process that discovers sites rather than hard-coding them.

## Considered Options

1. One codebase; each site is a `sites/<id>/` directory (`index.html`, `main.js`, `site.css`) that the build scripts discover; deployment URLs come from `VITE_ROSA_*` env vars.
2. Separate repositories or packages per site.
3. A single runtime-configurable viewer that selects the site from a URL parameter or config fetch.

## Decision Outcome

**Chosen option:** Option 1. Site identity (collection ids, `enableFacets`, styling)
lives in `sites/<site>/main.js` and `site.css`; only deployment URLs vary, via
`VITE_ROSA_IIIF_BASE_URL` and `VITE_ROSA_IMAGE_BASE_URL`. `npm run build:sites`
discovers sites by directory and emits a self-contained bundle per site. The same
build serves local-dev, stage, and prod; only the substituted URLs differ.

### Positive Consequences

- One codebase; features and fixes land in both sites at once.
- Adding a site is adding a `sites/<id>/` directory; no build-script changes.
- `VITE_ROSA_*` values are substituted into each site's `index.html` and remain editable in the built page, so one build retargets across environments.

### Negative Consequences

- Shared code must stay genuinely site-agnostic; site specifics must not leak into it.
- A change to shared code affects both sites, so both must be verified.

## Options Analysis

### Option 1: One codebase, per-site directories
Pros: shared code; discovery-based builds; env-var retargeting. | Cons: discipline needed to keep shared code site-agnostic.

### Option 2: Separate repos/packages per site
Pros: full isolation. | Cons: divergence; duplicated fixes; double maintenance.

### Option 3: Runtime site selection
Pros: single artifact. | Cons: ships every site's assets to every user; runtime config path to secure and operate; heavier bundles.

## Links and References
- Relates to: [ADR-001](./ADR-001-static-iiif-presentation-generation.md), [ADR-007](./ADR-007-annotation-bodies-iiif-cookbook-html.md)
- Implementation: `rosa-viewer/sites/`, `rosa-viewer/scripts/build-sites.mjs`, `rosa-viewer/.env`
