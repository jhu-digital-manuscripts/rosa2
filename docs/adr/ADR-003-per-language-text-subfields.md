# ADR-003: Per-language text sub-fields for multi-language analysis

**Date:** 2026-10-14
**Status:** Accepted
**Deciders:** rosa2 maintainers
**Supersedes:** N/A

> Retroactive record of a decision already embodied in the codebase.

## Context and Problem Statement

The corpus mixes English, French, Old French, Italian, Latin, Greek, Spanish, and
German within and across annotations. Correct full-text search needs
language-appropriate analysis (stemming, stop words, folding), but Opensearch
applies one analyzer per field. We needed a mapping strategy that lets each piece
of text be analyzed in its own language.

## Decision Drivers

- Language-appropriate stemming and stop words for relevant recall.
- A single, predictable field-naming convention across annotation types.
- A sensible fallback for unknown or unsupported language codes.
- Phrase and highlight behavior that does not bleed across separate annotations.

## Considered Options

1. Route each text into a per-language sub-field (`field.en`, `field.fr`, `field.la`, ...), one analyzer per sub-field, with `.en` as the fallback.
2. One analyzer per field chosen for the dominant language of the corpus.
3. A single language-agnostic analyzer (standard/whitespace) for all text.

## Decision Outcome

**Chosen option:** Option 1. Text is routed at index time into sub-fields suffixed
by language (`.en .fr .ofr .la .it .el .es .de`), each with the matching analyzer;
Old French adds asciifolding for spelling variation. Unknown languages fall back
to `.en`. Each sub-field is an array with a `position_increment_gap` so phrase and
proximity queries and highlight fragments stay within a single annotation.

### Positive Consequences

- Each text fragment is stemmed and filtered in its own language, improving recall.
- Uniform, predictable field names across all annotation types.
- Phrase/highlight stay within one annotation, not across array elements.

### Negative Consequences

- Wide mappings and a routing layer (`LanguageFieldRouter`) that must be kept in sync with the index definitions.
- Mixed-language queries must target the right sub-fields or search across all of them.

## Options Analysis

### Option 1: Per-language sub-fields
Pros: correct per-language analysis; uniform naming; clean fallback. | Cons: wide mappings; routing logic to maintain.

### Option 2: One analyzer per field
Pros: simpler mapping. | Cons: wrong analysis for non-dominant languages; poor recall on Latin, Greek, Old French.

### Option 3: Single language-agnostic analyzer
Pros: simplest. | Cons: no stemming or stop words; weakest relevance of the three.

## Links and References
- Relates to: [ADR-002](./ADR-002-opensearch-two-index-design.md)
- Implementation: `rosa-tool/src/main/java/rosa/archive/opensearch/LanguageFieldRouter.java`, `rosa-search/opensearch/`, `docs/opensearch-indexes.md`
