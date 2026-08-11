#!/bin/bash
#
# status.sh - Check status of the local development environment
#
# Usage:
#   ./status.sh

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# State files
PID_FILE="$SCRIPT_DIR/.server.pid"
STATE_FILE="$SCRIPT_DIR/.state"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[0;33m'
NC='\033[0m' # No Color

status_ok() { echo -e "  ${GREEN}✓${NC} $1"; }
status_warn() { echo -e "  ${YELLOW}!${NC} $1"; }
status_error() { echo -e "  ${RED}✗${NC} $1"; }

# Load configuration if available
IIIF_SERVER_PORT=3000
OPENSEARCH_PORT=9200

if [[ -f "$SCRIPT_DIR/config.env" ]]; then
    # shellcheck source=/dev/null
    source "$SCRIPT_DIR/config.env"
    IIIF_SERVER_PORT="${IIIF_SERVER_PORT:-3000}"
    OPENSEARCH_PORT="${OPENSEARCH_PORT:-9200}"
fi

echo ""
echo "Rosa Local Development Environment Status"
echo "=========================================="
echo ""

# Check IIIF server
echo "IIIF Server:"
if [[ -f "$PID_FILE" ]]; then
    pid=$(cat "$PID_FILE")
    if kill -0 "$pid" 2>/dev/null; then
        status_ok "Running (PID $pid) on port $IIIF_SERVER_PORT"
        
        # Check if actually responding
        if curl -s "http://localhost:$IIIF_SERVER_PORT/" > /dev/null 2>&1; then
            status_ok "Responding to requests"
        else
            status_warn "Not responding to HTTP requests"
        fi
    else
        status_error "Not running (stale PID file)"
    fi
else
    status_error "Not running"
fi
echo ""

# Check Opensearch
echo "Opensearch:"
if docker compose -f "$SCRIPT_DIR/docker-compose.yml" ps -q 2>/dev/null | grep -q .; then
    status_ok "Container running"
    
    # Check if responding
    if curl -s "http://localhost:$OPENSEARCH_PORT/_cluster/health" > /dev/null 2>&1; then
        health=$(curl -s "http://localhost:$OPENSEARCH_PORT/_cluster/health" | grep -o '"status":"[^"]*"' | cut -d'"' -f4)
        case "$health" in
            green)
                status_ok "Cluster health: green"
                ;;
            yellow)
                status_warn "Cluster health: yellow"
                ;;
            red)
                status_error "Cluster health: red"
                ;;
            *)
                status_warn "Cluster health: unknown"
                ;;
        esac
        
        # Check indexes
        manifest_count=$(curl -s "http://localhost:$OPENSEARCH_PORT/manifest/_count" 2>/dev/null | grep -o '"count":[0-9]*' | cut -d':' -f2 || echo "0")
        canvas_count=$(curl -s "http://localhost:$OPENSEARCH_PORT/canvas/_count" 2>/dev/null | grep -o '"count":[0-9]*' | cut -d':' -f2 || echo "0")
        
        if [[ "$manifest_count" != "0" ]] || [[ "$canvas_count" != "0" ]]; then
            status_ok "Indexes: manifest ($manifest_count docs), canvas ($canvas_count docs)"
        else
            status_warn "Indexes empty or not created"
        fi
    else
        status_warn "Not responding to HTTP requests"
    fi
else
    status_error "Container not running"
fi
echo ""

# Check generated files
echo "Generated Files:"
if [[ -d "$SCRIPT_DIR/site/iiif" ]]; then
    count=$(find "$SCRIPT_DIR/site/iiif" -name "*.json" 2>/dev/null | wc -l)
    status_ok "IIIF files: $count JSON files"
else
    status_warn "IIIF files not generated"
fi

if [[ -d "$SCRIPT_DIR/opensearch-data" ]]; then
    count=$(find "$SCRIPT_DIR/opensearch-data" -name "*.bulk.json" 2>/dev/null | wc -l)
    status_ok "Opensearch bulk files: $count files"
else
    status_warn "Opensearch bulk files not generated"
fi
echo ""

# Show URLs if running
if [[ -f "$PID_FILE" ]] && kill -0 "$(cat "$PID_FILE")" 2>/dev/null; then
    echo "URLs:"
    echo "  IIIF Collection: http://localhost:$IIIF_SERVER_PORT/iiif/collection.json"
    echo "  Opensearch:      http://localhost:$OPENSEARCH_PORT/"
    echo ""
fi

# Show state if available
if [[ -f "$STATE_FILE" ]]; then
    echo "Started: $(grep STARTED "$STATE_FILE" | cut -d= -f2)"
    echo ""
fi
