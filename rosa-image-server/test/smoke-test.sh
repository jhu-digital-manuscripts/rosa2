#!/usr/bin/env bash
#
# Local smoke test for the rosa-image-server image. No AWS access needed.
#
# Generates a JPEG 2000 test image with the image's own kdu_compress, serves it
# with FilesystemSource under an identifier containing slashes, and checks the
# IIIF Image API 2 and 3 endpoints, the legacy-path @id rewrite done by
# delegates.rb, caching and CORS headers, and that Kakadu does the decoding.
#
# Usage: test/smoke-test.sh [--image IMAGE] [--port PORT] [--benchmark] [--keep]
#
# Exits non-zero if any check fails.

set -euo pipefail

IMAGE="rosa-image-server:dev"
PORT=18182
BENCHMARK=false
KEEP=false
CONTAINER_CLI="${CONTAINER_CLI:-docker}"

# Public URL that Cantaloupe is told it lives at (as OpenTofu does in AWS).
BASE_URI="https://images.example.org"

usage() {
  cat <<EOF
Usage: $(basename "$0") [--image IMAGE] [--port PORT] [--benchmark] [--keep]

  --image IMAGE   Image to test (default: ${IMAGE})
  --port PORT     Host port to publish Cantaloupe on (default: ${PORT})
  --benchmark     Also compare info.json latency with the delegate on and off
  --keep          After the checks, keep the server running until Ctrl-C and
                  print the environment for the CloudFront Function e2e test
EOF
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --image) IMAGE="$2"; shift 2 ;;
    --port) PORT="$2"; shift 2 ;;
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
cleanup() {
  for c in "${CONTAINERS[@]}"; do
    "${CONTAINER_CLI}" rm -f "${c}" >/dev/null 2>&1 || true
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

# Identifier rose/Test/Test.001r -> <prefix>/rose/Test/Test.001r.jp2
ID_PATH="rose/Test/Test.001r"
ID_ENC="rose%2FTest%2FTest.001r"
WIDTH=2400
HEIGHT=3200

mkdir -p "${WORK_DIR}/images/rose/Test"
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
chmod -R a+rX "${WORK_DIR}/images"
if [[ -s "${WORK_DIR}/images/${ID_PATH}.jp2" ]]; then
  pass "kdu_compress produced a test JP2"
else
  fail "kdu_compress produced a test JP2"; exit 1
fi

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
echo "--- Codecs ---"
LOGS="$("${CONTAINER_CLI}" logs "${NAME}" 2>&1)"
check_contains "KakaduNativeProcessor decodes JP2" "${LOGS}" "KakaduNativeProcessor"
check_contains "TurboJPEG writes JPEG output" "${LOGS}" "TurboJPEGImageWriter"
if grep -q "UnsatisfiedLinkError" <<<"${LOGS}"; then
  fail "no native library load errors" "$(grep -m1 UnsatisfiedLinkError <<<"${LOGS}")"
else
  pass "no native library load errors"
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
  echo "    node --test jhu-rosa2-tf/modules/rosa2-image-server/functions/test/"
  echo
  echo "Press Ctrl-C to stop it."
  while true; do sleep 3600; done
fi

[[ "${FAIL}" -eq 0 ]]
