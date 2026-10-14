/**
 * Load and parse a TEI book description. Results are cached by URL for the
 * session, so reopening the panel or another window on the same book does not
 * fetch it again.
 */

import { useEffect, useState } from 'react';
import { parseDescription } from '../utils/tei';

const cache = new Map();

/** Forget cached descriptions (for tests). */
export function clearBookDescriptionCache() {
  cache.clear();
}

/**
 * Fetch and parse a description.
 * @param {string} url
 * @param {Object} [options]
 * @param {AbortSignal} [options.signal]
 * @param {Function} [options.fetchImpl=fetch]
 * @returns {Promise<{sections: Array}|null>}
 */
export async function loadBookDescription(url, { signal, fetchImpl = globalThis.fetch } = {}) {
  if (cache.has(url)) return cache.get(url);
  const response = await fetchImpl(url, { signal });
  if (!response.ok) {
    throw new Error(`Failed to load the book description (HTTP ${response.status})`);
  }
  const description = parseDescription(await response.text());
  cache.set(url, description);
  return description;
}

const IDLE = { status: 'idle', description: null, error: null };

/**
 * @param {string|null} url - Description URL, or null when there is none
 * @returns {{status: 'idle'|'loading'|'ready'|'empty'|'error', description: Object|null, error: Error|null}}
 */
export function useBookDescription(url) {
  const [state, setState] = useState(() => {
    if (!url) return IDLE;
    if (cache.has(url)) return settled(cache.get(url));
    return { status: 'loading', description: null, error: null };
  });

  useEffect(() => {
    if (!url) {
      setState(IDLE);
      return undefined;
    }
    if (cache.has(url)) {
      setState(settled(cache.get(url)));
      return undefined;
    }
    const controller = new AbortController();
    setState({ status: 'loading', description: null, error: null });
    loadBookDescription(url, { signal: controller.signal })
      .then((description) => {
        if (!controller.signal.aborted) setState(settled(description));
      })
      .catch((error) => {
        if (controller.signal.aborted || error?.name === 'AbortError') return;
        console.error('rosa-viewer: book description:', error);
        setState({ status: 'error', description: null, error });
      });
    return () => controller.abort();
  }, [url]);

  return state;
}

function settled(description) {
  return description
    ? { status: 'ready', description, error: null }
    : { status: 'empty', description: null, error: null };
}
