/**
 * In-memory cache for search responses.
 *
 * The indexes change only when a new archive build is ingested, which happens
 * rarely, while the viewer repeats the same searches constantly: going back and
 * forward through history, paging back to a page already seen, or returning to
 * the browse view all rebuild a query that was sent moments ago. Caching the
 * response makes those transitions instant and takes load off a small search
 * domain.
 *
 * The cache lives for the page session. A reload starts empty, so the worst case
 * after a re-index is that an open tab shows the old results until it is
 * reloaded or the entry expires.
 */

/** How long an entry stays usable, in milliseconds. */
export const DEFAULT_TTL = 30 * 60 * 1000;

/**
 * Most entries kept at once. A response is at most a page of hits plus facet
 * buckets, so this bounds memory to a few megabytes.
 */
export const DEFAULT_MAX_ENTRIES = 100;

/**
 * A size-bounded, time-limited LRU map.
 *
 * Values are stored as given; the search service stores promises so that
 * identical searches issued while one is in flight share a single request.
 */
export class SearchCache {
  /**
   * @param {Object} [options]
   * @param {number} [options.ttl] - Entry lifetime in milliseconds
   * @param {number} [options.maxEntries] - Maximum number of entries
   * @param {() => number} [options.now] - Clock, for tests
   */
  constructor({ ttl = DEFAULT_TTL, maxEntries = DEFAULT_MAX_ENTRIES, now = Date.now } = {}) {
    this.ttl = ttl;
    this.maxEntries = maxEntries;
    this.now = now;
    // Map iteration order is insertion order, so the first key is the least
    // recently used once get() re-inserts what it returns.
    this.entries = new Map();
  }

  /**
   * Return the value for `key`, or undefined when absent or expired.
   *
   * @param {string} key
   * @returns {*}
   */
  get(key) {
    const entry = this.entries.get(key);
    if (!entry) {
      return undefined;
    }

    if (entry.expires <= this.now()) {
      this.entries.delete(key);
      return undefined;
    }

    this.entries.delete(key);
    this.entries.set(key, entry);
    return entry.value;
  }

  /**
   * Store `value` under `key`, evicting the least recently used entry when full.
   *
   * @param {string} key
   * @param {*} value
   */
  set(key, value) {
    this.entries.delete(key);
    this.entries.set(key, { value, expires: this.now() + this.ttl });

    while (this.entries.size > this.maxEntries) {
      this.entries.delete(this.entries.keys().next().value);
    }
  }

  /**
   * Remove `key`, but only while it still maps to `value` when one is given.
   * That stops a failed request from evicting a newer entry stored under the
   * same key after the cache was cleared.
   *
   * @param {string} key
   * @param {*} [value]
   */
  delete(key, value) {
    if (value === undefined || this.entries.get(key)?.value === value) {
      this.entries.delete(key);
    }
  }

  /** Remove every entry. */
  clear() {
    this.entries.clear();
  }

  /** Number of entries, including any that have expired but not been read. */
  get size() {
    return this.entries.size;
  }
}

/**
 * Cache key for a search: the endpoint and the exact query body. The query
 * builders always assemble keys in the same order, so equal searches serialize
 * identically.
 *
 * @param {string} url - Search endpoint
 * @param {Object} query - Query body
 * @returns {string}
 */
export function searchCacheKey(url, query) {
  return `${url}\n${JSON.stringify(query)}`;
}
