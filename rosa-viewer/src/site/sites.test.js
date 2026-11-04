import { describe, it, expect } from 'vitest';
import { readdirSync } from 'node:fs';
import { resolve } from 'node:path';
import { listSites } from '../../vite.config.js';

const sitesDir = resolve(import.meta.dirname, '../../sites');

/**
 * Every site has a site.config.js the build and packaging scripts can read
 * (scripts/site-collections.mjs) and that createSiteViewer() accepts.
 */
describe('site definitions', () => {
  const sites = listSites(sitesDir);

  it('lists the aor and dlmm sites', () => {
    expect(sites).toEqual(['aor', 'dlmm']);
  });

  it.each(sites)('%s has a complete site.config.js', async (site) => {
    const files = readdirSync(resolve(sitesDir, site));
    expect(files).toEqual(
      expect.arrayContaining(['index.html', 'main.js', 'site.config.js', 'site.css'])
    );

    const { default: config } = await import(`../../sites/${site}/site.config.js`);
    expect(config.id).toBe(site);
    expect(typeof config.title).toBe('string');
    expect(typeof config.jhsearch.collectionId).toBe('string');
    expect(Array.isArray(config.jhsearch.childCollectionIds)).toBe(true);
    expect(config.jhsearch.childCollectionIds).not.toContain(config.jhsearch.collectionId);
  });
});
