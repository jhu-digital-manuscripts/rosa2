#! /bin/sh

# Generate IIIF data
java -jar ../target/rosa2-2.0.0-SNAPSHOT.jar generate-iiif-pres --archive=../../rosa-archive/ --output=site/iiif --image-api-version=2 --image-base-url=https://image.library.jhu.edu/iiif/ --base-url=http://localhost:3000/iiif/

# Serve out website
npx serve site
