/**
 * Opensearch client service.
 */

const DEFAULT_TIMEOUT = 30000;

/**
 * Custom error class for Opensearch errors.
 */
export class OpensearchError extends Error {
  constructor(message, status, response) {
    super(message);
    this.name = 'OpensearchError';
    this.status = status;
    this.response = response;
  }
}

/**
 * Execute a search query against Opensearch.
 *
 * @param {string} opensearchUrl - The Opensearch _search endpoint URL
 * @param {Object} query - The query body
 * @param {Object} options - Request options
 * @param {number} options.timeout - Request timeout in milliseconds
 * @returns {Promise<Object>} Search response
 */
export async function executeSearch(opensearchUrl, query, options = {}) {
  const { timeout = DEFAULT_TIMEOUT } = options;

  const controller = new AbortController();
  const timeoutId = setTimeout(() => controller.abort(), timeout);

  try {
    const response = await fetch(opensearchUrl, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(query),
      signal: controller.signal,
    });

    clearTimeout(timeoutId);

    if (!response.ok) {
      const errorText = await response.text();
      throw new OpensearchError(
        `Opensearch request failed: ${response.status} ${response.statusText}`,
        response.status,
        errorText
      );
    }

    return await response.json();
  } catch (error) {
    clearTimeout(timeoutId);

    if (error.name === 'AbortError') {
      throw new OpensearchError('Opensearch request timed out', 408, null);
    }

    if (error instanceof OpensearchError) {
      throw error;
    }

    throw new OpensearchError(`Network error: ${error.message}`, 0, null);
  }
}

/**
 * Execute a search on the manifest index.
 *
 * @param {string} opensearchUrl - Base Opensearch URL
 * @param {Object} query - The query body
 * @param {Object} options - Request options
 * @returns {Promise<Object>} Search response
 */
export async function searchManifests(opensearchUrl, query, options = {}) {
  // Opensearch URL typically ends with /_search, but we may need to target a specific index
  // If URL already contains /manifest, use it; otherwise construct it
  const url = opensearchUrl.includes('/manifest')
    ? opensearchUrl
    : opensearchUrl.replace('/_search', '/manifest/_search');

  return executeSearch(url, query, options);
}

/**
 * Execute a search on the canvas index.
 *
 * @param {string} opensearchUrl - Base Opensearch URL
 * @param {Object} query - The query body
 * @param {Object} options - Request options
 * @returns {Promise<Object>} Search response
 */
export async function searchCanvases(opensearchUrl, query, options = {}) {
  const url = opensearchUrl.includes('/canvas')
    ? opensearchUrl
    : opensearchUrl.replace('/_search', '/canvas/_search');

  return executeSearch(url, query, options);
}

/**
 * Execute a multi-index search (searches both manifest and canvas).
 *
 * @param {string} opensearchUrl - Base Opensearch URL
 * @param {Object} query - The query body
 * @param {Object} options - Request options
 * @returns {Promise<Object>} Combined search response
 */
export async function searchAll(opensearchUrl, query, options = {}) {
  // Use the base _search endpoint which searches all indices
  return executeSearch(opensearchUrl, query, options);
}

/**
 * Parse search results into a normalized format.
 *
 * @param {Object} response - Raw Opensearch response
 * @returns {Object} Normalized results
 */
export function parseSearchResults(response) {
  const hits = response.hits || {};
  const total =
    typeof hits.total === 'object' ? hits.total.value : typeof hits.total === 'number' ? hits.total : 0;

  const items = (hits.hits || []).map((hit) => ({
    id: hit._id,
    index: hit._index,
    score: hit._score,
    source: hit._source,
    highlight: hit.highlight || {},
  }));

  const aggregations = {};
  if (response.aggregations) {
    for (const [name, agg] of Object.entries(response.aggregations)) {
      if (agg.buckets) {
        aggregations[name] = agg.buckets.map((bucket) => ({
          key: bucket.key,
          count: bucket.doc_count,
        }));
      }
    }
  }

  return {
    total,
    items,
    aggregations,
  };
}
