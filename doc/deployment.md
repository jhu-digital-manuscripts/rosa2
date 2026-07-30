# Deployment

## Purpose

This document describes how to build, package, and deploy the rosa2 CLI tool, including running it in production environments.

## Audience

Developers building the project and system administrators deploying the tool.

## Prerequisites

- Java 25 or later (JDK)
- Maven 3.9+
- Access to the archive data directory
- Opensearch cluster (for search functionality)

## Building

Build the executable JAR from the project root:

```bash
mvn clean package
```

This produces a fat JAR at `target/rosa2-2.0.0-SNAPSHOT.jar` containing all runtime dependencies. The JAR includes a `Main-Class` manifest entry, so it can be executed directly.

To skip tests during development builds:

```bash
mvn clean package -DskipTests
```

## Running

```bash
java -jar target/rosa2-2.0.0-SNAPSHOT.jar <command> [options]
```

Verify the build works:

```bash
java -jar target/rosa2-2.0.0-SNAPSHOT.jar --help
```

## Typical Deployment Pipeline

A standard deployment workflow for generating and publishing content:

1. **Build the JAR** on a CI server or locally.

2. **Shallow-copy the archive** to the deployment environment (if only metadata is needed):
   ```bash
   java -jar rosa2.jar shallow-copy --archive /data/archive --output /deploy/archive
   ```

3. **Generate IIIF files** for static serving:
   ```bash
   java -jar rosa2.jar generate-iiif-pres \
     --archive /data/archive \
     --output /deploy/iiif \
     --base-url https://iiif.example.org
   ```

4. **Create Opensearch indexes** using the definition files:
   ```bash
   curl -X PUT "https://search.example.org:9200/manifests" \
     -H "Content-Type: application/json" -d @opensearch/manifests.json
   curl -X PUT "https://search.example.org:9200/canvases" \
     -H "Content-Type: application/json" -d @opensearch/canvases.json
   curl -X PUT "https://search.example.org:9200/annotations" \
     -H "Content-Type: application/json" -d @opensearch/annotations.json
   ```

5. **Generate and ingest bulk data**:
   ```bash
   java -jar rosa2.jar generate-opensearch-ingest \
     --archive /data/archive --output /deploy/bulk

   for f in /deploy/bulk/*.bulk.json; do
     curl -X POST "https://search.example.org:9200/_bulk" \
       -H "Content-Type: application/x-ndjson" --data-binary @"$f"
   done
   ```

6. **Serve IIIF files** via a static web server (nginx, Apache, S3, etc.) with CORS headers enabled for IIIF viewer access.

## GitHub Actions Release

The project includes a GitHub Actions release workflow (triggered via `workflow_dispatch`) that:
- Sets the release version in the POM
- Builds and tests the project
- Creates a GitHub release with the JAR attached
- Bumps the version to the next development snapshot

See `.github/workflows/release.yml` for configuration details.

## Validation

After deployment, verify data integrity:

```bash
java -jar rosa2.jar check --archive /data/archive
java -jar rosa2.jar validate-xml --archive /data/archive --collection aor
```
