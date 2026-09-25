/**
 * Apply a site's branding to the document: title and header banner image.
 * The header markup itself lives in sites/<site>/index.html; this only fills in
 * the parts that depend on runtime configuration.
 */

import { buildBannerUrl } from './config';

/**
 * @param {Object} site - Site definition (see resolveSiteConfig)
 * @param {Object} config - Resolved Mirador configuration
 * @param {Document} [doc=document]
 */
export function applySiteBranding(site, config, doc = globalThis.document) {
  if (!doc) return;

  if (site.title) {
    doc.title = site.title;
  }

  const bannerUrl = buildBannerUrl(site.banner, config.jhsearch?.imageBaseUrl);
  const root = doc.documentElement;
  if (bannerUrl) {
    root.style.setProperty('--rosa-banner-image', `url("${bannerUrl}")`);
    root.classList.add('rosa-has-banner');
  } else {
    root.style.removeProperty('--rosa-banner-image');
    root.classList.remove('rosa-has-banner');
  }
}
