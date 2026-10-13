/**
 * Entry point for the rosa-viewer sites (sites/aor, sites/dlmm).
 */

import { createViewer } from '../index';
import { annotationStylesheetUrl, resolveSiteConfig, validateSiteEnv } from './config';
import { addStylesheet } from '../plugins/annotations/utils/stylesheet';
import { applySiteBranding } from './branding';
import { connectHistory } from './history';

export {
  resolveSiteConfig,
  validateSiteEnv,
  buildBannerUrl,
  annotationStylesheetUrl,
} from './config';
export { applySiteBranding } from './branding';

/**
 * Create a Mirador viewer for a site.
 *
 * @param {Object} site - Site definition (see resolveSiteConfig)
 * @param {Object} [env=window.rosaSiteEnv] - Site environment
 * @param {Object} [options]
 * @param {Function} [options.viewerFactory=createViewer] - Function that receives the
 *   resolved config and returns a viewer; mainly for tests
 * @param {boolean} [options.history=true] - Keep the URL and browser history in step
 *   with the viewer (see history/historySync.js)
 * @returns {Object} Mirador viewer instance
 */
export function createSiteViewer(site, env = globalThis.window?.rosaSiteEnv, options = {}) {
  const { viewerFactory = createViewer, history = true } = options;
  const problems = validateSiteEnv(env);
  for (const problem of problems) {
    console.error(`rosa-viewer: site environment: ${problem}`);
  }

  const config = resolveSiteConfig(site, env);
  applySiteBranding(site, config);
  // Per-collection annotation styles from public/styles/
  addStylesheet(annotationStylesheetUrl(site, env));
  const viewer = viewerFactory(config);
  if (history && typeof viewer?.store?.subscribe === 'function') {
    connectHistory(viewer.store, { siteTitle: site.title });
  }
  return viewer;
}

export default createSiteViewer;
