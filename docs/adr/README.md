# Architecture Decision Records

This directory holds the Architecture Decision Records (ADRs) for rosa2, in
[MADR](https://adr.github.io/madr/) format. Each record captures the context, the
decision, and its consequences for a significant architectural choice.

The initial set below is a retroactive record of decisions already embodied in the
codebase, created to make their rationale explicit for future contributors. New
decisions are added as new ADRs; an accepted decision is never rewritten in place —
it is superseded by a later ADR.

See the `adr` power's `workflow` steering for the templates and process.

| ADR | Title | Status | Date |
|-----|-------|--------|------|
| [ADR-001](./ADR-001-static-iiif-presentation-generation.md) | Static IIIF Presentation 3.0 generation as the publishing format | Accepted | 2026-10-14 |
| [ADR-002](./ADR-002-opensearch-two-index-design.md) | Two-index Opensearch design with annotations merged into canvas | Accepted | 2026-10-14 |
| [ADR-003](./ADR-003-per-language-text-subfields.md) | Per-language text sub-fields for multi-language analysis | Accepted | 2026-10-14 |
| [ADR-004](./ADR-004-single-shard-zero-replicas.md) | Single shard and zero replicas for the Opensearch indexes | Accepted | 2026-10-14 |
| [ADR-005](./ADR-005-read-only-signed-opensearch-access.md) | Read-only, signed, validated Opensearch access via API Gateway + Lambda | Accepted | 2026-10-14 |
| [ADR-006](./ADR-006-single-viewer-codebase-per-site-builds.md) | Single viewer codebase built into per-site bundles | Accepted | 2026-10-14 |
| [ADR-007](./ADR-007-annotation-bodies-iiif-cookbook-html.md) | Annotation bodies as IIIF cookbook 0019/0045 styled HTML | Accepted | 2026-10-14 |
| [ADR-008](./ADR-008-cantaloupe-kakadu-image-server.md) | Cantaloupe + Kakadu IIIF Image server on ECS Fargate behind CloudFront | Accepted | 2026-10-14 |
| [ADR-009](./ADR-009-custom-latin-analyzer.md) | Custom Latin analyzer via stemmer_override with character normalization | Accepted | 2026-10-14 |
