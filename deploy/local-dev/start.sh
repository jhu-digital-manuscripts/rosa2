#!/bin/bash
#
# start.sh - Start the local development environment for rosa
#
# This script:
#   1. Generates IIIF Presentation API files from the archive
#   2. Generates Opensearch bulk ingest files
#   3. Starts Opensearch in Docker
#   4. Creates indexes and ingests data
#   5. Starts a static file server for IIIF files
#
# Prerequisites:
#   - Java 25+ installed
#   - Docker and docker compose installed
#   - rosa-tool.jar built (run 'mvn package' in rosa-tool/)
#   - config.env configured with ROSA_ARCHIVE path
#
# After starting, run rosa-viewer with 'npm run dev' in the rosa-viewer/ directory
#
# Usage:
#   ./start.sh [--skip-generate] [--skip-ingest]
#
# Options:
#   --skip-generate  Skip IIIF and Opensearch file generation (use existing files)
#   --skip-ingest    Skip Opensearch index creation and data ingest
#   --help           Show this help message

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
DEPLOY_DIR="$REPO_ROOT/deploy"

# State files
PID_FILE="$SCRIPT_DIR/.server.pid"
STATE_FILE="$SCRIPT_DIR/.state"

# Defaults
SKIP_GENERATE=false
SKIP_INGEST=false

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[0;33m'
NC='\033[0m' # No Color

log_info() { echo -e "${GREEN}[INFO]${NC} $1"; }
log_warn() { echo -e "${YELLOW}[WARN]${NC} $1"; }
log_error() { echo -e "${RED}[ERROR]${NC} $1" >&2; }

error() {
    log_error "$1"
    exit 1
}

usage() {
    sed -n '3,23p' "$0" | sed 's/^# \?//'
    exit "${1:-0}"
}

# Parse arguments
while [[ $# -gt 0 ]]; do
    case "$1" in
        --skip-generate)
            SKIP_GENERATE=true
            shift
            ;;
        --skip-ingest)
            SKIP_INGEST=true
            shift
            ;;
        --help|-h)
            usage 0
            ;;
        *)
            error "Unknown option: $1"
            ;;
    esac
done

# Load configuration
CONFIG_FILE="$SCRIPT_DIR/config.env"
if [[ ! -f "$CONFIG_FILE" ]]; then
    error "Configuration file not found: $CONFIG_FILE

Please create config.env from the template and set ROSA_ARCHIVE."
fi

# shellcheck source=/dev/null
source "$CONFIG_FILE"

# Validate configuration
[[ -z "${ROSA_ARCHIVE:-}" ]] && error "ROSA_ARCHIVE not set in config.env"
[[ ! -d "$ROSA_ARCHIVE" ]] && error "Archive directory not found: $ROSA_ARCHIVE"

# Set defaults for optional config
IMAGE_API_VERSION="${IMAGE_API_VERSION:-2}"
IMAGE_BASE_URL="${IMAGE_BASE_URL:-}"
IIIF_SERVER_PORT="${IIIF_SERVER_PORT:-3000}"
OPENSEARCH_PORT="${OPENSEARCH_PORT:-9200}"
OPENSEARCH_URL="${OPENSEARCH_URL:-http://localhost:$OPENSEARCH_PORT/_search}"

# Directories
SITE_DIR="$SCRIPT_DIR/site"
IIIF_DIR="$SITE_DIR/iiif"
OS_DIR="$SCRIPT_DIR/opensearch-data"
JAR_PATH="$REPO_ROOT/rosa-tool/target/rosa-tool.jar"

# Check prerequisites
check_prerequisites() {
    log_info "Checking prerequisites..."
    
    if ! command -v java &> /dev/null; then
        error "Java not found. Please install Java 25 or later."
    fi
    
    if ! command -v docker &> /dev/null; then
        error "Docker not found. Please install Docker."
    fi
    
    if ! docker compose version &> /dev/null; then
        error "Docker Compose not found. Please install Docker Compose."
    fi
    
    if [[ ! -f "$JAR_PATH" ]]; then
        error "rosa-tool.jar not found at: $JAR_PATH

Please build the JAR first:
    cd $REPO_ROOT/rosa-tool && mvn package"
    fi
    
    log_info "Prerequisites OK"
}

# Generate IIIF files
generate_iiif() {
    log_info "Generating IIIF Presentation API files..."
    
    mkdir -p "$IIIF_DIR"
    
    local cmd=(java -jar "$JAR_PATH" generate-iiif-pres
        --archive "$ROSA_ARCHIVE"
        --output "$IIIF_DIR"
        --image-api-version "$IMAGE_API_VERSION"
        --base-url "http://localhost:$IIIF_SERVER_PORT/iiif/"
        --opensearch-url "$OPENSEARCH_URL"
    )
    
    [[ -n "$IMAGE_BASE_URL" ]] && cmd+=(--image-base-url "$IMAGE_BASE_URL")
    
    "${cmd[@]}"
    
    log_info "IIIF files generated in $IIIF_DIR"
}

# Generate Opensearch bulk ingest files
generate_opensearch() {
    log_info "Generating Opensearch bulk ingest files..."
    
    mkdir -p "$OS_DIR"
    
    java -jar "$JAR_PATH" generate-opensearch-ingest \
        --archive "$ROSA_ARCHIVE" \
        --output "$OS_DIR"
    
    log_info "Opensearch bulk files generated in $OS_DIR"
}

# Start Opensearch
start_opensearch() {
    log_info "Starting Opensearch..."
    
    docker compose -f "$SCRIPT_DIR/docker-compose.yml" up -d
    
    log_info "Waiting for Opensearch to be ready..."
    local max_attempts=30
    local attempt=0
    
    while [[ $attempt -lt $max_attempts ]]; do
        if curl -s "http://localhost:$OPENSEARCH_PORT/_cluster/health" > /dev/null 2>&1; then
            log_info "Opensearch is ready"
            return 0
        fi
        attempt=$((attempt + 1))
        echo -n "."
        sleep 2
    done
    
    echo ""
    error "Opensearch failed to start within 60 seconds"
}

# Create Opensearch indexes
create_indexes() {
    log_info "Creating Opensearch indexes..."
    
    local index_dir="$DEPLOY_DIR/opensearch"
    
    # Delete existing indexes (ignore errors if they don't exist)
    curl -s -X DELETE "http://localhost:$OPENSEARCH_PORT/manifest" > /dev/null 2>&1 || true
    curl -s -X DELETE "http://localhost:$OPENSEARCH_PORT/canvas" > /dev/null 2>&1 || true
    
    # Create indexes
    curl -s -X PUT "http://localhost:$OPENSEARCH_PORT/manifest" \
        -H "Content-Type: application/json" \
        -d @"$index_dir/manifest.json" > /dev/null
    
    curl -s -X PUT "http://localhost:$OPENSEARCH_PORT/canvas" \
        -H "Content-Type: application/json" \
        -d @"$index_dir/canvas.json" > /dev/null
    
    log_info "Indexes created"
}

# Ingest data into Opensearch
ingest_data() {
    log_info "Ingesting data into Opensearch..."
    
    # Create temp directory for chunked files
    local chunk_dir="$OS_DIR/chunks"
    mkdir -p "$chunk_dir"
    
    for f in "$OS_DIR"/*.bulk.json; do
        [[ -f "$f" ]] || continue
        
        local filename
        filename=$(basename "$f")
        log_info "  Processing $filename..."
        
        # Split into chunks to avoid circuit breaker limits
        split -l 2000 "$f" "$chunk_dir/chunk_"
        
        for chunk in "$chunk_dir"/chunk_*; do
            [[ -f "$chunk" ]] || continue
            curl -s -X POST "http://localhost:$OPENSEARCH_PORT/_bulk" \
                -H "Content-Type: application/x-ndjson" \
                --data-binary @"$chunk" > /dev/null
            sleep 0.3
        done
        
        rm -f "$chunk_dir"/chunk_*
    done
    
    rmdir "$chunk_dir" 2>/dev/null || true
    
    log_info "Data ingestion complete"
}

# Start the IIIF file server
start_iiif_server() {
    log_info "Starting IIIF file server on port $IIIF_SERVER_PORT..."
    
    # Check if server is already running
    if [[ -f "$PID_FILE" ]]; then
        local old_pid
        old_pid=$(cat "$PID_FILE")
        if kill -0 "$old_pid" 2>/dev/null; then
            log_warn "Server already running (PID $old_pid)"
            return 0
        fi
        rm -f "$PID_FILE"
    fi
    
    # Start serve in background
    cd "$SITE_DIR"
    npx serve -l "$IIIF_SERVER_PORT" --cors > "$SCRIPT_DIR/server.log" 2>&1 &
    local pid=$!
    echo "$pid" > "$PID_FILE"
    
    # Wait for server to be ready
    sleep 2
    if ! kill -0 "$pid" 2>/dev/null; then
        error "Failed to start IIIF server. Check $SCRIPT_DIR/server.log"
    fi
    
    log_info "IIIF server started (PID $pid)"
}

# Save state
save_state() {
    cat > "$STATE_FILE" << EOF
STARTED=$(date -Iseconds)
IIIF_SERVER_PORT=$IIIF_SERVER_PORT
OPENSEARCH_PORT=$OPENSEARCH_PORT
EOF
}

# Main
main() {
    echo ""
    echo "========================================"
    echo "  Rosa Local Development Environment"
    echo "========================================"
    echo ""
    
    check_prerequisites
    
    if [[ "$SKIP_GENERATE" != "true" ]]; then
        generate_iiif
        generate_opensearch
    else
        log_info "Skipping file generation (--skip-generate)"
    fi
    
    start_opensearch
    
    if [[ "$SKIP_INGEST" != "true" ]]; then
        create_indexes
        ingest_data
    else
        log_info "Skipping index creation and ingest (--skip-ingest)"
    fi
    
    start_iiif_server
    save_state
    
    echo ""
    echo "========================================"
    log_info "Local dev environment is running!"
    echo ""
    echo "  IIIF Server:   http://localhost:$IIIF_SERVER_PORT/iiif/"
    echo "  Opensearch:    http://localhost:$OPENSEARCH_PORT/"
    echo ""
    echo "  To run rosa-viewer:"
    echo "    cd $REPO_ROOT/rosa-viewer"
    echo "    npm run dev"
    echo ""
    echo "  Then open: http://localhost:3001"
    echo ""
    echo "  To stop: ./stop.sh"
    echo "========================================"
    echo ""
}

main "$@"
