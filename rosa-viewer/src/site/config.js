/**
 * Site configuration for the rosa-viewer sites (sites/aor, sites/dlmm).
 *
 * A site definition holds everything that is fixed for a website regardless of
 * where it is deployed: which collection it shows, whether facets are enabled,
 * the MUI theme and the header banner. Values that differ between local-dev,
 * stage and prod (the IIIF Presentation and Image API base URLs) are supplied
 * separately as the site environment, which the HTML page fills in from the
 * VITE_ROSA_* variables at build time.
 */

const REQUIRED_ENV = ['iiifBaseUrl'];

/**
 * Remove a trailing slash from a URL so paths can be appended consistently.
 * @param {string} url
 * @returns {string}
 */
export function stripTrailingSlash(url) {
  return typeof url === 'string' ? url.replace(/\/+$/, '') : url;
}

/**
 * Default logo base URL: the public/logo directory copied next to the built page.
 * Vite's BASE_URL is '/' in dev and './' for site builds, so the result is
 * relative to wherever the site is hosted.
 * @param {string} [baseUrl] - Vite base URL, defaults to import.meta.env.BASE_URL
 * @returns {string}
 */
export function defaultLogoBaseUrl(baseUrl = import.meta.env.BASE_URL) {
  const base = baseUrl || '/';
  return `${base.endsWith('/') ? base : `${base}/`}logo`;
}

/**
 * Validate the site environment, returning a list of problems (empty when valid).
 * @param {Object} env
 * @returns {string[]}
 */
export function validateSiteEnv(env = {}) {
  const problems = [];
  for (const key of REQUIRED_ENV) {
    const value = env[key];
    if (!value || /^%VITE_/.test(value)) {
      problems.push(`${key} is not set`);
    }
  }
  return problems;
}

/**
 * Build the Mirador configuration for a site.
 *
 * Environment values only ever fill in deployment-specific URLs. The site's own
 * jhsearch settings (collectionId, childCollectionIds, enableFacets, ...) win,
 * so a build for one website cannot be turned into another by changing env vars.
 *
 * @param {Object} site - Site definition
 * @param {string} site.id - Short site id ('aor', 'dlmm')
 * @param {string} site.title - Document title
 * @param {Object} site.jhsearch - Fixed JHSearch settings for the site
 * @param {Object} [site.theme] - Mirador/MUI theme overrides
 * @param {Object} [site.mirador] - Additional top-level Mirador settings
 * @param {Object} env - Site environment
 * @param {string} env.iiifBaseUrl - IIIF Presentation API base URL (required)
 * @param {string} [env.imageBaseUrl] - IIIF Image API base URL
 * @param {string} [env.logoBaseUrl] - Base URL for collection logos
 * @returns {Object} Mirador configuration accepted by createViewer()
 */
export function resolveSiteConfig(site, env = {}) {
  if (!site || !site.id || !site.jhsearch) {
    throw new Error('rosa-viewer: site definition requires id and jhsearch settings');
  }

  const jhsearch = {
    iiifBaseUrl: stripTrailingSlash(env.iiifBaseUrl),
    logoBaseUrl: stripTrailingSlash(env.logoBaseUrl) || defaultLogoBaseUrl(),
    ...site.jhsearch,
  };

  if (env.imageBaseUrl) {
    jhsearch.imageBaseUrl = stripTrailingSlash(env.imageBaseUrl);
  }

  return {
    id: 'rosa-viewer',
    selectedTheme: 'light',
    ...(site.theme ? { theme: site.theme } : {}),
    ...site.mirador,
    jhsearch,
  };
}

/**
 * Build the IIIF Image API URL for a site's header banner.
 *
 * @param {Object} banner - Banner definition from the site
 * @param {string} banner.imageId - Percent-encoded IIIF image identifier
 * @param {string} [banner.region='full'] - IIIF region parameter
 * @param {number} [banner.width=1600] - Requested width in pixels
 * @param {string} imageBaseUrl - IIIF Image API base URL
 * @returns {string|null} Image URL, or null when the banner cannot be built
 */
export function buildBannerUrl(banner, imageBaseUrl) {
  if (!banner || !banner.imageId || !imageBaseUrl) {
    return null;
  }
  const region = banner.region || 'full';
  const width = banner.width || 1600;
  return `${stripTrailingSlash(imageBaseUrl)}/${banner.imageId}/${region}/${width},/0/default.jpg`;
}
