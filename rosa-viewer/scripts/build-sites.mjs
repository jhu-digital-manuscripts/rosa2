#!/usr/bin/env node
/**
 * Build the self-contained static sites into dist/sites/<site>/.
 *
 * Usage: node scripts/build-sites.mjs [aor] [dlmm]
 *
 * With no arguments every site is built. Deployment-specific URLs are read from
 * VITE_ROSA_* variables (see .env); ROSA_BASE_PATH overrides the relative asset
 * base. The chooser page sites/index.html is copied to dist/sites/index.html.
 */

import { build } from 'vite';
import { existsSync, readdirSync } from 'node:fs';
import { copyFile, mkdir } from 'node:fs/promises';
import { dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const viewerDir = resolve(dirname(fileURLToPath(import.meta.url)), '..');
const sitesDir = resolve(viewerDir, 'sites');

// Same rule as listSites() in vite.config.js: a site is a sites/<id>/index.html.
const SITES = readdirSync(sitesDir, { withFileTypes: true })
  .filter((entry) => entry.isDirectory() && existsSync(resolve(sitesDir, entry.name, 'index.html')))
  .map((entry) => entry.name)
  .sort();

const requested = process.argv.slice(2);
const unknown = requested.filter((site) => !SITES.includes(site));
if (unknown.length > 0) {
  console.error(`Unknown site(s): ${unknown.join(', ')}. Expected: ${SITES.join(', ')}`);
  process.exit(1);
}
const sites = requested.length > 0 ? requested : SITES;

for (const site of sites) {
  console.log(`\nBuilding site "${site}"...`);
  process.env.ROSA_SITE = site;
  await build({ configFile: resolve(viewerDir, 'vite.config.js') });
}

const outDir = resolve(viewerDir, 'dist/sites');
await mkdir(outDir, { recursive: true });
await copyFile(resolve(viewerDir, 'sites/index.html'), resolve(outDir, 'index.html'));

console.log(`\nSites written to ${outDir}: ${sites.join(', ')}`);
