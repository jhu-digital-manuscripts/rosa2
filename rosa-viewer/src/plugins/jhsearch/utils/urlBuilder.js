/**
 * URL construction utilities for IIIF resources.
 */

/**
 * Build a thumbnail URL from an IIIF image ID.
 *
 * @param {string} iiifImageId - The IIIF image identifier (e.g., 'rose/Douce195/cropped/Douce195.001r')
 * @param {Object} config - Configuration object
 * @param {string} config.imageBaseUrl - Base URL for IIIF Image API
 * @param {string} config.thumbnailTemplate - URL template with placeholders
 * @param {number} config.thumbnailWidth - Thumbnail width in pixels
 * @returns {string} Complete thumbnail URL
 */
export function buildThumbnailUrl(iiifImageId, config) {
  if (!iiifImageId || !config.imageBaseUrl) {
    return null;
  }

  // Percent-encode the image ID (slashes become %2F)
  const encoded = encodeURIComponent(iiifImageId);

  return config.thumbnailTemplate
    .replace('{imageBaseUrl}', config.imageBaseUrl)
    .replace('{iiifImageId}', encoded)
    .replace('{width}', String(config.thumbnailWidth));
}

/**
 * Build a IIIF manifest URL from a manifest ID.
 *
 * @param {string} manifestId - Manifest ID in format 'collection.book' (e.g., 'rose.Douce195')
 * @param {string} iiifBaseUrl - Base URL for IIIF Presentation API
 * @returns {string} Complete manifest URL
 */
export function buildManifestUrl(manifestId, iiifBaseUrl) {
  if (!manifestId || !iiifBaseUrl) {
    return null;
  }

  const dotIndex = manifestId.indexOf('.');
  if (dotIndex === -1) {
    console.warn(`Invalid manifest ID format: ${manifestId}`);
    return null;
  }

  const collectionId = manifestId.substring(0, dotIndex);
  const bookId = manifestId.substring(dotIndex + 1);

  return `${iiifBaseUrl}/${collectionId}/${bookId}/manifest.json`;
}

/**
 * Build a IIIF canvas URL from manifest ID and page number.
 *
 * @param {string} manifestId - Manifest ID in format 'collection.book'
 * @param {number} pageNum - 0-based page number
 * @param {string} iiifBaseUrl - Base URL for IIIF Presentation API
 * @returns {string} Complete canvas URL
 */
export function buildCanvasUrl(manifestId, pageNum, iiifBaseUrl) {
  if (!manifestId || pageNum === undefined || !iiifBaseUrl) {
    return null;
  }

  const dotIndex = manifestId.indexOf('.');
  if (dotIndex === -1) {
    console.warn(`Invalid manifest ID format: ${manifestId}`);
    return null;
  }

  const collectionId = manifestId.substring(0, dotIndex);
  const bookId = manifestId.substring(dotIndex + 1);

  return `${iiifBaseUrl}/${collectionId}/${bookId}/canvas/${pageNum}`;
}

/**
 * Build a collection URL.
 *
 * @param {string} collectionId - Collection identifier
 * @param {string} iiifBaseUrl - Base URL for IIIF Presentation API
 * @returns {string} Complete collection URL
 */
export function buildCollectionUrl(collectionId, iiifBaseUrl) {
  if (!collectionId || !iiifBaseUrl) {
    return null;
  }

  return `${iiifBaseUrl}/${collectionId}/collection.json`;
}

/**
 * Build a logo URL.
 *
 * @param {string} logo - Logo filename (e.g., 'rose.jpg')
 * @param {string} logoBaseUrl - Base URL for logos
 * @returns {string} Complete logo URL
 */
export function buildLogoUrl(logo, logoBaseUrl) {
  if (!logo) {
    return null;
  }

  return `${logoBaseUrl}/${logo}`;
}

/**
 * Extract collection ID and book ID from a manifest ID.
 *
 * @param {string} manifestId - Manifest ID in format 'collection.book'
 * @returns {Object|null} Object with collectionId and bookId, or null if invalid
 */
export function parseManifestId(manifestId) {
  if (!manifestId) {
    return null;
  }

  const dotIndex = manifestId.indexOf('.');
  if (dotIndex === -1) {
    return null;
  }

  return {
    collectionId: manifestId.substring(0, dotIndex),
    bookId: manifestId.substring(dotIndex + 1),
  };
}
