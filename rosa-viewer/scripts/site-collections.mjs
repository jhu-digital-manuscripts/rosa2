#!/usr/bin/env node
/**
 * Print the collections a site shows, space-separated: its own collection
 * followed by its child collections, read from sites/<site>/site.config.js.
 *
 * Usage: node scripts/site-collections.mjs <site>
 *
 * package-site.sh uses this to generate only the IIIF files a site needs, so the
 * site definition is the one place that says which collections a site has.
 */

import { dirname, resolve } from 'node:path';
import { fileURLToPath, pathToFileURL } from 'node:url';
import { listSites } from '../vite.config.js';

const viewerDir = resolve(dirname(fileURLToPath(import.meta.url)), '..');
const sites = listSites();
const site = process.argv[2];

if (!site || !sites.includes(site)) {
  console.error(`Usage: site-collections.mjs <site>  (one of: ${sites.join(', ')})`);
  process.exit(1);
}

const { default: config } = await import(
  pathToFileURL(resolve(viewerDir, 'sites', site, 'site.config.js')).href
);
const { collectionId, childCollectionIds = [] } = config.jhsearch || {};
if (!collectionId) {
  console.error(`sites/${site}/site.config.js has no jhsearch.collectionId`);
  process.exit(1);
}
console.log([collectionId, ...childCollectionIds].join(' '));
