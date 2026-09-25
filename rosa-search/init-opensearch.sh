#!/bin/bash
#
# init-opensearch.sh - Initialize the AWS OpenSearch indexes for rosa2
#
# This script:
#   1. Reads the deployed infrastructure's details from the OpenTofu workspace
#   2. Generates Opensearch bulk ingest files from the archive
#   3. Rewrites the index definitions to reference the Latin analyzer packages
#   4. Stages the definitions and bulk files in S3
#   5. Drops and recreates the manifest and canvas indexes
#   6. Ingests the bulk data
#   7. Verifies the document counts and the Latin analyzer
#
# The Opensearch domain has no public endpoint, so steps 5 through 7 run through
# the admin Lambda in the VPC rather than against the domain directly. That is
# why this works from any machine with AWS credentials and no bastion host.
#
# Prerequisites:
#   - Java 25+, the AWS CLI v2, jq, and OpenTofu
#   - rosa-tool.jar built (run 'mvn package' in rosa-tool/)
#   - jhu-rosa2-tf applied, and initialized for the environment being targeted
#   - Credentials with the permissions in the ingest_operator_policy_arn output
#
# Usage:
#   ./init-opensearch.sh --env <stage|prod> [options]
#
# Options:
#   --env ENV         Target environment: stage or prod. Required.
#   --skip-generate   Reuse the bulk files already in build/opensearch-data
#   --skip-upload     Do not stage files to S3, use what is already there
#   --skip-index      Do not drop and recreate the indexes
#   --skip-ingest     Do not ingest bulk data
#   --archive DIR     Archive directory (default: ../archive)
#   --tf-dir DIR      OpenTofu workspace (default: ../jhu-rosa2-tf)
#   --yes             Do not prompt before dropping indexes
#   --help            Show this help message
#
# Recreating an index deletes every document in it. The script prompts before
# doing so unless --yes is given.

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

# Defaults
ENVIRONMENT=""
SKIP_GENERATE=false
SKIP_UPLOAD=false
SKIP_INDEX=false
SKIP_INGEST=false
ASSUME_YES=false
ARCHIVE_DIR="$REPO_ROOT/archive"
TF_DIR="$REPO_ROOT/jhu-rosa2-tf"

BUILD_DIR="$SCRIPT_DIR/build"
BULK_DIR="$BUILD_DIR/opensearch-data"
DEFS_DIR="$BUILD_DIR/index-defs"
JAR_PATH="$REPO_ROOT/rosa-tool/target/rosa-tool.jar"
INDEX_SRC_DIR="$SCRIPT_DIR/opensearch"

INDEXES=(manifest canvas)

# Local-dev paths in the checked-in index definitions that must be replaced with
# the AWS custom package IDs.
LOCAL_STEMMER_PATH="analyzers/latin-stemmer-rules.txt"
LOCAL_CHARMAP_PATH="analyzers/latin-charmap.txt"

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
    sed -n '3,39p' "$0" | sed 's/^# \?//'
    exit "${1:-0}"
}

TMP_DIR=""

# Must always succeed. The exit status of an EXIT trap's last command replaces
# the script's own, so a failing test here would turn every clean run into a
# non-zero exit.
cleanup() {
    if [[ -n "$TMP_DIR" && -d "$TMP_DIR" ]]; then
        rm -rf "$TMP_DIR"
    fi
    return 0
}
trap cleanup EXIT

# Parse arguments
while [[ $# -gt 0 ]]; do
    case "$1" in
        --env)
            [[ $# -ge 2 ]] || error "--env requires a value"
            ENVIRONMENT="$2"
            shift 2
            ;;
        --skip-generate)
            SKIP_GENERATE=true
            shift
            ;;
        --skip-upload)
            SKIP_UPLOAD=true
            shift
            ;;
        --skip-index)
            SKIP_INDEX=true
            shift
            ;;
        --skip-ingest)
            SKIP_INGEST=true
            shift
            ;;
        --archive)
            [[ $# -ge 2 ]] || error "--archive requires a value"
            ARCHIVE_DIR="$2"
            shift 2
            ;;
        --tf-dir)
            [[ $# -ge 2 ]] || error "--tf-dir requires a value"
            TF_DIR="$2"
            shift 2
            ;;
        --yes|-y)
            ASSUME_YES=true
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

[[ -z "$ENVIRONMENT" ]] && error "--env is required (stage or prod)

Run '$0 --help' for usage."

case "$ENVIRONMENT" in
    stage|prod) ;;
    *) error "--env must be 'stage' or 'prod', got '$ENVIRONMENT'" ;;
esac

# Check prerequisites
check_prerequisites() {
    log_info "Checking prerequisites..."

    local missing=()
    command -v aws  &> /dev/null || missing+=("aws (AWS CLI v2)")
    command -v jq   &> /dev/null || missing+=("jq")
    command -v tofu &> /dev/null || missing+=("tofu (OpenTofu)")

    if [[ "$SKIP_GENERATE" != "true" ]]; then
        command -v java &> /dev/null || missing+=("java (25 or later)")
    fi

    if [[ ${#missing[@]} -gt 0 ]]; then
        error "Missing required commands: ${missing[*]}"
    fi

    if [[ "$SKIP_GENERATE" != "true" ]]; then
        [[ -f "$JAR_PATH" ]] || error "rosa-tool.jar not found at: $JAR_PATH

Build it first:
    cd $REPO_ROOT/rosa-tool && mvn package"

        [[ -d "$ARCHIVE_DIR" ]] || error "Archive directory not found: $ARCHIVE_DIR"
    fi

    for index in "${INDEXES[@]}"; do
        [[ -f "$INDEX_SRC_DIR/$index.json" ]] \
            || error "Index definition not found: $INDEX_SRC_DIR/$index.json"
    done

    [[ -d "$TF_DIR" ]] || error "OpenTofu workspace not found: $TF_DIR"

    if ! aws sts get-caller-identity &> /dev/null; then
        error "No usable AWS credentials. Configure them and try again."
    fi

    log_info "Prerequisites OK"
}

# Read the deployed infrastructure's details from the OpenTofu workspace.
#
# The workspace holds prod and stage in separate state files selected at init
# time, so the environment recorded in state is checked against --env. Without
# that check, running this after initializing the other backend would rebuild the
# wrong environment's indexes.
load_config() {
    log_info "Reading configuration from $TF_DIR..."

    local outputs
    if ! outputs=$(tofu -chdir="$TF_DIR" output -json 2>/dev/null); then
        error "Could not read OpenTofu outputs from $TF_DIR

Initialize the workspace for this environment first:
    cd $TF_DIR
    tofu init -reconfigure -backend-config=$ENVIRONMENT.s3.tfbackend"
    fi

    if [[ "$(jq -r 'length' <<< "$outputs")" == "0" ]]; then
        error "The OpenTofu workspace has no outputs. Has it been applied?

    cd $TF_DIR
    tofu apply -var-file=$ENVIRONMENT.tfvars"
    fi

    local state_env
    state_env=$(jq -r '.environment.value // empty' <<< "$outputs")

    if [[ "$state_env" != "$ENVIRONMENT" ]]; then
        error "The workspace in $TF_DIR is initialized for '$state_env', not '$ENVIRONMENT'.

Switch backends before continuing:
    cd $TF_DIR
    tofu init -reconfigure -backend-config=$ENVIRONMENT.s3.tfbackend"
    fi

    AWS_REGION_OUT=$(jq -r '.aws_region.value' <<< "$outputs")
    DOMAIN_NAME=$(jq -r '.opensearch_domain_name.value' <<< "$outputs")
    ADMIN_LAMBDA=$(jq -r '.admin_lambda_name.value' <<< "$outputs")
    INGEST_BUCKET=$(jq -r '.ingest_bucket.value' <<< "$outputs")
    INGEST_PREFIX=$(jq -r '.ingest_prefix.value' <<< "$outputs")
    STEMMER_PATH=$(jq -r '.latin_stemmer_rules_path.value' <<< "$outputs")
    CHARMAP_PATH=$(jq -r '.latin_charmap_path.value' <<< "$outputs")
    SEARCH_URL=$(jq -r '.search_url.value' <<< "$outputs")

    for name in AWS_REGION_OUT DOMAIN_NAME ADMIN_LAMBDA INGEST_BUCKET INGEST_PREFIX \
                STEMMER_PATH CHARMAP_PATH SEARCH_URL; do
        [[ -n "${!name}" && "${!name}" != "null" ]] \
            || error "OpenTofu output for $name is missing. Re-apply $TF_DIR."
    done

    log_info "  environment:   $ENVIRONMENT"
    log_info "  region:        $AWS_REGION_OUT"
    log_info "  domain:        $DOMAIN_NAME"
    log_info "  admin lambda:  $ADMIN_LAMBDA"
    log_info "  staging:       s3://$INGEST_BUCKET/$INGEST_PREFIX"
    log_info "  latin stemmer: $STEMMER_PATH"
    log_info "  latin charmap: $CHARMAP_PATH"
}

# Confirm before dropping indexes.
confirm_destructive() {
    if [[ "$SKIP_INDEX" == "true" ]]; then
        return 0
    fi

    if [[ "$ASSUME_YES" == "true" ]]; then
        log_warn "Dropping and recreating ${INDEXES[*]} on $DOMAIN_NAME (--yes given)"
        return 0
    fi

    echo ""
    log_warn "This will DELETE and recreate the '${INDEXES[*]}' indexes on"
    log_warn "domain '$DOMAIN_NAME' ($ENVIRONMENT). All indexed documents will be lost."
    echo ""
    local reply
    read -r -p "Type the environment name ('$ENVIRONMENT') to continue: " reply
    if [[ "$reply" != "$ENVIRONMENT" ]]; then
        error "Aborted."
    fi
}

# Invoke the admin Lambda with a JSON payload and echo its result.
#
# The Lambda reports failures in its response body rather than raising, so both
# the invocation metadata and the payload have to be checked.
invoke_admin() {
    local payload="$1"
    local out="$TMP_DIR/response.json"
    local meta="$TMP_DIR/metadata.json"

    # --cli-read-timeout 0 matters: the default is 60 seconds, and a bulk ingest
    # invocation can run for the Lambda's full 15 minute timeout.
    if ! aws lambda invoke \
            --function-name "$ADMIN_LAMBDA" \
            --region "$AWS_REGION_OUT" \
            --cli-binary-format raw-in-base64-out \
            --cli-read-timeout 0 \
            --no-cli-pager \
            --payload "$payload" \
            "$out" > "$meta" 2> "$TMP_DIR/invoke.err"; then
        log_error "aws lambda invoke failed:"
        cat "$TMP_DIR/invoke.err" >&2
        return 1
    fi

    local function_error
    function_error=$(jq -r '.FunctionError // empty' "$meta")
    if [[ -n "$function_error" ]]; then
        log_error "$ADMIN_LAMBDA raised $function_error:"
        cat "$out" >&2
        return 1
    fi

    if [[ "$(jq -r '.ok // false' "$out")" != "true" ]]; then
        log_error "$ADMIN_LAMBDA reported an error:"
        jq -r '.error // "unknown error"' "$out" >&2
        return 1
    fi

    cat "$out"
}

# Generate Opensearch bulk ingest files
generate_bulk_data() {
    log_info "Generating Opensearch bulk ingest files from $ARCHIVE_DIR..."

    rm -rf "$BULK_DIR"
    mkdir -p "$BULK_DIR"

    java -jar "$JAR_PATH" generate-opensearch-ingest \
        --archive "$ARCHIVE_DIR" \
        --output "$BULK_DIR"

    local count
    count=$(find "$BULK_DIR" -name '*.bulk.json' -type f | wc -l)
    [[ "$count" -gt 0 ]] || error "No bulk files were generated in $BULK_DIR"

    log_info "Generated $count bulk file(s):"
    local f
    for f in "$BULK_DIR"/*.bulk.json; do
        log_info "  $(basename "$f") ($(du -h "$f" | cut -f1))"
    done
}

# Rewrite the index definitions for AWS.
#
# The checked-in definitions reference the Latin dictionary files by filename,
# which is how they are bind-mounted in local-dev. AWS exposes them as custom
# packages addressed by ID instead, so every occurrence of the local path is
# replaced. The definitions in rosa-search/opensearch are left untouched so that
# local-dev keeps working.
prepare_index_definitions() {
    log_info "Rewriting index definitions for the Latin analyzer packages..."

    rm -rf "$DEFS_DIR"
    mkdir -p "$DEFS_DIR"

    local index src dst
    for index in "${INDEXES[@]}"; do
        src="$INDEX_SRC_DIR/$index.json"
        dst="$DEFS_DIR/$index.json"

        jq --arg stemmer "$STEMMER_PATH" \
           --arg charmap "$CHARMAP_PATH" \
           --arg local_stemmer "$LOCAL_STEMMER_PATH" \
           --arg local_charmap "$LOCAL_CHARMAP_PATH" '
            walk(
                if type == "string" then
                    if . == $local_stemmer then $stemmer
                    elif . == $local_charmap then $charmap
                    else . end
                else . end
            )
        ' "$src" > "$dst" || error "Failed to rewrite $src"

        # A leftover local path means the definition changed shape and the
        # analyzer would fail to load, so fail here rather than at index creation.
        if grep -q 'analyzers/latin-' "$dst"; then
            error "$dst still references a local analyzer path.

Expected to replace '$LOCAL_STEMMER_PATH' and '$LOCAL_CHARMAP_PATH'.
Check the analyzer paths in $src."
        fi

        # Confirm the package paths actually landed in the output.
        grep -q "$STEMMER_PATH" "$dst" \
            || error "$dst does not reference the stemmer package $STEMMER_PATH"
        grep -q "$CHARMAP_PATH" "$dst" \
            || error "$dst does not reference the charmap package $CHARMAP_PATH"

        log_info "  $index.json rewritten"
    done
}

# Stage the definitions and bulk files in S3 for the admin Lambda to read.
upload_staging_data() {
    log_info "Staging files in s3://$INGEST_BUCKET/$INGEST_PREFIX/..."

    # Clear the previous run's bulk files so a collection that no longer exists
    # is not re-ingested from a stale object.
    aws s3 rm "s3://$INGEST_BUCKET/$INGEST_PREFIX/bulk/" \
        --recursive \
        --region "$AWS_REGION_OUT" \
        --only-show-errors || true

    local index
    for index in "${INDEXES[@]}"; do
        aws s3 cp "$DEFS_DIR/$index.json" \
            "s3://$INGEST_BUCKET/$INGEST_PREFIX/index-defs/$index.json" \
            --region "$AWS_REGION_OUT" \
            --sse AES256 \
            --only-show-errors
        log_info "  index-defs/$index.json"
    done

    local f
    for f in "$BULK_DIR"/*.bulk.json; do
        [[ -f "$f" ]] || continue
        aws s3 cp "$f" \
            "s3://$INGEST_BUCKET/$INGEST_PREFIX/bulk/$(basename "$f")" \
            --region "$AWS_REGION_OUT" \
            --sse AES256 \
            --only-show-errors
        log_info "  bulk/$(basename "$f")"
    done
}

# Confirm the domain is reachable and healthy before touching indexes.
check_cluster() {
    log_info "Checking cluster health..."

    local result status
    result=$(invoke_admin '{"action":"health"}') \
        || error "Could not reach the Opensearch domain through $ADMIN_LAMBDA.

Check that the Lambda is attached to the VPC and that its security group is
permitted by the domain's security group. An error of \"[Errno 16] Device or
resource busy\" after about 20 seconds means the endpoint could not be resolved:
set dns_server_cidrs to the VPC's DNS servers."

    status=$(jq -r '.result.status' <<< "$result")
    log_info "  cluster status: $status"

    if [[ "$status" == "red" ]]; then
        error "Cluster status is red. Resolve that before reindexing."
    fi
}

# Drop and recreate the indexes.
recreate_indexes() {
    log_info "Recreating indexes..."

    local index result
    for index in "${INDEXES[@]}"; do
        local payload
        payload=$(jq -nc \
            --arg index "$index" \
            --arg key "$INGEST_PREFIX/index-defs/$index.json" \
            '{action:"recreate_index", index:$index, definition_key:$key}')

        if ! result=$(invoke_admin "$payload"); then
            error "Failed to create index '$index'.

If the error mentions mappings_path or rules_path, the Latin analyzer packages
are not readable by the domain. Confirm both package associations are active:

    aws opensearch list-packages-for-domain \\
        --domain-name $DOMAIN_NAME --region $AWS_REGION_OUT"
        fi

        if [[ "$(jq -r '.result.deleted' <<< "$result")" == "true" ]]; then
            log_info "  $index: dropped and recreated"
        else
            log_info "  $index: created"
        fi
    done
}

# Ingest the staged bulk files.
#
# Each invocation processes as much of a file as it can before its time budget
# runs out and reports where to resume, so a file larger than one invocation can
# handle still completes.
ingest_bulk_data() {
    log_info "Ingesting bulk data..."

    local total_indexed=0
    local total_failed=0
    local f

    for f in "$BULK_DIR"/*.bulk.json; do
        [[ -f "$f" ]] || continue

        local name key start_byte indexed failed done_flag next_byte result rounds file_indexed
        name=$(basename "$f")
        key="$INGEST_PREFIX/bulk/$name"
        start_byte=0
        rounds=0
        file_indexed=0

        log_info "  $name..."

        while true; do
            rounds=$((rounds + 1))
            if [[ "$rounds" -gt 200 ]]; then
                error "Ingest of $name did not finish after 200 invocations."
            fi

            local payload
            payload=$(jq -nc \
                --arg key "$key" \
                --argjson start "$start_byte" \
                '{action:"bulk_ingest", key:$key, start_byte:$start}')

            result=$(invoke_admin "$payload") \
                || error "Bulk ingest of $name failed at byte $start_byte"

            indexed=$(jq -r '.result.indexed' <<< "$result")
            failed=$(jq -r '.result.failed' <<< "$result")
            done_flag=$(jq -r '.result.done' <<< "$result")
            next_byte=$(jq -r '.result.next_byte' <<< "$result")

            file_indexed=$((file_indexed + indexed))
            total_indexed=$((total_indexed + indexed))
            total_failed=$((total_failed + failed))

            if [[ "$failed" -gt 0 ]]; then
                log_warn "    $failed document(s) rejected: $(jq -r '.result.first_error // "no detail"' <<< "$result")"
            fi

            if [[ "$done_flag" == "true" ]]; then
                break
            fi

            # Guard against a loop that cannot make progress.
            if [[ "$next_byte" -le "$start_byte" ]]; then
                error "Bulk ingest of $name stalled at byte $start_byte"
            fi

            log_info "    resuming at byte $next_byte..."
            start_byte="$next_byte"
        done

        log_info "    done ($file_indexed document(s) in $rounds pass(es))"
    done

    log_info "Indexed $total_indexed document(s), $total_failed failure(s)"

    if [[ "$total_failed" -gt 0 ]]; then
        log_warn "Some documents were rejected. Check $ADMIN_LAMBDA logs for detail."
    fi
}

# Verify the result: document counts and the Latin analyzer.
verify() {
    log_info "Verifying..."

    local index result count
    for index in "${INDEXES[@]}"; do
        invoke_admin "$(jq -nc --arg i "$index" '{action:"refresh", index:$i}')" > /dev/null \
            || log_warn "  could not refresh $index"

        if result=$(invoke_admin "$(jq -nc --arg i "$index" '{action:"count", index:$i}')"); then
            count=$(jq -r '.result.count' <<< "$result")
            log_info "  $index: $count document(s)"
            if [[ "$count" == "0" ]]; then
                log_warn "  $index is empty"
            fi
        else
            log_warn "  could not count $index"
        fi
    done

    # The Latin analyzer is the part most likely to be silently wrong: a missing
    # dictionary fails index creation loudly, but a stale package simply returns
    # unlemmatised tokens. 'dividunt' should normalize v to u, match the rule for
    # 'diuidunt', and come back as the lemma 'divido'.
    log_info "  checking the Latin analyzer..."
    local tokens
    if result=$(invoke_admin '{"action":"analyze","index":"canvas","analyzer":"latin","text":"dividunt"}'); then
        tokens=$(jq -r '.result.tokens | join(" ")' <<< "$result")
        if [[ "$tokens" == "divido" ]]; then
            log_info "    latin analyzer OK ('dividunt' -> '$tokens')"
        else
            log_warn "    latin analyzer returned '$tokens', expected 'divido'."
            log_warn "    The stemmer dictionary may be stale. Re-apply $TF_DIR and rerun."
        fi
    else
        log_warn "    could not run the analyzer check"
    fi
}

print_summary() {
    echo ""
    echo "========================================"
    log_info "Opensearch is initialized ($ENVIRONMENT)"
    echo ""
    echo "  Search endpoint:"
    echo "    $SEARCH_URL"
    echo ""
    echo "  Pass it to generate-iiif-pres so the viewer can find it:"
    echo ""
    echo "    java -jar rosa-tool/target/rosa-tool.jar generate-iiif-pres \\"
    echo "      --archive $ARCHIVE_DIR \\"
    echo "      --output /deploy/iiif \\"
    echo "      --base-url https://iiif.example.org \\"
    echo "      --opensearch-url $SEARCH_URL"
    echo ""
    echo "  Verify from a browser or any host:"
    echo ""
    echo "    curl -X POST '$SEARCH_URL' \\"
    echo "      -H 'Content-Type: application/json' \\"
    echo "      -d '{\"query\":{\"match_all\":{}},\"size\":1}'"
    echo "========================================"
    echo ""
}

main() {
    echo ""
    echo "========================================"
    echo "  rosa2 Opensearch Initialization"
    echo "  environment: $ENVIRONMENT"
    echo "========================================"
    echo ""

    TMP_DIR=$(mktemp -d)

    check_prerequisites
    load_config
    confirm_destructive

    mkdir -p "$BUILD_DIR"

    if [[ "$SKIP_GENERATE" != "true" ]]; then
        generate_bulk_data
    else
        log_info "Skipping bulk generation (--skip-generate)"
        [[ -d "$BULK_DIR" ]] || error "No bulk files at $BULK_DIR. Run without --skip-generate."
    fi

    prepare_index_definitions

    if [[ "$SKIP_UPLOAD" != "true" ]]; then
        upload_staging_data
    else
        log_info "Skipping S3 staging (--skip-upload)"
    fi

    check_cluster

    if [[ "$SKIP_INDEX" != "true" ]]; then
        recreate_indexes
    else
        log_info "Skipping index recreation (--skip-index)"
    fi

    if [[ "$SKIP_INGEST" != "true" ]]; then
        ingest_bulk_data
    else
        log_info "Skipping ingest (--skip-ingest)"
    fi

    verify
    print_summary
}

main "$@"
