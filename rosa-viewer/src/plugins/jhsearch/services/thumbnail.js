/**
 * Thumbnail service for building and managing thumbnail URLs.
 */

import { buildThumbnailUrl, buildLogoUrl } from '../utils/urlBuilder';

/**
 * Default thumbnail configuration.
 */
const DEFAULT_CONFIG = {
  imageBaseUrl: '',
  thumbnailTemplate: '{imageBaseUrl}/{iiifImageId}/full/{width},/0/default.jpg',
  thumbnailWidth: 80,
  logoBaseUrl: '/logo',
};

/**
 * Create a thumbnail service instance with the given configuration.
 *
 * @param {Object} config - Thumbnail configuration
 * @returns {Object} Thumbnail service methods
 */
export function createThumbnailService(config = {}) {
  const mergedConfig = { ...DEFAULT_CONFIG, ...config };

  return {
    /**
     * Get a thumbnail URL for an IIIF image ID.
     *
     * @param {string} iiifImageId - The IIIF image identifier
     * @param {number} width - Optional width override
     * @returns {string|null} Thumbnail URL or null
     */
    getThumbnailUrl(iiifImageId, width) {
      const configWithWidth = width
        ? { ...mergedConfig, thumbnailWidth: width }
        : mergedConfig;
      return buildThumbnailUrl(iiifImageId, configWithWidth);
    },

    /**
     * Get a logo URL.
     *
     * @param {string} logo - Logo filename
     * @returns {string|null} Logo URL or null
     */
    getLogoUrl(logo) {
      return buildLogoUrl(logo, mergedConfig.logoBaseUrl);
    },

    /**
     * Get thumbnail URLs for a manifest's thumbnail array.
     *
     * @param {Object[]} thumbnails - Array of thumbnail objects with iiif_image_id
     * @param {number} maxCount - Maximum number of thumbnails to return
     * @returns {Object[]} Array of objects with url and pageNum
     */
    getManifestThumbnails(thumbnails, maxCount = 3) {
      if (!thumbnails || !Array.isArray(thumbnails)) {
        return [];
      }

      return thumbnails.slice(0, maxCount).map((thumb) => ({
        url: buildThumbnailUrl(thumb.iiif_image_id, mergedConfig),
        pageNum: thumb.page_num,
        iiifImageId: thumb.iiif_image_id,
      }));
    },

    /**
     * Get a single thumbnail URL for a canvas.
     *
     * @param {Object} canvas - Canvas object from search results
     * @returns {Object|null} Object with url, pageNum, and iiifImageId
     */
    getCanvasThumbnail(canvas) {
      if (!canvas?.iiif_image_id) {
        return null;
      }

      return {
        url: buildThumbnailUrl(canvas.iiif_image_id, mergedConfig),
        pageNum: canvas.page_num,
        iiifImageId: canvas.iiif_image_id,
      };
    },

    /**
     * Get the current configuration.
     *
     * @returns {Object} Current configuration
     */
    getConfig() {
      return { ...mergedConfig };
    },
  };
}

/**
 * Singleton thumbnail service (can be overridden with configure).
 */
let defaultService = createThumbnailService();

/**
 * Configure the default thumbnail service.
 *
 * @param {Object} config - Configuration options
 */
export function configureThumbnailService(config) {
  defaultService = createThumbnailService(config);
}

/**
 * Get the default thumbnail service.
 *
 * @returns {Object} Default thumbnail service
 */
export function getThumbnailService() {
  return defaultService;
}

export default {
  createThumbnailService,
  configureThumbnailService,
  getThumbnailService,
};
