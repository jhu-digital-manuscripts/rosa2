# ADR-007: Annotation bodies as IIIF cookbook 0019/0045 styled HTML

**Date:** 2026-10-14
**Status:** Accepted
**Deciders:** rosa2 maintainers
**Supersedes:** N/A

> Retroactive record of a decision already embodied in the codebase.

## Context and Problem Statement

Archaeology of Reading and DLMM annotations are rich: marginalia with hands,
people, places, cross-references; marks and symbols; transcriptions; commentary.
They must display legibly in a standard IIIF viewer (Mirador) and be styled to
match each site, without a viewer plugin that invents a private body format.

## Decision Drivers

- Render in standard IIIF viewers without bespoke body parsing.
- Allow per-site visual styling of annotation content.
- Keep generated bodies safe and predictable (bounded HTML, no arbitrary markup).
- Share one set of style conventions between generated output and the viewer.

## Considered Options

1. IIIF cookbook recipe 0019 (HTML in annotations) bodies wrapped in a recipe 0045 (CSS in an annotation) `SpecificResource` carrying `styleClass` and an optional `stylesheet` URL, with a fixed, limited HTML tag set.
2. Plain-text annotation bodies.
3. A custom structured JSON body format rendered by a dedicated viewer plugin.

## Decision Outcome

**Chosen option:** Option 1. Commenting annotations are a `SpecificResource` whose
`styleClass` names CSS classes, wrapping an HTML `TextualBody`; with
`--annotation-stylesheet-url` the annotation also carries a `stylesheet`. The HTML
is limited to the recipe 0019 tags (`a b br i img p small span sub sup`), checked
by `AnnotationHtml.findViolation`. Every annotation gets `rosa-anno` plus
collection- and type-specific classes (`aor-mark`, `aor-place-intext`,
`dlmm-transcription dlmm-column-{n}`, ...). Those classes and conventions are
shared with the stylesheets in `rosa-viewer/public/styles/{aor,dlmm}.css` and must
change together.

### Positive Consequences

- Renders in standard IIIF viewers with no custom body parser.
- Per-site CSS styles the same bodies differently via `styleClass` + `stylesheet`.
- The bounded tag set keeps bodies safe and deterministic.

### Negative Consequences

- The generator's HTML conventions/classes and the viewer stylesheets are coupled and must be changed together.
- The limited tag set constrains how expressive a body can be.

## Options Analysis

### Option 1: Cookbook 0019/0045 styled HTML
Pros: standard-viewer rendering; per-site CSS; safe bounded HTML. | Cons: generator/stylesheet coupling; limited tags.

### Option 2: Plain-text bodies
Pros: trivially safe. | Cons: no structure or styling; loses the richness of the data.

### Option 3: Custom JSON body + plugin
Pros: fully expressive. | Cons: requires a bespoke plugin in every viewer; non-standard; more to maintain.

## Links and References
- Relates to: [ADR-001](./ADR-001-static-iiif-presentation-generation.md), [ADR-006](./ADR-006-single-viewer-codebase-per-site-builds.md)
- Implementation: `rosa-tool/src/main/java/rosa/archive/iiif/AnnotationHtml.java`, `rosa-tool/src/main/java/rosa/archive/iiif/AorAnnotationFormatter.java`, `rosa-viewer/public/styles/`, `docs/iiif-generation.md`
