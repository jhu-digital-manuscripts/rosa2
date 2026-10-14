# ADR-004: Single shard and zero replicas for the Opensearch indexes

**Date:** 2026-10-14
**Status:** Accepted
**Deciders:** rosa2 maintainers
**Supersedes:** N/A

> Retroactive record of a decision already embodied in the codebase.

## Context and Problem Statement

The `manifest` and `canvas` index definitions set `number_of_shards: 1` and
`number_of_replicas: 0`. Both are deliberate departures from cluster defaults, and
the shard count is fixed at index creation. The rationale needs recording so the
settings are not "corrected" toward defaults.

## Decision

Set `number_of_shards: 1` and `number_of_replicas: 0` explicitly in both index
definitions.

## Rationale for No Alternatives

The corpus is roughly 35 MB of bulk data across both indexes, which fits
comfortably in one shard. Leaving the shard count to the cluster default is the
problem being avoided: AWS managed domains defaulted to 5 primary shards per
index, so a combined `/_search` fanned out to 10 shard searches for a corpus that
belongs in one — measurable latency and heap overhead on a small single-node
domain. One replica is equally wrong here: every environment runs a single data
node, a replica cannot be placed on the same node as its primary, so the default
of 1 leaves a shard permanently unassigned and the cluster permanently yellow,
making health alarms meaningless. There is no viable "more shards / more replicas"
alternative for a single-node, 35 MB deployment.

## Consequences

- The combined search touches two shard searches, not ten; latency and heap stay low.
- Cluster health is green, so health alarms carry signal.
- No redundancy: a lost node loses the index until re-ingest. If a domain is ever scaled to two or more data nodes, raise `number_of_replicas` (it is dynamic: `PUT /<index>/_settings`).
- Changing the shard count requires rebuilding the indexes (`init-opensearch.sh` deletes and recreates them), since `number_of_shards` is fixed at creation.

## Links and References
- Relates to: [ADR-002](./ADR-002-opensearch-two-index-design.md), [ADR-005](./ADR-005-read-only-signed-opensearch-access.md)
- Implementation: `rosa-search/opensearch/{manifest,canvas}.json`, `docs/opensearch-indexes.md`
