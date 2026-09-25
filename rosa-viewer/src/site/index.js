/**
 * Entry point for the rosa-viewer sites (sites/aor, sites/dlmm).
 */

import { createViewer } from '../index';
import { resolveSiteConfig, validateSiteEnv } from './config';
import { applySiteBranding } from './branding';

export { resolveSiteConfig, validateSiteEnv, buildBannerUrl } from './config';
export { applySiteBranding } from './branding';

/**
 * Create a Mirador viewer for a site.
 *
 * @param {Object} site - Site definition (see resolveSiteConfig)
 * @param {Object} [env=window.rosaSiteEnv] - Site environment
 * @param {Object} [options]
 * @param {Function} [options.viewerFactory=createViewer] - Function that receives the
 *   resolved config and returns a viewer; mainly for tests
 * @returns {Object} Mirador viewer instance
 */
export function createSiteViewer(site, env = globalThis.window?.rosaSiteEnv, options = {}) {
  const { viewerFactory = createViewer } = options;
  const problems = validateSiteEnv(env);
  for (const problem of problems) {
    console.error(`rosa-viewer: site environment: ${problem}`);
  }

  const config = resolveSiteConfig(site, env);
  applySiteBranding(site, config);
  return viewerFactory(config);
}

export default createSiteViewer;
