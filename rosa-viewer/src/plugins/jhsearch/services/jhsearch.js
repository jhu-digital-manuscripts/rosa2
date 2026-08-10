/**
 * JHSearch service configuration loader.
 */

const DEFAULT_TIMEOUT = 15000;

/**
 * Custom error class for JHSearch configuration errors.
 */
export class JHSearchConfigError extends Error {
  constructor(message, url, cause) {
    super(message);
    this.name = 'JHSearchConfigError';
    this.url = url;
    this.cause = cause;
  }
}

/**
 * Fetch JSON from a URL with timeout.
 *
 * @param {string} url - URL to fetch
 * @param {number} timeout - Timeout in milliseconds
 * @returns {Promise<Object>} Parsed JSON response
 */
async function fetchJson(url, timeout = DEFAULT_TIMEOUT) {
  const controller = new AbortController();
  const timeoutId = setTimeout(() => controller.abort(), timeout);

  try {
    const response = await fetch(url, {
      signal: controller.signal,
    });

    clearTimeout(timeoutId);

    if (!response.ok) {
      throw new JHSearchConfigError(
        `Failed to fetch ${url}: ${response.status} ${response.statusText}`,
        url
      );
    }

    return await response.json();
  } catch (error) {
    clearTimeout(timeoutId);

    if (error.name === 'AbortError') {
      throw new JHSearchConfigError(`Request timed out: ${url}`, url);
    }

    if (error instanceof JHSearchConfigError) {
      throw error;
    }

    throw new JHSearchConfigError(`Network error fetching ${url}: ${error.message}`, url, error);
  }
}

/**
 * Extract the JHSearch service from a IIIF collection or manifest.
 *
 * @param {Object} iiifResource - IIIF Collection or Manifest
 * @returns {Object|null} JHSearch service block or null if not found
 */
export function extractJHSearchService(iiifResource) {
  const services = iiifResource.service || [];
  const serviceArray = Array.isArray(services) ? services : [services];

  return (
    serviceArray.find(
      (s) => s.type === 'JHSearchService2' || s['@type'] === 'JHSearchService2'
    ) || null
  );
}

/**
 * Load the JHSearch service configuration.
 *
 * @param {string} serviceUrl - URL of the jhsearch.json file
 * @returns {Promise<Object>} JHSearch configuration
 */
export async function loadJHSearchConfig(serviceUrl) {
  const config = await fetchJson(serviceUrl);

  // Validate required fields
  if (!config.opensearch) {
    throw new JHSearchConfigError('JHSearch config missing opensearch URL', serviceUrl);
  }

  return {
    fields: config.fields || [],
    categories: config.categories || [],
    defaultFields: config['default-fields'] || [],
    opensearchUrl: config.opensearch,
  };
}

/**
 * Load collection metadata to get label and service info.
 *
 * @param {string} collectionUrl - URL of the collection.json
 * @returns {Promise<Object>} Collection metadata
 */
export async function loadCollectionMetadata(collectionUrl) {
  const collection = await fetchJson(collectionUrl);

  // Extract label (IIIF 3.0 format with language maps)
  let label = '';
  if (collection.label) {
    if (typeof collection.label === 'string') {
      label = collection.label;
    } else if (typeof collection.label === 'object') {
      // Language map - try 'en' first, then any available
      const labelMap = collection.label;
      const langKeys = Object.keys(labelMap);
      const preferredLang = langKeys.includes('en') ? 'en' : langKeys[0];
      const labelValue = labelMap[preferredLang];
      label = Array.isArray(labelValue) ? labelValue[0] : labelValue;
    }
  }

  // Extract JHSearch service
  const jhsearchService = extractJHSearchService(collection);

  return {
    id: collection.id,
    label,
    service: jhsearchService,
    items: collection.items || [],
  };
}

/**
 * Full initialization: load collection, extract service, load jhsearch config.
 *
 * @param {string} iiifBaseUrl - Base URL for IIIF Presentation API
 * @param {string} collectionId - Collection identifier
 * @returns {Promise<Object>} Complete configuration including service config
 */
export async function initializeJHSearch(iiifBaseUrl, collectionId) {
  // Load the collection to get the service URL
  const collectionUrl = `${iiifBaseUrl}/${collectionId}/collection.json`;
  const collection = await loadCollectionMetadata(collectionUrl);

  if (!collection.service) {
    throw new JHSearchConfigError(
      `Collection ${collectionId} does not have a JHSearchService2`,
      collectionUrl
    );
  }

  // Load the JHSearch configuration from the service URL
  const serviceUrl = collection.service.id;
  const serviceConfig = await loadJHSearchConfig(serviceUrl);

  return {
    collection: {
      id: collectionId,
      label: collection.label,
      url: collectionUrl,
    },
    service: serviceConfig,
  };
}
