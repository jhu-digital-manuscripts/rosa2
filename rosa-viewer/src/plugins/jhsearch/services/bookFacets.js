/**
 * Loads the facet fields of every book in a collection, for working out the facets
 * of searches that match pages (see utils/bookFacets.js).
 */

import { searchManifests } from './opensearch';
import { BOOK_FACET_PAGE_SIZE, buildBookFacetQuery } from '../utils/bookFacets';

/** Loads by collection, kept for the page session like search responses. */
const loads = new Map();

/** Forget every loaded collection, e.g. after the indexes have been rebuilt. */
export function clearBookFacets() {
  loads.clear();
}

/**
 * Fetch every page of books in the collection: the first to learn the total, then
 * the rest together.
 */
async function fetchBookFacets(opensearchUrl, collectionId, categories) {
  const page = (number) =>
    searchManifests(opensearchUrl, buildBookFacetQuery({ collectionId, categories, page: number }));

  const first = await page(0);
  const total = first.hits?.total?.value ?? first.hits?.total ?? 0;
  const pageCount = Math.ceil(total / BOOK_FACET_PAGE_SIZE);

  const rest = await Promise.all(
    Array.from({ length: Math.max(pageCount - 1, 0) }, (_, i) => page(i + 1))
  );

  return [first, ...rest].flatMap((response) =>
    (response.hits?.hits || []).map((hit) => hit._source || {}).filter((book) => book.id)
  );
}

/**
 * The facet fields and `collection_id` of every book in a collection. Loaded once
 * per collection and set of categories; concurrent callers share the request, and a
 * failed load is forgotten so the next call tries again. Load the site's top-level
 * collection and use booksInCollection for its children, so switching collection
 * needs no request.
 *
 * @param {string} opensearchUrl - Base Opensearch URL
 * @param {string} collectionId - Collection ID
 * @param {Object[]} categories - Category definitions from jhsearch.json
 * @returns {Promise<Object[]>} Each book's `id`, `collection_id` and category fields
 */
export function loadBookFacets(opensearchUrl, collectionId, categories) {
  const key = JSON.stringify([opensearchUrl, collectionId, categories.map((c) => c.name)]);
  let load = loads.get(key);
  if (!load) {
    load = fetchBookFacets(opensearchUrl, collectionId, categories);
    loads.set(key, load);
    load.catch(() => {
      if (loads.get(key) === load) {
        loads.delete(key);
      }
    });
  }
  return load;
}
