#!/usr/bin/env python3
"""
Latency and throughput benchmark for a Rosa IIIF image server.

Replays the request patterns rosa-viewer produces (see the README, "Benchmarking"):

  info     info.json for each identifier
  thumbs   the 80 px thumbnails the search results, gallery view and thumbnail strip use
  open     opening a page: one scale-factor-8 overview tile and the scale-factor-4 tile grid
  zoom     a 3x2 block of full-resolution 512 px tiles, as a zoomed-in viewport requests
  fullmax  full/max, the painting body URL in the manifests

Each identifier contributes one copy of every request per round. With --unique the
region of every request is shifted by the round number, so neither CloudFront nor
Cantaloupe's derivative cache can answer it and the numbers measure decoding.

Usage:
  bench.py --base http://127.0.0.1:18182/iiif/3 --ids rose/Test/Test.001r ... \
           [--scenarios info,thumbs,open,zoom,fullmax] [--concurrency 4] [--rounds 3] \
           [--unique] [--label name] [--json out.json]

Requires python3 and httpx.
"""

import argparse
import json
import math
import statistics
import sys
import time
import urllib.parse
from concurrent.futures import ThreadPoolExecutor

import httpx

TILE = 512
THUMB_WIDTH = 80


def enc(identifier):
    return urllib.parse.quote(identifier, safe="")


def image_url(base, identifier, region, size):
    return f"{base}/{enc(identifier)}/{region}/{size}/0/default.jpg"


def tiles_for_level(width, height, scale, shift=0, limit=None):
    """Regions of the 512 px tiles at the given scale factor, clipped to the image.
    OpenSeadragon asks for the tile's full-resolution region and the scaled width."""
    span = TILE * scale
    cols = math.ceil((width - shift) / span)
    rows = math.ceil((height - shift) / span)
    out = []
    for row in range(rows):
        for col in range(cols):
            x = shift + col * span
            y = shift + row * span
            w = min(span, width - x)
            h = min(span, height - y)
            if w <= 0 or h <= 0:
                continue
            out.append((f"{x},{y},{w},{h}", f"{math.ceil(w / scale)},"))
    if limit:
        # Keep a block around the centre, as a viewport would.
        mid = len(out) // 2
        start = max(0, mid - limit // 2)
        out = out[start:start + limit]
    return out


def scenario_urls(name, base, identifier, width, height, round_no, unique):
    k = round_no if unique else 0
    if name == "info":
        return [f"{base}/{enc(identifier)}/info.json" + (f"?r={k}" if unique else "")]
    if name == "thumbs":
        region = "full" if k == 0 else f"0,{k},{width},{height - k}"
        return [image_url(base, identifier, region, f"{THUMB_WIDTH},")]
    if name == "open":
        urls = []
        # Overview: whole image at scale factor 8 (one tile for images up to 4096 px wide)
        for region, size in tiles_for_level(width, height, 8, shift=k):
            urls.append(image_url(base, identifier, region, size))
        for region, size in tiles_for_level(width, height, 4, shift=k):
            urls.append(image_url(base, identifier, region, size))
        return urls
    if name == "zoom":
        return [image_url(base, identifier, region, size)
                for region, size in tiles_for_level(width, height, 1, shift=k, limit=6)]
    if name == "fullmax":
        region = "full" if k == 0 else f"0,{k},{width},{height - k}"
        return [image_url(base, identifier, region, "max")]
    raise ValueError(name)


def fetch(client, url):
    t0 = time.perf_counter()
    try:
        r = client.get(url)
        elapsed = time.perf_counter() - t0
        return {
            "url": url, "status": r.status_code, "ms": elapsed * 1000,
            "bytes": len(r.content),
            "x_cache": r.headers.get("x-cache", ""),
            "server": r.headers.get("x-powered-by", ""),
        }
    except httpx.HTTPError as e:
        return {"url": url, "status": 0, "ms": (time.perf_counter() - t0) * 1000,
                "bytes": 0, "x_cache": "", "server": "", "error": str(e)}


def percentile(values, pct):
    if not values:
        return float("nan")
    values = sorted(values)
    idx = min(len(values) - 1, max(0, math.ceil(pct / 100 * len(values)) - 1))
    return values[idx]


def summarize(results, wall):
    ok = [r for r in results if r["status"] == 200]
    ms = [r["ms"] for r in ok]
    summary = {
        "requests": len(results), "ok": len(ok),
        "errors": sorted({(r["status"], r.get("error", "")) for r in results if r["status"] != 200}),
        "median_ms": statistics.median(ms) if ms else float("nan"),
        "p90_ms": percentile(ms, 90), "p95_ms": percentile(ms, 95),
        "max_ms": max(ms) if ms else float("nan"),
        "mean_ms": statistics.fmean(ms) if ms else float("nan"),
        "wall_s": wall, "req_per_s": len(ok) / wall if wall else float("nan"),
        "mb": sum(r["bytes"] for r in ok) / 1e6,
        "cloudfront": {},
    }
    for state in ("Hit", "Miss", "RefreshHit"):
        sub = [r["ms"] for r in ok if r["x_cache"].startswith(state)]
        if sub:
            summary["cloudfront"][state] = {"n": len(sub), "median_ms": statistics.median(sub)}
    return summary


def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--base", required=True, help="Image API base, e.g. http://host:8182/iiif/3")
    ap.add_argument("--ids", nargs="+", required=True, help="image identifiers (unencoded)")
    ap.add_argument("--scenarios", default="info,thumbs,open,zoom,fullmax")
    ap.add_argument("--concurrency", type=int, default=4)
    ap.add_argument("--rounds", type=int, default=3)
    ap.add_argument("--unique", action="store_true", help="shift regions per round to defeat caches")
    ap.add_argument("--warmup", action="store_true", help="fetch each info.json once before timing")
    ap.add_argument("--label", default="")
    ap.add_argument("--json", help="write per-scenario summaries to this file")
    ap.add_argument("--timeout", type=float, default=120)
    args = ap.parse_args()

    base = args.base.rstrip("/")
    client = httpx.Client(timeout=args.timeout, headers={"User-Agent": "rosa-bench/1"})

    dims = {}
    for identifier in args.ids:
        r = client.get(f"{base}/{enc(identifier)}/info.json")
        if r.status_code != 200:
            sys.exit(f"info.json for {identifier}: HTTP {r.status_code}")
        info = r.json()
        dims[identifier] = (info["width"], info["height"])
        server = r.headers.get("x-powered-by", "")
    print(f"# {args.label or base}: {len(args.ids)} ids, concurrency {args.concurrency}, "
          f"rounds {args.rounds}{', unique regions' if args.unique else ''}"
          f"{', server ' + server if server else ''}")

    out = {"label": args.label, "base": base, "concurrency": args.concurrency,
           "rounds": args.rounds, "unique": args.unique, "scenarios": {}}
    header = f"{'scenario':<9}{'n':>5}{'ok':>5}{'median':>9}{'p95':>9}{'max':>9}{'req/s':>8}{'MB':>8}  cloudfront"
    print(header)
    with ThreadPoolExecutor(max_workers=args.concurrency) as pool:
        for name in args.scenarios.split(","):
            urls = []
            for round_no in range(1, args.rounds + 1):
                for identifier in args.ids:
                    w, h = dims[identifier]
                    urls.extend(scenario_urls(name, base, identifier, w, h, round_no, args.unique))
            t0 = time.perf_counter()
            results = list(pool.map(lambda u: fetch(client, u), urls))
            summary = summarize(results, time.perf_counter() - t0)
            out["scenarios"][name] = summary
            cf = " ".join(f"{k}:{v['n']}@{v['median_ms']:.0f}ms" for k, v in summary["cloudfront"].items())
            print(f"{name:<9}{summary['requests']:>5}{summary['ok']:>5}{summary['median_ms']:>8.0f} "
                  f"{summary['p95_ms']:>8.0f} {summary['max_ms']:>8.0f} {summary['req_per_s']:>8.2f}"
                  f"{summary['mb']:>8.1f}  {cf}")
            if summary["errors"]:
                print(f"         errors: {summary['errors']}")
    if args.json:
        with open(args.json, "w") as f:
            json.dump(out, f, indent=1)


if __name__ == "__main__":
    main()
