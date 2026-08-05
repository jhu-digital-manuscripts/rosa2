#! /bin/sh

# Generate IIIF data
java -jar ../target/rosa2-2.0.0-SNAPSHOT.jar generate-iiif-pres --archive=../../rosa-archive/ --output=site/iiif --image-api-version=2 --image-base-url=https://image.library.jhu.edu/iiif/ --base-url=http://localhost:3000/iiif/ --opensearch-url http://localhost:9200/_search

# Generate Opensearch data for bulk ingest
java -jar ../target/rosa2-2.0.0-SNAPSHOT.jar generate-opensearch-ingest --archive=../../rosa-archive/  --output os

# Create Opensearch indexes

curl -X PUT "http://localhost:9200/manifest" \
  -H "Content-Type: application/json" \
  -d @../opensearch/manifest.json

curl -X PUT "http://localhost:9200/canvas" \
  -H "Content-Type: application/json" \
  -d @../opensearch/canvas.json

curl -X PUT "http://localhost:9200/annotation" \
  -H "Content-Type: application/json" \
  -d @../opensearch/annotation.json

# Ingest data
for f in os/*.bulk.json; do
  curl -X POST "http://localhost:9200/_bulk" \
    -H "Content-Type: application/x-ndjson" \
    --data-binary @"$f"
done


# Serve out website
npx serve site
