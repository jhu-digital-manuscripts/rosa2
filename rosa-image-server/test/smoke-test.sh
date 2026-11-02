#!/usr/bin/env bash
#
# Local smoke test for the rosa-image-server image. No AWS access needed.
#
# Generates a JPEG 2000 test image with the image's own kdu_compress, serves it
# with FilesystemSource under an identifier containing slashes, and checks the
# IIIF Image API 2 and 3 endpoints, the legacy-path @id rewrite done by
# delegates.rb, caching and CORS headers, that Kakadu does the decoding, and
# that Cantaloupe writes its application log at all. With --s3 it also serves
# the image from, and caches derivatives into, a local S3-compatible server
# (SeaweedFS, pulled from Docker Hub), which exercises the AWS SDK S3 client
# and its native CRT HTTP library the way the ECS deployment does.
#
# Usage: test/smoke-test.sh [--image IMAGE] [--port PORT] [--s3] [--benchmark] [--keep]
#
# Exits non-zero if any check fails.

set -euo pipefail

# build-and-push.sh tags images <cantaloupe-version>-<git sha>; the default
# here is the tag it produces for the current checkout.
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
IMAGE="rosa-image-server:$(sed -n 's/^ARG CANTALOUPE_VERSION=//p' "${SCRIPT_DIR}/../Dockerfile" | head -1)-$(git -C "${SCRIPT_DIR}" rev-parse --short=8 HEAD 2>/dev/null || echo dev)"
PORT=18182
S3=false
S3_IMAGE="${S3_IMAGE:-docker.io/chrislusf/seaweedfs:latest}"
BENCHMARK=false
KEEP=false
CONTAINER_CLI="${CONTAINER_CLI:-docker}"

# Public URL that Cantaloupe is told it lives at (as OpenTofu does in AWS).
BASE_URI="https://images.example.org"

usage() {
  cat <<EOF
Usage: $(basename "$0") [--image IMAGE] [--port PORT] [--s3] [--benchmark] [--keep]

  --image IMAGE   Image to test (default: ${IMAGE})
  --port PORT     Host port to publish Cantaloupe on (default: ${PORT})
  --s3            Also test S3Source and S3Cache against a local SeaweedFS
                  container (${S3_IMAGE}; override with S3_IMAGE)
  --benchmark     Also compare info.json latency with the delegate on and off
  --keep          After the checks, keep the server running until Ctrl-C and
                  print the environment for the CloudFront Function e2e test
EOF
}

require_value() {
  [[ $# -ge 2 && -n "$2" ]] || { echo "$1 requires a value" >&2; exit 2; }
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --image) require_value "$@"; IMAGE="$2"; shift 2 ;;
    --port) require_value "$@"; PORT="$2"; shift 2 ;;
    --s3) S3=true; shift ;;
    --benchmark) BENCHMARK=true; shift ;;
    --keep) KEEP=true; shift ;;
    -h | --help) usage; exit 0 ;;
    *) usage >&2; exit 2 ;;
  esac
done

command -v "${CONTAINER_CLI}" >/dev/null || { echo "${CONTAINER_CLI} not found" >&2; exit 2; }
command -v python3 >/dev/null || { echo "python3 not found" >&2; exit 2; }

WORK_DIR="$(mktemp -d "${TMPDIR:-/tmp}/rosa-smoke.XXXXXX")"
CONTAINERS=()
NETWORKS=()
cleanup() {
  for c in "${CONTAINERS[@]}"; do
    "${CONTAINER_CLI}" rm -f "${c}" >/dev/null 2>&1 || true
  done
  for n in "${NETWORKS[@]}"; do
    "${CONTAINER_CLI}" network rm "${n}" >/dev/null 2>&1 || true
  done
  rm -rf "${WORK_DIR}"
}
trap cleanup EXIT

PASS=0
FAIL=0
pass() { PASS=$((PASS + 1)); printf 'PASS  %s\n' "$1"; }
fail() { FAIL=$((FAIL + 1)); printf 'FAIL  %s\n' "$1"; [[ $# -lt 2 ]] || printf '      %s\n' "$2"; }

# check NAME ACTUAL EXPECTED
check_eq() {
  if [[ "$2" == "$3" ]]; then pass "$1"; else fail "$1" "expected '$3', got '$2'"; fi
}
# check_contains NAME HAYSTACK NEEDLE
check_contains() {
  if [[ "$2" == *"$3"* ]]; then pass "$1"; else fail "$1" "expected to contain '$3', got '$2'"; fi
}

# ---------- Test image ----------

# Identifier rose/Test/Test.001r -> <prefix>/rose/Test/Test.001r.jp2, and the
# cropped form rose/Test/cropped/Test.001r that rosa-tool emits for books with
# an images.crop.csv -> <prefix>/rose/Test/cropped/Test.001r.jp2.
ID_PATH="rose/Test/Test.001r"
ID_ENC="rose%2FTest%2FTest.001r"
CROPPED_PATH="rose/Test/cropped/Test.001r"
CROPPED_ENC="rose%2FTest%2Fcropped%2FTest.001r"
WIDTH=2400
HEIGHT=3200
CROP_WIDTH=2000
CROP_HEIGHT=2800
AREA=$((WIDTH * HEIGHT))

mkdir -p "${WORK_DIR}/images/rose/Test/cropped"
python3 - "${WORK_DIR}/source.ppm" "${WIDTH}" "${HEIGHT}" <<'PY'
import sys
path, w, h = sys.argv[1], int(sys.argv[2]), int(sys.argv[3])
row = bytearray()
for x in range(w):
    row += bytes(((x * 255) // w, 128, 255 - (x * 255) // w))
with open(path, "wb") as f:
    f.write(b"P6\n%d %d\n255\n" % (w, h))
    for y in range(h):
        f.write(row)
PY
chmod -R a+rwX "${WORK_DIR}"

"${CONTAINER_CLI}" run --rm --user 0 --entrypoint kdu_compress \
  -v "${WORK_DIR}:/work:z" "${IMAGE}" \
  -quiet -i /work/source.ppm -o "/work/images/${ID_PATH}.jp2" \
  Clevels=6 Corder=RPCL "Cblk={64,64}" "Cprecincts={256,256}" ORGgen_plt=yes \
  -rate 2.4 >/dev/null
# The "cropped" image is a smaller region of the same source.
python3 - "${WORK_DIR}/source.ppm" "${WORK_DIR}/cropped.ppm" "${CROP_WIDTH}" "${CROP_HEIGHT}" <<'PY'
import sys
src, dst, cw, ch = sys.argv[1], sys.argv[2], int(sys.argv[3]), int(sys.argv[4])
data = open(src, "rb").read()
header_end = data.index(b"255\n") + 4
w = int(data[3:].split()[0])
pixels = data[header_end:]
with open(dst, "wb") as f:
    f.write(b"P6\n%d %d\n255\n" % (cw, ch))
    for y in range(ch):
        f.write(pixels[y * w * 3:(y * w + cw) * 3])
PY
chmod -R a+rwX "${WORK_DIR}"
"${CONTAINER_CLI}" run --rm --user 0 --entrypoint kdu_compress \
  -v "${WORK_DIR}:/work:z" "${IMAGE}" \
  -quiet -i /work/cropped.ppm -o "/work/images/${CROPPED_PATH}.jp2" \
  Clevels=6 Corder=RPCL "Cblk={64,64}" "Cprecincts={256,256}" ORGgen_plt=yes \
  -rate 2.4 >/dev/null
chmod -R a+rX "${WORK_DIR}/images"
if [[ -s "${WORK_DIR}/images/${ID_PATH}.jp2" && -s "${WORK_DIR}/images/${CROPPED_PATH}.jp2" ]]; then
  pass "kdu_compress produced the test JP2s"
else
  fail "kdu_compress produced the test JP2s"; exit 1
fi
# kdu_expand -v prints the core version and links libkdu_vNNR.so, so this also
# proves that the Kakadu shared libraries resolve at runtime.
KDU_VERSION="$("${CONTAINER_CLI}" run --rm --entrypoint kdu_expand "${IMAGE}" -v 2>&1 | grep -o 'v[0-9][0-9.]*' | head -1)"
if [[ -n "${KDU_VERSION}" ]]; then
  pass "kdu_expand -v reports Kakadu ${KDU_VERSION}"
else
  fail "kdu_expand -v reports Kakadu version"
fi
EXPECTED_CANTALOUPE="$(sed -n 's/^ARG CANTALOUPE_VERSION=//p' "${SCRIPT_DIR}/../Dockerfile" | head -1)"

# ---------- Start Cantaloupe ----------

# start_server NAME HOST_PORT [extra docker args...]
start_server() {
  local name="$1" port="$2"
  shift 2
  "${CONTAINER_CLI}" run -d --name "${name}" -p "127.0.0.1:${port}:8182" \
    -v "${WORK_DIR}/images:/images:ro,z" \
    -e SOURCE_STATIC=FilesystemSource \
    -e FILESYSTEMSOURCE_BASICLOOKUPSTRATEGY_PATH_PREFIX=/images/ \
    -e FILESYSTEMSOURCE_BASICLOOKUPSTRATEGY_PATH_SUFFIX=.jp2 \
    -e CACHE_SERVER_DERIVATIVE_ENABLED=false \
    -e BASE_URI="${BASE_URI}" \
    "$@" "${IMAGE}" >/dev/null
  CONTAINERS+=("${name}")
  for _ in $(seq 1 60); do
    if curl -fsS "http://127.0.0.1:${port}/health" >/dev/null 2>&1; then return 0; fi
    sleep 1
  done
  "${CONTAINER_CLI}" logs "${name}" | tail -30 >&2
  return 1
}

NAME="rosa-smoke-$$"
if start_server "${NAME}" "${PORT}" -e LOG_APPLICATION_LEVEL=debug; then
  pass "server started and /health responds"
else
  fail "server started and /health responds"; exit 1
fi

URL="http://127.0.0.1:${PORT}"

# Fetch: sets BODY, STATUS, HEADERS for a URL with optional extra curl args.
fetch() {
  local url="$1"
  shift
  local hdr="${WORK_DIR}/headers" body="${WORK_DIR}/body"
  STATUS="$(curl -sS -o "${body}" -D "${hdr}" -w '%{http_code}' "$@" "${url}")"
  HEADERS="$(tr -d '\r' <"${hdr}")"
  BODY_FILE="${body}"
}
header() { # header NAME -> value of the last occurrence
  printf '%s\n' "${HEADERS}" | awk -v n="$(echo "$1" | tr '[:upper:]' '[:lower:]')" \
    -F': ' 'tolower($1)==n { sub(/^[^:]*: /, ""); v=$0 } END { print v }'
}
json() { # json EXPR -> python expression over parsed body "d"
  python3 -c "import json,sys; d=json.load(open(sys.argv[1])); print($1)" "${BODY_FILE}"
}
jpeg_size() { # prints WxH of the JPEG in BODY_FILE
  python3 - "${BODY_FILE}" <<'PY'
import struct, sys
data = open(sys.argv[1], "rb").read()
assert data[:2] == b"\xff\xd8", "not a JPEG"
i = 2
while i < len(data):
    marker, length = data[i + 1], struct.unpack(">H", data[i + 2:i + 4])[0]
    if marker in (0xC0, 0xC1, 0xC2):
        h, w = struct.unpack(">HH", data[i + 5:i + 9])
        print(f"{w}x{h}")
        break
    i += 2 + length
PY
}

echo
echo "--- Image API 2 ---"
fetch "${URL}/iiif/2/${ID_ENC}/info.json"
check_eq "v2 info.json status" "${STATUS}" "200"
check_eq "v2 @id uses base_uri and /iiif/2" "$(json 'd["@id"]')" "${BASE_URI}/iiif/2/${ID_ENC}"
check_eq "v2 width/height" "$(json 'str(d["width"]) + "x" + str(d["height"])')" "${WIDTH}x${HEIGHT}"
check_eq "v2 tile width is 512" "$(json 'd["tiles"][0]["width"]')" "512"
check_eq "CORS Access-Control-Allow-Origin" "$(header Access-Control-Allow-Origin)" "*"
check_contains "Cache-Control max-age" "$(header Cache-Control)" "max-age=2592000"
check_contains "Cache-Control s-maxage" "$(header Cache-Control)" "s-maxage=31536000"

fetch "${URL}/iiif/2/${ID_ENC}"
check_eq "v2 base URI redirects (303)" "${STATUS}" "303"
check_eq "v2 redirect Location" "$(header Location)" "${BASE_URI}/iiif/2/${ID_ENC}/info.json"

fetch "${URL}/iiif/2/${ID_ENC}/0,0,1024,1024/512,/0/default.jpg"
check_eq "v2 512px tile status" "${STATUS}" "200"
check_eq "v2 tile Content-Type" "$(header Content-Type)" "image/jpeg"
check_eq "v2 tile dimensions" "$(jpeg_size)" "512x512"
check_contains "v2 tile canonical Link header" "$(header Link)" "<${BASE_URI}/iiif/2/${ID_ENC}/"

fetch "${URL}/iiif/2/${ID_ENC}/full/80,/0/default.jpg"
check_eq "v2 80px thumbnail status" "${STATUS}" "200"
check_eq "v2 thumbnail width" "$(jpeg_size | cut -dx -f1)" "80"

echo
echo "--- Image API 3 ---"
fetch "${URL}/iiif/3/${ID_ENC}/info.json"
check_eq "v3 info.json status" "${STATUS}" "200"
check_eq "v3 id uses base_uri and /iiif/3" "$(json 'd["id"]')" "${BASE_URI}/iiif/3/${ID_ENC}"

fetch "${URL}/iiif/3/${ID_ENC}/full/max/0/default.jpg"
check_eq "v3 full/max status" "${STATUS}" "200"
check_eq "v3 full/max is the full image (within max_pixels)" "$(jpeg_size)" "${WIDTH}x${HEIGHT}"

# Image API 3 removed the `full` size keyword; rosa-tool emits `max`.
fetch "${URL}/iiif/3/${ID_ENC}/full/full/0/default.jpg"
check_eq "v3 full/full is rejected (400)" "${STATUS}" "400"

fetch "${URL}/iiif/2/${ID_ENC}/full/full/0/default.jpg"
check_eq "v2 full/full status (within max_pixels)" "${STATUS}" "200"

echo
echo "--- Cropped identifier (rose/<book>/cropped/<image>) ---"
fetch "${URL}/iiif/3/${CROPPED_ENC}/info.json"
check_eq "cropped v3 info.json status" "${STATUS}" "200"
check_eq "cropped v3 id" "$(json 'd["id"]')" "${BASE_URI}/iiif/3/${CROPPED_ENC}"
check_eq "cropped width/height" "$(json 'str(d["width"]) + "x" + str(d["height"])')" "${CROP_WIDTH}x${CROP_HEIGHT}"
fetch "${URL}/iiif/2/${CROPPED_ENC}/full/80,/0/default.jpg"
check_eq "cropped v2 thumbnail status" "${STATUS}" "200"

echo
echo "--- max_pixels (server started with MAX_PIXELS below the test image's area) ---"
# A limit below the test image's ${AREA} pixels. A JP2 served as JPEG counts as
# modified, so a v2 full/full exceeding the limit is refused, while v3 `max`
# scales down to fit and info.json advertises the limit as maxArea.
MAXPX=$(((AREA * 2) / 3))
MAXPX_PORT=$((PORT + 2))
if start_server "${NAME}-maxpx" "${MAXPX_PORT}" -e MAX_PIXELS="${MAXPX}"; then
  pass "server with MAX_PIXELS=${MAXPX} started"
  MAXPX_URL="http://127.0.0.1:${MAXPX_PORT}"
  fetch "${MAXPX_URL}/iiif/3/${ID_ENC}/info.json"
  check_eq "v3 info.json maxArea equals max_pixels" "$(json 'd.get("maxArea")')" "${MAXPX}"
  fetch "${MAXPX_URL}/iiif/2/${ID_ENC}/full/full/0/default.jpg"
  check_eq "v2 full/full over max_pixels is refused (403)" "${STATUS}" "403"
  fetch "${MAXPX_URL}/iiif/3/${ID_ENC}/full/max/0/default.jpg"
  check_eq "v3 full/max over max_pixels still succeeds" "${STATUS}" "200"
  SIZE="$(jpeg_size)"
  if [[ -n "${SIZE}" ]] && (( ${SIZE%x*} * ${SIZE#*x} <= MAXPX )) && (( ${SIZE%x*} < WIDTH )); then
    pass "v3 full/max is scaled down to fit max_pixels (${SIZE})"
  else
    fail "v3 full/max is scaled down to fit max_pixels" "got ${SIZE}"
  fi
  fetch "${MAXPX_URL}/iiif/2/${ID_ENC}/full/80,/0/default.jpg"
  check_eq "thumbnails unaffected by max_pixels" "${STATUS}" "200"
else
  fail "server with MAX_PIXELS=${MAXPX} started"
fi

fetch "${URL}/iiif/3/${ID_ENC}/info.json" -H 'X-Rosa-Legacy-Path: 1'
check_eq "v3 id ignores legacy marker" "$(json 'd["id"]')" "${BASE_URI}/iiif/3/${ID_ENC}"

echo
echo "--- Legacy /iiif @id (delegates.rb) ---"
fetch "${URL}/iiif/2/${ID_ENC}/info.json" -H 'X-Rosa-Legacy-Path: 1'
check_eq "legacy marker rewrites @id to /iiif/<id>" "$(json 'd["@id"]')" "${BASE_URI}/iiif/${ID_ENC}"
check_eq "legacy info.json keeps other keys" "$(json 'd["width"]')" "${WIDTH}"

fetch "${URL}/iiif/2/${ID_ENC}/info.json" -H 'x-rosa-legacy-path: 1'
check_eq "legacy marker header is case-insensitive" "$(json 'd["@id"]')" "${BASE_URI}/iiif/${ID_ENC}"

fetch "${URL}/iiif/2/${ID_ENC}/info.json"
check_eq "no marker leaves @id at /iiif/2 (info cache not polluted)" "$(json 'd["@id"]')" "${BASE_URI}/iiif/2/${ID_ENC}"

fetch "${URL}/iiif/2/rose%2FTest%2FMissing/info.json" -H 'X-Rosa-Legacy-Path: 1'
check_eq "missing image returns 404" "${STATUS}" "404"
# Missing-source errors are plain text from Cantaloupe's error handler (no @id),
# and must not be cached for long by CloudFront.
check_contains "404 is not cacheable (Cache-Control no-cache)" "$(header Cache-Control)" "no-cache"
check_eq "404 has CORS header" "$(header Access-Control-Allow-Origin)" "*"

fetch "${URL}/iiif/2/${ID_ENC}/full/80,/0/default.jpg" -H 'X-Rosa-Legacy-Path: 1'
check_eq "image requests unaffected by marker" "${STATUS}" "200"

echo
echo "--- Logging and codecs ---"
LOGS="$("${CONTAINER_CLI}" logs "${NAME}" 2>&1)"
# Cantaloupe 5.0.8 as released attaches no log appender at all
# (cantaloupe-project/cantaloupe#1007); the image patches logback.xml.
check_contains "application log is written (Starting Cantaloupe ${EXPECTED_CANTALOUPE})" "${LOGS}" "Starting Cantaloupe ${EXPECTED_CANTALOUPE}"
check_contains "KakaduNativeProcessor decodes JP2" "${LOGS}" "KakaduNativeProcessor"
check_contains "TurboJPEG writes JPEG output" "${LOGS}" "TurboJPEGImageWriter"
if grep -q "UnsatisfiedLinkError" <<<"${LOGS}"; then
  fail "no native library load errors" "$(grep -m1 UnsatisfiedLinkError <<<"${LOGS}")"
else
  pass "no native library load errors"
fi

# ---------- Optional S3Source / S3Cache test ----------

if [[ "${S3}" == true ]]; then
  echo
  echo "--- S3Source and S3Cache against SeaweedFS ---"
  # Cantaloupe and SeaweedFS share a network so that the S3 endpoint has a
  # stable name; the host reaches SeaweedFS through a published port to create
  # the buckets, upload the source image and inspect the cache bucket. No
  # identities are configured, so SeaweedFS accepts any credentials; the SDK
  # still needs some, and a region. Note that with S3SOURCE_ENDPOINT set,
  # Cantaloupe 5.0.8 creates a new S3 client per S3 call (README, "5.0.8 S3
  # client caching bug"); harmless for the few requests made here.
  S3_PORT=$((PORT + 3))
  S3_NET="rosa-smoke-net-$$"
  S3_NAME="${NAME}-s3"
  S3_URL="http://127.0.0.1:${S3_PORT}"
  SRC_BUCKET="rosa-source"
  CACHE_BUCKET="rosa-cache"
  "${CONTAINER_CLI}" network create "${S3_NET}" >/dev/null
  NETWORKS+=("${S3_NET}")
  "${CONTAINER_CLI}" run -d --name "${S3_NAME}" --network "${S3_NET}" \
    -p "127.0.0.1:${S3_PORT}:8333" "${S3_IMAGE}" \
    server -s3 -dir=/data -ip="${S3_NAME}" -ip.bind=0.0.0.0 -master.volumeSizeLimitMB=64 >/dev/null
  CONTAINERS+=("${S3_NAME}")
  S3_READY=false
  for _ in $(seq 1 60); do
    if curl -fsS -X PUT "${S3_URL}/${SRC_BUCKET}/" >/dev/null 2>&1 \
      && curl -fsS -X PUT "${S3_URL}/${CACHE_BUCKET}/" >/dev/null 2>&1; then
      S3_READY=true; break
    fi
    sleep 1
  done
  if [[ "${S3_READY}" == true ]]; then
    pass "SeaweedFS started and buckets created"
    curl -fsS -X PUT --data-binary "@${WORK_DIR}/images/${ID_PATH}.jp2" \
      "${S3_URL}/${SRC_BUCKET}/${ID_PATH}.jp2" >/dev/null
    S3_SERVER_PORT=$((PORT + 4))
    if start_server "${NAME}-s3src" "${S3_SERVER_PORT}" --network "${S3_NET}" \
        -e LOG_APPLICATION_LEVEL=debug \
        -e SOURCE_STATIC=S3Source \
        -e S3SOURCE_ENDPOINT="http://${S3_NAME}:8333" \
        -e S3SOURCE_REGION=us-east-1 \
        -e S3SOURCE_ACCESS_KEY_ID=smoke -e S3SOURCE_SECRET_KEY=smoke \
        -e S3SOURCE_BASICLOOKUPSTRATEGY_BUCKET_NAME="${SRC_BUCKET}" \
        -e S3SOURCE_BASICLOOKUPSTRATEGY_PATH_PREFIX= \
        -e CACHE_SERVER_DERIVATIVE_ENABLED=true \
        -e S3CACHE_ENDPOINT="http://${S3_NAME}:8333" \
        -e S3CACHE_REGION=us-east-1 \
        -e S3CACHE_ACCESS_KEY_ID=smoke -e S3CACHE_SECRET_KEY=smoke \
        -e S3CACHE_BUCKET_NAME="${CACHE_BUCKET}"; then
      pass "server with S3Source and S3Cache started"
      S3_SERVER_URL="http://127.0.0.1:${S3_SERVER_PORT}"
      fetch "${S3_SERVER_URL}/iiif/3/${ID_ENC}/info.json"
      check_eq "S3Source v3 info.json status" "${STATUS}" "200"
      check_eq "S3Source width/height" "$(json 'str(d["width"]) + "x" + str(d["height"])')" "${WIDTH}x${HEIGHT}"
      fetch "${S3_SERVER_URL}/iiif/2/${ID_ENC}/0,0,1024,1024/512,/0/default.jpg"
      check_eq "S3Source 512px tile status" "${STATUS}" "200"
      check_eq "S3Source tile dimensions" "$(jpeg_size)" "512x512"
      fetch "${S3_SERVER_URL}/iiif/2/rose%2FTest%2FMissing/info.json"
      check_eq "S3Source missing object returns 404" "${STATUS}" "404"
      # The derivative and the info are written to the cache bucket asynchronously.
      CACHED=""
      for _ in $(seq 1 20); do
        CACHED="$(curl -fsS "${S3_URL}/${CACHE_BUCKET}/?prefix=cache/" 2>/dev/null | grep -o '<Key>[^<]*</Key>' | tr '\n' ' ')"
        [[ "${CACHED}" == *"<Key>cache/image/"* && "${CACHED}" == *"<Key>cache/info/"* ]] && break
        sleep 1
      done
      check_contains "S3Cache wrote the derivative image" "${CACHED}" "<Key>cache/image/"
      check_contains "S3Cache wrote the info" "${CACHED}" "<Key>cache/info/"
      # Second request for the same tile is served from the derivative cache.
      fetch "${S3_SERVER_URL}/iiif/2/${ID_ENC}/0,0,1024,1024/512,/0/default.jpg"
      check_eq "cached tile status" "${STATUS}" "200"
      # On a hit S3Cache.newDerivativeImageInputStream() touches the object
      # (touchAsync); on a miss it does not.
      S3_LOGS="$("${CONTAINER_CLI}" logs "${NAME}-s3src" 2>&1)"
      check_contains "S3Cache hit for the repeated tile" "${S3_LOGS}" "touchAsync(): cache/image/"
      if grep -q -E "UnsatisfiedLinkError|CrtRuntimeException|Unable to load.*crt" <<<"${S3_LOGS}"; then
        fail "AWS CRT native library loads" "$(grep -m1 -E "UnsatisfiedLinkError|CrtRuntimeException|Unable to load.*crt" <<<"${S3_LOGS}")"
      else
        pass "AWS CRT native library loads"
      fi
    else
      fail "server with S3Source and S3Cache started"
    fi
  else
    fail "SeaweedFS started and buckets created" "$("${CONTAINER_CLI}" logs "${S3_NAME}" 2>&1 | tail -5)"
  fi
fi

# ---------- Optional benchmark ----------

if [[ "${BENCHMARK}" == true ]]; then
  echo
  echo "--- Benchmark: info.json latency, delegate on vs off (100 requests each) ---"
  bench() {
    local url="$1"
    shift
    python3 - "$url" "$@" <<'PY'
import statistics, sys, time, urllib.request
url, headers = sys.argv[1], dict(h.split(": ", 1) for h in sys.argv[2:])
for _ in range(10):
    urllib.request.urlopen(urllib.request.Request(url, headers=headers)).read()
times = []
for _ in range(100):
    t = time.perf_counter()
    urllib.request.urlopen(urllib.request.Request(url, headers=headers)).read()
    times.append((time.perf_counter() - t) * 1000)
times.sort()
print(f"median {statistics.median(times):.2f} ms, p95 {times[94]:.2f} ms")
PY
  }
  OFF_PORT=$((PORT + 1))
  start_server "${NAME}-nodelegate" "${OFF_PORT}" -e DELEGATE_SCRIPT_ENABLED=false
  echo "delegate on : $(bench "${URL}/iiif/2/${ID_ENC}/info.json")"
  echo "delegate on (legacy marker): $(bench "${URL}/iiif/2/${ID_ENC}/info.json" 'X-Rosa-Legacy-Path: 1')"
  echo "delegate off: $(bench "http://127.0.0.1:${OFF_PORT}/iiif/2/${ID_ENC}/info.json")"
fi

echo
echo "${PASS} passed, ${FAIL} failed"

if [[ "${KEEP}" == true ]]; then
  echo
  echo "Server left running for the CloudFront Function e2e test. In another shell:"
  echo
  echo "  ROSA_IMAGE_SERVER_URL=${URL} ROSA_PUBLIC_BASE_URL=${BASE_URI} ROSA_TEST_ID=${ID_ENC} \\"
  echo "    node --test 'jhu-rosa2-tf/modules/rosa2-image-server/functions/test/*.test.mjs'"
  echo
  echo "Press Ctrl-C to stop it."
  while true; do sleep 3600; done
fi

[[ "${FAIL}" -eq 0 ]]
