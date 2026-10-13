/**
 * Opensearch client service.
 */

import { SearchCache, searchCacheKey } from './searchCache';

const DEFAULT_TIMEOUT = 30000;

/**
 * Statuses worth a second attempt.
 *
 * All three mean the search backend did not answer this time, not that the query
 * was wrong. The search proxy sits in front of a small single-node domain and
 * returns 503 while it is busy, 504 when a query outruns its upstream timeout,
 * and 502 when it cannot reach the domain at all. Each of those clears in well
 * under a second, so one retry turns most of them into a successful search
 * instead of an error the user has to react to.
 *
 * 429 is deliberately absent. It means a rate limit was hit, and retrying
 * immediately is the one response guaranteed not to help.
 */
const RETRY_STATUSES = new Set([502, 503, 504]);

/** Attempts per search, including the first. */
const DEFAULT_ATTEMPTS = 2;

/** Base delay before a retry, in milliseconds. */
const RETRY_BASE_DELAY = 250;

/**
 * Upper bound on the random part of the retry delay, in milliseconds.
 *
 * Jitter matters more than the delay itself here. The viewer issues several
 * searches per interaction and multiple users browse at once, so without it a
 * burst that overloads the domain would retry in lockstep and overload it again.
 */
const RETRY_JITTER = 250;

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
 * Resolve after `ms` milliseconds.
 *
 * @param {number} ms - Delay in milliseconds
 * @returns {Promise<void>}
 */
function delay(ms) {
  return new Promise((resolve) => {
    setTimeout(resolve, ms);
  });
}

/**
 * Milliseconds to wait before attempt number `attempt` (1-based, so the first
 * retry is attempt 2).
 *
 * Honours the server's Retry-After when it sends one, since the proxy knows more
 * about the backend's state than we do. Only the delta-seconds form is handled;
 * an HTTP-date is ignored in favour of the default, which is the conservative
 * choice for a value we would otherwise have to trust a clock to interpret.
 *
 * @param {number} attempt - 1-based attempt number about to be made
 * @param {Response} [response] - Response that triggered the retry
 * @returns {number} Delay in milliseconds
 */
function retryDelay(attempt, response) {
  const retryAfter = Number(response?.headers?.get?.('Retry-After'));
  if (Number.isFinite(retryAfter) && retryAfter > 0) {
    return Math.min(retryAfter * 1000, 5000);
  }

  return RETRY_BASE_DELAY * (attempt - 1) + Math.random() * RETRY_JITTER;
}

/**
 * Perform a single POST, applying `timeout` to that attempt alone.
 *
 * @param {string} url - Request URL
 * @param {Object} query - Query body
 * @param {number} timeout - Timeout in milliseconds
 * @returns {Promise<Response>} The fetch response
 */
async function postOnce(url, query, timeout) {
  const controller = new AbortController();
  const timeoutId = setTimeout(() => controller.abort(), timeout);

  try {
    return await fetch(url, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(query),
      signal: controller.signal,
    });
  } finally {
    clearTimeout(timeoutId);
  }
}

/** Responses shared by every search in this page session. */
const searchCache = new SearchCache();

/**
 * Empty the search response cache, e.g. after the indexes have been rebuilt.
 */
export function clearSearchCache() {
  searchCache.clear();
}

/**
 * Execute a search query against Opensearch.
 *
 * Successful, complete responses are cached in memory (see searchCache.js), and
 * an identical search made while one is in flight waits for that request rather
 * than sending another. Errors and partial responses are never cached, so a
 * retry always reaches the backend. The cached response object is shared between
 * callers and must not be mutated.
 *
 * Retries once on a transient backend failure. The timeout applies per attempt,
 * so a retried search can take longer than `timeout` in total.
 *
 * @param {string} opensearchUrl - The Opensearch _search endpoint URL
 * @param {Object} query - The query body
 * @param {Object} options - Request options
 * @param {number} options.timeout - Request timeout in milliseconds, per attempt
 * @param {number} options.attempts - Attempts including the first. 1 disables retrying
 * @param {SearchCache|null} options.cache - Cache to use; null bypasses caching
 * @returns {Promise<Object>} Search response
 */
export function executeSearch(opensearchUrl, query, options = {}) {
  const { cache = searchCache } = options;

  if (!cache) {
    return fetchSearch(opensearchUrl, query, options);
  }

  const key = searchCacheKey(opensearchUrl, query);
  const cached = cache.get(key);
  if (cached) {
    return cached;
  }

  const pending = fetchSearch(opensearchUrl, query, options).then(
    (response) => {
      if (describePartialResults(response)) {
        cache.delete(key, pending);
      }
      return response;
    },
    (error) => {
      cache.delete(key, pending);
      throw error;
    }
  );

  cache.set(key, pending);
  return pending;
}

/**
 * Send a search to Opensearch, retrying a transient failure. Uncached.
 *
 * @param {string} opensearchUrl - The Opensearch _search endpoint URL
 * @param {Object} query - The query body
 * @param {Object} options - See executeSearch
 * @returns {Promise<Object>} Search response
 */
async function fetchSearch(opensearchUrl, query, options) {
  const { timeout = DEFAULT_TIMEOUT, attempts = DEFAULT_ATTEMPTS } = options;
  const totalAttempts = Math.max(attempts, 1);

  let lastError = null;

  for (let attempt = 1; attempt <= totalAttempts; attempt += 1) {
    const lastAttempt = attempt === totalAttempts;

    let response;
    try {
      response = await postOnce(opensearchUrl, query, timeout);
    } catch (error) {
      // A timeout is not retried. The proxy's own upstream timeout is far shorter
      // than ours, so if we hit this the backend is badly overloaded and a second
      // request would add to the problem rather than route around it.
      if (error.name === 'AbortError') {
        throw new OpensearchError('Opensearch request timed out', 408, null);
      }

      // A network-level failure, which includes the connection being refused and
      // CORS preflight failing. Worth one retry.
      lastError = new OpensearchError(`Network error: ${error.message}`, 0, null);
      if (lastAttempt) {
        throw lastError;
      }
      await delay(retryDelay(attempt + 1));
      continue;
    }

    if (response.ok) {
      return await response.json();
    }

    const errorText = await response.text();
    lastError = new OpensearchError(
      `Opensearch request failed: ${response.status} ${response.statusText}`,
      response.status,
      errorText
    );

    if (!RETRY_STATUSES.has(response.status) || lastAttempt) {
      throw lastError;
    }

    await delay(retryDelay(attempt + 1, response));
  }

  // Unreachable: the loop returns or throws on its final iteration.
  throw lastError;
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
 * Describe an incomplete response, or return null when the response is complete.
 *
 * Opensearch answers HTTP 200 when only some shards contributed. The two ways
 * that happens are a shard erroring and a shard running out of time, and in both
 * cases the response looks like a normal, smaller result set: `hits.total` counts
 * only the shards that answered. Reported as success it is indistinguishable
 * from a search that genuinely matched less, which is the worst possible
 * presentation — the user sees a plausible wrong answer.
 *
 * @param {Object} response - Raw Opensearch response
 * @returns {{message: string, failedShards: number, totalShards: number, timedOut: boolean}|null}
 */
function describePartialResults(response) {
  const shards = response._shards || {};
  const failedShards = typeof shards.failed === 'number' ? shards.failed : 0;
  const totalShards = typeof shards.total === 'number' ? shards.total : 0;
  const timedOut = response.timed_out === true;

  if (failedShards === 0 && !timedOut) {
    return null;
  }

  const reasons = [];
  if (failedShards > 0) {
    reasons.push(
      totalShards > 0
        ? `${failedShards} of ${totalShards} shards failed`
        : `${failedShards} shards failed`
    );
  }
  if (timedOut) {
    reasons.push('the search timed out');
  }

  return {
    message: `Showing partial results: ${reasons.join(' and ')}. Some matches may be missing.`,
    failedShards,
    totalShards,
    timedOut,
  };
}

/**
 * The key of an aggregation bucket. Terms buckets on boolean fields are keyed 1
 * and 0, which Opensearch will not accept back in a terms filter, so use true and
 * false instead.
 */
function bucketKey(bucket) {
  if (bucket.key_as_string === 'true') return true;
  if (bucket.key_as_string === 'false') return false;
  return bucket.key;
}

/**
 * Parse search results into a normalized format.
 *
 * @param {Object} response - Raw Opensearch response
 * @returns {Object} Normalized results, including a `partial` description when
 *   the response did not cover every shard
 */
export function parseSearchResults(response) {
  const hits = response.hits || {};
  const total =
    typeof hits.total === 'object'
      ? hits.total.value
      : typeof hits.total === 'number'
        ? hits.total
        : 0;

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
          key: bucketKey(bucket),
          count: bucket.doc_count,
        }));
      }
    }
  }

  const partial = describePartialResults(response);

  if (partial) {
    // Shard failures carry the mapping or field name at fault, which is the
    // detail needed to fix the query that caused them.
    console.warn('Opensearch returned partial results', {
      failedShards: partial.failedShards,
      totalShards: partial.totalShards,
      timedOut: partial.timedOut,
      failures: response._shards?.failures,
    });
  }

  return {
    total,
    items,
    aggregations,
    partial,
  };
}
