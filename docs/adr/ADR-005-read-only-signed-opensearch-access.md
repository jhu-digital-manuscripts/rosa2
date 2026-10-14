# ADR-005: Read-only, signed, validated Opensearch access via API Gateway + Lambda

**Date:** 2026-10-14
**Status:** Accepted
**Deciders:** rosa2 maintainers
**Supersedes:** N/A

> Retroactive record of a decision already embodied in the infrastructure.

## Context and Problem Statement

The production viewer is static and runs in the browser, but it must query a
managed Opensearch domain. Exposing the domain directly would mean either a public
search endpoint or shipping credentials to the browser, and would let clients send
arbitrary, expensive, or destructive queries. We needed browser-reachable search
without a public domain endpoint and without trusting the client's query shape.

## Decision Drivers

- No public endpoint on the Opensearch domain.
- No AWS credentials in the browser; requests signed server-side.
- Read-only access from the public path; writes isolated.
- Protection against expensive or abusive queries (unbounded size, scripts, wildcards).

## Considered Options

1. Browser → API Gateway HTTP API → request-signing Lambda (SigV4) → Opensearch, with a strict query allow-list; a separate in-VPC admin Lambda for index creation and ingest.
2. Public Opensearch domain with an IP/network access policy.
3. Fine-grained Opensearch security with per-client credentials delivered to the browser.

## Decision Outcome

**Chosen option:** Option 1. The domain has no public endpoint. A signing Lambda
accepts only `POST` to `/_search`, `/manifest/_search`, and `/canvas/_search`, and
its IAM credentials are scoped to those paths. Queries are validated and capped:
`size` ≤ 100, `from + size` ≤ 10000, body ≤ 16 KB, and `script`, `query_string`,
`regexp`, and wildcard queries are rejected. Index creation and ingest go through a
separate administration Lambda inside the VPC. The validation in
`search_proxy/app.py` is the contract the viewer's query builder must satisfy.

### Positive Consequences

- No public domain endpoint and no credentials in the browser.
- The public path is read-only by construction (IAM + path allow-list).
- Expensive and dangerous query shapes are rejected before reaching the domain.

### Negative Consequences

- The viewer's query builder and the proxy's allow-list are coupled: changing query shapes requires updating `test_search_proxy.py` and the proxy together.
- An extra network hop and a Lambda to operate.

## Options Analysis

### Option 1: API Gateway + signing Lambda with allow-list
Pros: private domain; read-only public path; validated queries. | Cons: proxy/viewer coupling; extra hop.

### Option 2: Public domain with network ACL
Pros: no proxy to run. | Cons: public attack surface; no query validation; coarse control.

### Option 3: Per-client Opensearch credentials in the browser
Pros: native Opensearch security. | Cons: secrets in the browser; still no query-shape control; credential rotation burden.

## Links and References
- Relates to: [ADR-002](./ADR-002-opensearch-two-index-design.md), [ADR-004](./ADR-004-single-shard-zero-replicas.md)
- Implementation: `rosa-search/lambda/search_proxy/app.py`, `jhu-rosa2-tf/modules/rosa2-search/`, `docs/deployment.md`
