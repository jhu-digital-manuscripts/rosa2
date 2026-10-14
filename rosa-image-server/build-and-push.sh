#!/usr/bin/env bash
#
# Build the rosa-image-server Docker image and, optionally, push it to the
# environment's ECR repository (created by jhu-rosa2-tf).
#
# The licensed Kakadu ZIP is passed to the build as a named build context, so
# it never has to be copied into this repository. This script does not deploy:
# set image_server_image_tag in jhu-rosa2-tf/<env>.tfvars to the printed tag
# and run `tofu apply`.
#
# Run with --help for usage.

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

CONTAINER_CLI="${CONTAINER_CLI:-docker}"
KAKADU_ZIP_PATH="${KAKADU_ZIP:-}"
CANTALOUPE_VERSION="5.0.7"
PLATFORM="linux/amd64"
TAG=""
ENVIRONMENT=""
PUSH=false
ALLOW_DIRTY=false
DRY_RUN=false
REPOSITORY_URL=""
NAME_PREFIX=""
REGION="${AWS_REGION:-${AWS_DEFAULT_REGION:-us-east-1}}"
LOCAL_IMAGE_NAME="rosa-image-server"

usage() {
  cat <<EOF
Usage: $(basename "$0") --kakadu-zip PATH [options]

Builds the Cantaloupe + Kakadu image as ${LOCAL_IMAGE_NAME}:<tag> and, with
--push, pushes it to the ECR repository of the given environment.

Options:
  --kakadu-zip PATH          Licensed Kakadu distribution ZIP (required; or set
                             KAKADU_ZIP). Read from where it is, never copied
                             into the repository.
  --cantaloupe-version VER   Cantaloupe release to install (default: ${CANTALOUPE_VERSION}).
  --tag TAG                  Image tag (default: <cantaloupe-version>-<git short sha>).
  --platform PLATFORM        Target platform (default: ${PLATFORM}).
  --env stage|prod           Environment whose ECR repository to push to.
                             Required with --push.
  --push                     Push the image to ECR after building.
  --no-push                  Build only (default).
  --repository URL           ECR repository URL to push to, overriding the
                             naming convention (<account>.dkr.ecr.<region>.amazonaws.com/<name-prefix>-image-server).
  --name-prefix PREFIX       name_prefix of the environment (default: jhu-rosa2 for
                             prod, jhu-rosa2-stage for stage, as in the tfvars).
  --region REGION            AWS region (default: ${REGION}).
  --allow-dirty              Allow building from uncommitted changes in rosa-image-server/.
  --dry-run                  Print the build and push commands without running them.
  -h, --help                 Show this help.

Environment:
  CONTAINER_CLI              Container CLI to use (default: docker; podman also works).
  KAKADU_ZIP                 Default for --kakadu-zip.

Examples:
  $(basename "$0") --kakadu-zip /home/msp/work/kakadu/v8_6_1-02130L.zip
  $(basename "$0") --kakadu-zip /home/msp/work/kakadu/v8_6_1-02130L.zip --env stage --push

Pushing needs AWS credentials (aws sso login; export AWS_PROFILE=...). Tags are
immutable in ECR, so pushing an existing tag fails before anything is uploaded.
EOF
}

die() {
  echo "error: $*" >&2
  exit 1
}

# Prints a command, then runs it unless --dry-run was given.
run() {
  printf '+'
  printf ' %q' "$@"
  printf '\n'
  if [[ "${DRY_RUN}" == false ]]; then
    "$@"
  fi
}

require_value() {
  [[ $# -ge 2 && -n "$2" ]] || die "$1 requires a value"
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --kakadu-zip) require_value "$@"; KAKADU_ZIP_PATH="$2"; shift 2 ;;
    --cantaloupe-version) require_value "$@"; CANTALOUPE_VERSION="$2"; shift 2 ;;
    --tag) require_value "$@"; TAG="$2"; shift 2 ;;
    --platform) require_value "$@"; PLATFORM="$2"; shift 2 ;;
    --env) require_value "$@"; ENVIRONMENT="$2"; shift 2 ;;
    --push) PUSH=true; shift ;;
    --no-push) PUSH=false; shift ;;
    --repository) require_value "$@"; REPOSITORY_URL="$2"; shift 2 ;;
    --name-prefix) require_value "$@"; NAME_PREFIX="$2"; shift 2 ;;
    --region) require_value "$@"; REGION="$2"; shift 2 ;;
    --allow-dirty) ALLOW_DIRTY=true; shift ;;
    --dry-run) DRY_RUN=true; shift ;;
    -h | --help) usage; exit 0 ;;
    *) usage >&2; die "unknown option: $1" ;;
  esac
done

# ---------- Validate arguments ----------

[[ -n "${KAKADU_ZIP_PATH}" ]] || { usage >&2; die "--kakadu-zip is required"; }
[[ -f "${KAKADU_ZIP_PATH}" ]] || die "Kakadu ZIP not found: ${KAKADU_ZIP_PATH}"
[[ "${CANTALOUPE_VERSION}" =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]] \
  || die "invalid Cantaloupe version: ${CANTALOUPE_VERSION}"

if [[ -n "${ENVIRONMENT}" && "${ENVIRONMENT}" != "stage" && "${ENVIRONMENT}" != "prod" ]]; then
  die "--env must be stage or prod"
fi
if [[ "${PUSH}" == true && -z "${ENVIRONMENT}" && -z "${REPOSITORY_URL}" ]]; then
  die "--push needs --env stage|prod (or --repository)"
fi

command -v "${CONTAINER_CLI}" >/dev/null || die "${CONTAINER_CLI} not found"

# ---------- Work out the tag ----------

if git -C "${SCRIPT_DIR}" rev-parse --is-inside-work-tree >/dev/null 2>&1; then
  if [[ "${ALLOW_DIRTY}" == false ]] \
    && [[ -n "$(git -C "${SCRIPT_DIR}" status --porcelain -- .)" ]]; then
    git -C "${SCRIPT_DIR}" status --short -- . >&2
    die "uncommitted changes in rosa-image-server/; commit them or pass --allow-dirty"
  fi
  GIT_SHA="$(git -C "${SCRIPT_DIR}" rev-parse --short=8 HEAD)"
else
  GIT_SHA=""
fi

if [[ -z "${TAG}" ]]; then
  [[ -n "${GIT_SHA}" ]] || die "not in a git checkout; pass --tag"
  TAG="${CANTALOUPE_VERSION}-${GIT_SHA}"
  if [[ "${ALLOW_DIRTY}" == true ]] \
    && [[ -n "$(git -C "${SCRIPT_DIR}" status --porcelain -- .)" ]]; then
    TAG="${TAG}-dirty"
  fi
fi
[[ "${TAG}" =~ ^[A-Za-z0-9_][A-Za-z0-9_.-]{0,127}$ ]] || die "invalid image tag: ${TAG}"

LOCAL_IMAGE="${LOCAL_IMAGE_NAME}:${TAG}"

# ---------- Build ----------

# Stage the ZIP alone in a temporary directory, so that the named build context
# holds only that file instead of whatever else sits next to it (such as an
# unpacked Kakadu tree). A hard link avoids copying where possible.
KAKADU_CONTEXT="$(mktemp -d "${TMPDIR:-/tmp}/rosa-kakadu-ctx.XXXXXX")"
cleanup() { rm -rf "${KAKADU_CONTEXT}"; }
trap cleanup EXIT

KAKADU_ZIP_NAME="$(basename "${KAKADU_ZIP_PATH}")"
ln "${KAKADU_ZIP_PATH}" "${KAKADU_CONTEXT}/${KAKADU_ZIP_NAME}" 2>/dev/null \
  || cp "${KAKADU_ZIP_PATH}" "${KAKADU_CONTEXT}/${KAKADU_ZIP_NAME}"

echo "Building ${LOCAL_IMAGE} (${PLATFORM}, Cantaloupe ${CANTALOUPE_VERSION}, Kakadu ${KAKADU_ZIP_NAME})"
run "${CONTAINER_CLI}" build \
  --platform "${PLATFORM}" \
  --build-context "kakadu_src=${KAKADU_CONTEXT}" \
  --build-arg "KAKADU_ZIP=${KAKADU_ZIP_NAME}" \
  --build-arg "CANTALOUPE_VERSION=${CANTALOUPE_VERSION}" \
  --label "org.opencontainers.image.revision=${GIT_SHA}" \
  --tag "${LOCAL_IMAGE}" \
  "${SCRIPT_DIR}"

if [[ "${PUSH}" == false ]]; then
  echo
  if [[ "${DRY_RUN}" == true ]]; then
    echo "Dry run: nothing was built."
  else
    echo "Built ${LOCAL_IMAGE}. Not pushed (use --env <env> --push)."
  fi
  exit 0
fi

# ---------- Push ----------

command -v aws >/dev/null || die "aws CLI not found"

if [[ -z "${REPOSITORY_URL}" ]]; then
  if [[ -z "${NAME_PREFIX}" ]]; then
    if [[ "${ENVIRONMENT}" == "prod" ]]; then NAME_PREFIX="jhu-rosa2"; else NAME_PREFIX="jhu-rosa2-stage"; fi
  fi
  if [[ "${DRY_RUN}" == true ]]; then
    ACCOUNT_ID="<account-id>"
  else
    ACCOUNT_ID="$(aws sts get-caller-identity --query Account --output text)"
  fi
  REPOSITORY_URL="${ACCOUNT_ID}.dkr.ecr.${REGION}.amazonaws.com/${NAME_PREFIX}-image-server"
fi

REGISTRY="${REPOSITORY_URL%%/*}"
REPOSITORY_NAME="${REPOSITORY_URL#*/}"
REMOTE_IMAGE="${REPOSITORY_URL}:${TAG}"

# Tags are immutable in the repository; fail early with a clear message.
if [[ "${DRY_RUN}" == false ]] && aws ecr describe-images --region "${REGION}" \
  --repository-name "${REPOSITORY_NAME}" --image-ids "imageTag=${TAG}" >/dev/null 2>&1; then
  die "${REMOTE_IMAGE} already exists; choose another --tag"
fi

echo "Pushing ${REMOTE_IMAGE}"
if [[ "${DRY_RUN}" == true ]]; then
  echo "+ aws ecr get-login-password --region ${REGION} | ${CONTAINER_CLI} login --username AWS --password-stdin ${REGISTRY}"
else
  aws ecr get-login-password --region "${REGION}" \
    | "${CONTAINER_CLI}" login --username AWS --password-stdin "${REGISTRY}"
fi
run "${CONTAINER_CLI}" tag "${LOCAL_IMAGE}" "${REMOTE_IMAGE}"
run "${CONTAINER_CLI}" push "${REMOTE_IMAGE}"

echo
if [[ "${DRY_RUN}" == true ]]; then
  echo "Dry run: nothing was built or pushed. A real run would print:"
fi
echo "Pushed ${REMOTE_IMAGE}. To deploy it, set in jhu-rosa2-tf/${ENVIRONMENT:-<env>}.tfvars:"
echo
echo "  image_server_image_tag = \"${TAG}\""
echo
echo "and run tofu apply for that environment."
