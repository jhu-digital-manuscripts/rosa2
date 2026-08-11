#!/bin/bash
#
# index-archive.sh - Generate IIIF Presentation API files from a rosa archive
#
# This script uses rosa-tool.jar to generate static IIIF files that can be
# served by any web server for use with IIIF viewers.
#
# Prerequisites:
#   - Java 25+ installed
#   - rosa-tool.jar built (run 'mvn package' in rosa-tool/)
#
# Usage:
#   ./index-archive.sh --archive <path> --output <path> [options]
#
# Options:
#   --archive <path>       Path to the rosa archive directory (required)
#   --output <path>        Output directory for IIIF files (required)
#   --base-url <url>       Base URL for IIIF resource IDs (optional)
#   --image-api-version <2|3>  IIIF Image API version (default: 2)
#   --image-base-url <url> Base URL for image server (optional)
#   --opensearch-url <url> Opensearch URL to embed in manifests (optional)
#   --jar <path>           Path to rosa-tool.jar (default: rosa-tool/target/rosa-tool.jar)
#   --help                 Show this help message

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

# Default values
JAR_PATH="$REPO_ROOT/rosa-tool/target/rosa-tool.jar"
IMAGE_API_VERSION="2"

# Arguments
ARCHIVE=""
OUTPUT=""
BASE_URL=""
IMAGE_BASE_URL=""
OPENSEARCH_URL=""

usage() {
    sed -n '3,19p' "$0" | sed 's/^# \?//'
    exit "${1:-0}"
}

error() {
    echo "Error: $1" >&2
    exit 1
}

# Parse command line arguments
while [[ $# -gt 0 ]]; do
    case "$1" in
        --archive)
            ARCHIVE="$2"
            shift 2
            ;;
        --output)
            OUTPUT="$2"
            shift 2
            ;;
        --base-url)
            BASE_URL="$2"
            shift 2
            ;;
        --image-api-version)
            IMAGE_API_VERSION="$2"
            shift 2
            ;;
        --image-base-url)
            IMAGE_BASE_URL="$2"
            shift 2
            ;;
        --opensearch-url)
            OPENSEARCH_URL="$2"
            shift 2
            ;;
        --jar)
            JAR_PATH="$2"
            shift 2
            ;;
        --help|-h)
            usage 0
            ;;
        *)
            error "Unknown option: $1"
            ;;
    esac
done

# Validate required arguments
[[ -z "$ARCHIVE" ]] && error "Missing required argument: --archive"
[[ -z "$OUTPUT" ]] && error "Missing required argument: --output"

# Validate archive directory exists
[[ ! -d "$ARCHIVE" ]] && error "Archive directory not found: $ARCHIVE"

# Validate JAR exists
if [[ ! -f "$JAR_PATH" ]]; then
    error "rosa-tool.jar not found at: $JAR_PATH
    
Please build the JAR first:
    cd $REPO_ROOT/rosa-tool && mvn package"
fi

# Validate Java is available
if ! command -v java &> /dev/null; then
    error "Java not found. Please install Java 25 or later."
fi

# Build the command
CMD=(java -jar "$JAR_PATH" generate-iiif-pres
    --archive "$ARCHIVE"
    --output "$OUTPUT"
    --image-api-version "$IMAGE_API_VERSION"
)

[[ -n "$BASE_URL" ]] && CMD+=(--base-url "$BASE_URL")
[[ -n "$IMAGE_BASE_URL" ]] && CMD+=(--image-base-url "$IMAGE_BASE_URL")
[[ -n "$OPENSEARCH_URL" ]] && CMD+=(--opensearch-url "$OPENSEARCH_URL")

# Create output directory if needed
mkdir -p "$OUTPUT"

echo "Generating IIIF Presentation API files..."
echo "  Archive: $ARCHIVE"
echo "  Output:  $OUTPUT"
echo "  Image API Version: $IMAGE_API_VERSION"
[[ -n "$BASE_URL" ]] && echo "  Base URL: $BASE_URL"
[[ -n "$IMAGE_BASE_URL" ]] && echo "  Image Base URL: $IMAGE_BASE_URL"
[[ -n "$OPENSEARCH_URL" ]] && echo "  Opensearch URL: $OPENSEARCH_URL"
echo ""

"${CMD[@]}"

echo ""
echo "IIIF generation complete. Files written to: $OUTPUT"
