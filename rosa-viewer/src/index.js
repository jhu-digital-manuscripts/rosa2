import Mirador from 'mirador';
import { jhsearchPlugin } from './plugins/jhsearch';
import { mergeConfig } from './config';

/**
 * Initialize the Rosa Viewer with the JHSearch plugin.
 *
 * @param {Object} config - Configuration object
 * @param {Object} config.jhsearch - JHSearch plugin configuration
 * @param {string} config.jhsearch.iiifBaseUrl - Base URL for IIIF Presentation API (required)
 * @param {string} config.jhsearch.collectionId - Top-level collection ID (required)
 * @param {string[]} [config.jhsearch.childCollectionIds] - Child collection IDs
 * @param {string} [config.jhsearch.imageBaseUrl] - IIIF Image API base URL
 * @param {string} [config.jhsearch.thumbnailTemplate] - Thumbnail URL template
 * @param {number} [config.jhsearch.thumbnailWidth] - Thumbnail width in pixels
 * @param {string} [config.jhsearch.logoBaseUrl] - Base URL for collection logos
 * @param {boolean} [config.jhsearch.enableFacets] - Enable faceted browsing
 * @param {number} [config.jhsearch.pageSize] - Default results per page
 * @param {number[]} [config.jhsearch.pageSizeOptions] - Available page size options
 * @returns {Object} Mirador viewer instance
 */
export function createViewer(config = {}) {
  const mergedConfig = mergeConfig(config);

  // Validate required configuration
  if (!mergedConfig.jhsearch?.iiifBaseUrl) {
    console.error('rosa-viewer: jhsearch.iiifBaseUrl is required');
  }
  if (!mergedConfig.jhsearch?.collectionId) {
    console.error('rosa-viewer: jhsearch.collectionId is required');
  }

  return Mirador.viewer(mergedConfig, [...jhsearchPlugin]);
}

// Auto-initialize if configuration is provided on window object
if (typeof window !== 'undefined' && window.rosaViewerConfig) {
  createViewer(window.rosaViewerConfig);
}

export { jhsearchPlugin };
export default createViewer;
