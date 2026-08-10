#! /bin/sh

# Generate IIIF data
java -jar ../rosa-tool/target/rosa2-2.0.0-SNAPSHOT.jar generate-iiif-pres --archive=../../rosa-archive/ --output=site/iiif --image-api-version=2 --image-base-url=https://image.library.jhu.edu/iiif/ --base-url=http://localhost:3000/iiif/ --opensearch-url http://localhost:9200/_search

# Generate Opensearch data for bulk ingest
java -jar ../rosa-tool/target/rosa2-2.0.0-SNAPSHOT.jar generate-opensearch-ingest --archive=../../rosa-archive/  --output os

# Start Opensearch

docker compose up -d

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

# Ingest data in chunks to avoid circuit breaker limits
CHUNK_DIR=os/split
mkdir $CHUNK_DIR
for f in os/*.bulk.json; do
  echo "Splitting $f..."
  split -l 2000 "$f" "$CHUNK_DIR/chunk_"
  for chunk in "$CHUNK_DIR"/chunk_*; do
    curl -s -X POST "http://localhost:9200/_bulk" \
      -H "Content-Type: application/x-ndjson" \
      --data-binary @"$chunk"
    echo ""
    sleep 0.5
  done
  rm -f "$CHUNK_DIR"/chunk_*
done
rm -rf "$CHUNK_DIR"

# Serve out website
npx serve site
