# ADR-002: Two-index Opensearch design with annotations merged into canvas

**Date:** 2026-10-14
**Status:** Accepted
**Deciders:** rosa2 maintainers
**Supersedes:** N/A

> Retroactive record of a decision already embodied in the codebase.

## Context and Problem Statement

The viewer needs both book-level search (find a book by metadata) and page-level
search (find a page by its annotations and transcription). The Archaeology of
Reading data attaches many annotations of different types to each page. We had to
decide how to model books, pages, and annotations as Opensearch documents so that
search is fast and faceting works.

## Decision Drivers

- Support book-level and page-level queries with clear, stable document identities.
- Keep per-page annotation text independently searchable by type.
- Avoid join-style queries, which Opensearch does not do well.
- Keep the corpus small enough to serve from a single small node.

## Considered Options

1. Two indexes — `manifest` (one doc per book) and `canvas` (one doc per page) — with all annotations targeting a page merged into that page's canvas document.
2. Three indexes — `manifest`, `canvas`, and a separate `annotation` index — with annotations joined to canvases at query time.
3. A single flat index mixing books, pages, and annotations.

## Decision Outcome

**Chosen option:** Option 1, because merging annotations into the canvas document
makes a page a single self-contained document: per-type fields (`marginalia`,
`mark`, `symbol`, ...) are searchable directly, with no join. Books and pages each
get a clean identity (`{collection}.{book}` and `{collection}.{image-id}`), and the
page document carries `page_num` and `manifest_id` so clients can construct IIIF
canvas URIs from a hit.

### Positive Consequences

- No join-style queries; a page hit already contains all its annotation text.
- Stable, meaningful document IDs that map onto IIIF URIs.
- Clear separation of book-level and page-level search surfaces.

### Negative Consequences

- Re-indexing a single annotation means re-emitting the whole canvas document.
- Canvas documents are wide, with many per-type and per-language fields.

## Options Analysis

### Option 1: Two indexes, annotations merged into canvas
Pros: no joins; self-contained page docs; clean IDs. | Cons: wide documents; whole-page re-index on any annotation change.

### Option 2: Separate annotation index
Pros: granular annotation updates. | Cons: query-time joins Opensearch handles poorly; more complex client queries.

### Option 3: Single flat index
Pros: one mapping. | Cons: mixed document shapes defeat faceting and clean identity; awkward queries.

## Links and References
- Relates to: [ADR-003](./ADR-003-per-language-text-subfields.md), [ADR-004](./ADR-004-single-shard-zero-replicas.md), [ADR-001](./ADR-001-static-iiif-presentation-generation.md)
- Implementation: `rosa-search/opensearch/{manifest,canvas}.json`, `rosa-tool/src/main/java/rosa/archive/opensearch/`, `docs/opensearch-indexes.md`
