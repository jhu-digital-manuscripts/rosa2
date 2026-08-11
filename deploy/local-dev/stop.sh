#!/bin/bash
#
# stop.sh - Stop the local development environment for rosa
#
# This script stops all services started by start.sh:
#   - IIIF static file server
#   - Opensearch Docker container
#
# Usage:
#   ./stop.sh [--clean]
#
# Options:
#   --clean   Also remove generated files (site/, opensearch-data/)
#   --help    Show this help message

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# State files
PID_FILE="$SCRIPT_DIR/.server.pid"
STATE_FILE="$SCRIPT_DIR/.state"

# Options
CLEAN=false

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[0;33m'
NC='\033[0m' # No Color

log_info() { echo -e "${GREEN}[INFO]${NC} $1"; }
log_warn() { echo -e "${YELLOW}[WARN]${NC} $1"; }
log_error() { echo -e "${RED}[ERROR]${NC} $1" >&2; }

usage() {
    sed -n '3,14p' "$0" | sed 's/^# \?//'
    exit "${1:-0}"
}

# Parse arguments
while [[ $# -gt 0 ]]; do
    case "$1" in
        --clean)
            CLEAN=true
            shift
            ;;
        --help|-h)
            usage 0
            ;;
        *)
            log_error "Unknown option: $1"
            usage 1
            ;;
    esac
done

# Stop IIIF server
stop_server() {
    if [[ -f "$PID_FILE" ]]; then
        local pid
        pid=$(cat "$PID_FILE")
        
        if kill -0 "$pid" 2>/dev/null; then
            log_info "Stopping IIIF server (PID $pid)..."
            kill "$pid" 2>/dev/null || true
            
            # Wait for process to exit
            local count=0
            while kill -0 "$pid" 2>/dev/null && [[ $count -lt 10 ]]; do
                sleep 0.5
                count=$((count + 1))
            done
            
            # Force kill if still running
            if kill -0 "$pid" 2>/dev/null; then
                log_warn "Force killing server..."
                kill -9 "$pid" 2>/dev/null || true
            fi
            
            log_info "IIIF server stopped"
        else
            log_info "IIIF server not running (stale PID file)"
        fi
        
        rm -f "$PID_FILE"
    else
        log_info "No IIIF server PID file found"
    fi
}

# Stop Opensearch
stop_opensearch() {
    if docker compose -f "$SCRIPT_DIR/docker-compose.yml" ps -q 2>/dev/null | grep -q .; then
        log_info "Stopping Opensearch..."
        docker compose -f "$SCRIPT_DIR/docker-compose.yml" down
        log_info "Opensearch stopped"
    else
        log_info "Opensearch not running"
    fi
}

# Clean generated files
clean_files() {
    log_info "Cleaning generated files..."
    
    rm -rf "$SCRIPT_DIR/site"
    rm -rf "$SCRIPT_DIR/opensearch-data"
    rm -f "$SCRIPT_DIR/server.log"
    rm -f "$STATE_FILE"
    
    log_info "Generated files removed"
}

# Main
main() {
    echo ""
    log_info "Stopping local development environment..."
    echo ""
    
    stop_server
    stop_opensearch
    
    if [[ "$CLEAN" == "true" ]]; then
        clean_files
    fi
    
    rm -f "$STATE_FILE"
    
    echo ""
    log_info "Local dev environment stopped"
    echo ""
}

main "$@"
