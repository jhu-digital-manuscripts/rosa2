#!/bin/bash
#
# package-site.sh - Build a deployable rosa-viewer site: the viewer plus the
# static IIIF Presentation files it needs, ready to copy to any static web host
# (for example the WordPress portal used for production).
#
# Output layout:
#   <output>/index.html, assets/, logo/   the viewer built for the site
#   <output>/iiif/                        IIIF files for the site's collections
#
# Usage:
#   scripts/package-site.sh --site <aor|dlmm> --site-url <url> --opensearch-url <url> [options]
#
# Options:
#   --site <id>              Site to build: aor or dlmm (required)
#   --site-url <url>         Public URL the site will be served from, e.g.
#                            https://archaeologyofreading.org/viewer (required).
#                            IIIF ids are generated under <site-url>/iiif/.
#   --opensearch-url <url>   Opensearch _search URL written to jhsearch.json (required)
#   --image-base-url <url>   IIIF Image API base URL (default: https://image.library.jhu.edu/iiif)
#   --image-api-version <n>  IIIF Image API version, 2 or 3 (default: 2)
#   --archive <dir>          Archive directory (default: <repo>/archive)
#   --jar <file>             rosa-tool jar (default: <repo>/rosa-tool/target/rosa-tool.jar)
#   --output <dir>           Output directory (default: rosa-viewer/dist/packages/<site>)
#   --tarball <file>         Also write the output directory to a .tar.gz
#   --help                   Show this help
#
# Prerequisites: Java 25, Node.js 20 with rosa-viewer dependencies installed
# (npm ci), and rosa-tool built (mvn package in rosa-tool/).

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
VIEWER_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
REPO_ROOT="$(cd "$VIEWER_DIR/.." && pwd)"

SITE=""
SITE_URL=""
OPENSEARCH_URL=""
IMAGE_BASE_URL="https://image.library.jhu.edu/iiif"
IMAGE_API_VERSION="2"
ARCHIVE="$REPO_ROOT/archive"
JAR="$REPO_ROOT/rosa-tool/target/rosa-tool.jar"
OUTPUT=""
TARBALL=""

usage() {
    sed -n '3,29p' "$0" | sed 's/^# \?//'
    exit "${1:-0}"
}

error() {
    echo "[ERROR] $1" >&2
    exit 1
}

while [[ $# -gt 0 ]]; do
    case "$1" in
        --site) SITE="$2"; shift 2 ;;
        --site-url) SITE_URL="$2"; shift 2 ;;
        --opensearch-url) OPENSEARCH_URL="$2"; shift 2 ;;
        --image-base-url) IMAGE_BASE_URL="$2"; shift 2 ;;
        --image-api-version) IMAGE_API_VERSION="$2"; shift 2 ;;
        --archive) ARCHIVE="$2"; shift 2 ;;
        --jar) JAR="$2"; shift 2 ;;
        --output) OUTPUT="$2"; shift 2 ;;
        --tarball) TARBALL="$2"; shift 2 ;;
        --help|-h) usage 0 ;;
        *) error "Unknown option: $1 (see --help)" ;;
    esac
done

# Collections each site needs from the generated IIIF tree. Everything else is
# pruned so a package only carries its own data.
case "$SITE" in
    aor) COLLECTIONS=(aor) ;;
    dlmm) COLLECTIONS=(dlmm rose pizan) ;;
    "") error "--site is required (aor or dlmm)" ;;
    *) error "Unknown site '$SITE' (expected aor or dlmm)" ;;
esac

[[ -n "$SITE_URL" ]] || error "--site-url is required"
[[ -n "$OPENSEARCH_URL" ]] || error "--opensearch-url is required"
[[ -d "$ARCHIVE" ]] || error "Archive directory not found: $ARCHIVE"
[[ -f "$JAR" ]] || error "rosa-tool jar not found: $JAR
Build it first: cd $REPO_ROOT/rosa-tool && mvn package -DskipTests"
[[ -d "$VIEWER_DIR/node_modules" ]] || error "rosa-viewer dependencies not installed. Run: cd $VIEWER_DIR && npm ci"

SITE_URL="${SITE_URL%/}"
IMAGE_BASE_URL="${IMAGE_BASE_URL%/}"
OUTPUT="${OUTPUT:-$VIEWER_DIR/dist/packages/$SITE}"
IIIF_BASE_URL="$SITE_URL/iiif"

echo "Packaging site '$SITE'"
echo "  site URL:          $SITE_URL"
echo "  IIIF base URL:     $IIIF_BASE_URL"
echo "  image base URL:    $IMAGE_BASE_URL (API v$IMAGE_API_VERSION)"
echo "  Opensearch URL:    $OPENSEARCH_URL"
echo "  output:            $OUTPUT"

rm -rf "$OUTPUT"
mkdir -p "$OUTPUT/iiif"

echo "==> Generating IIIF Presentation files"
java -jar "$JAR" generate-iiif-pres \
    --archive "$ARCHIVE" \
    --output "$OUTPUT/iiif" \
    --base-url "$IIIF_BASE_URL/" \
    --image-base-url "$IMAGE_BASE_URL" \
    --image-api-version "$IMAGE_API_VERSION" \
    --opensearch-url "$OPENSEARCH_URL"

echo "==> Keeping collections: ${COLLECTIONS[*]}"
for entry in "$OUTPUT"/iiif/*; do
    name="$(basename "$entry")"
    keep=false
    for c in "${COLLECTIONS[@]}"; do
        [[ "$name" == "$c" ]] && keep=true
    done
    if [[ "$keep" != "true" ]]; then
        rm -rf "$entry"
    fi
done

echo "==> Building the viewer"
(
    cd "$VIEWER_DIR"
    VITE_ROSA_IIIF_BASE_URL="$IIIF_BASE_URL" \
    VITE_ROSA_IMAGE_BASE_URL="$IMAGE_BASE_URL" \
    npm run --silent build:sites -- "$SITE"
)
cp -R "$VIEWER_DIR/dist/sites/$SITE/." "$OUTPUT/"

if [[ -n "$TARBALL" ]]; then
    echo "==> Writing $TARBALL"
    mkdir -p "$(dirname "$TARBALL")"
    tar -czf "$TARBALL" -C "$OUTPUT" .
fi

echo "Done. Site '$SITE' packaged in $OUTPUT"
echo "Copy its contents to $SITE_URL so that $SITE_URL/index.html and $IIIF_BASE_URL/ resolve."
