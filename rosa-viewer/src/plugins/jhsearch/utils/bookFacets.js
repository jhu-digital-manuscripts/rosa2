/**
 * Facets for searches that match pages as well as books.
 *
 * Every facet category is a book (manifest) field. Simple and advanced searches run
 * over both indexes, and a canvas document carries none of those fields, so an
 * Opensearch aggregation over the hits counts only the books that matched directly,
 * and a facet filter on the field drops every page. A search that matches only
 * transcriptions would show no facet values at all.
 *
 * Collections hold a few hundred books, so the viewer loads the facet fields of
 * every book once and works facets out itself. It loads the site's top-level
 * collection, whose books include those of every child collection, and picks a child
 * collection's books from that by their `collection_id` (booksInCollection).
 *
 * - The search asks for the ids of the books it matched, either directly or through
 *   one of their pages (buildMatchedBookAggregations), and the facet counts are the
 *   number of those books with each value (computeBookFacetCounts).
 * - Selected facet values are resolved to the books they allow, and the search is
 *   filtered to those books by `id` or `manifest_id`, like the Name filter
 *   (buildAllowedBooksFilter).
 */

import { getBucketRange, getQuantizeInterval } from './facets';

/**
 * Books fetched per request when loading a collection. This is the search proxy's
 * size limit, so a collection of up to this many books loads in one request.
 */
export const BOOK_FACET_PAGE_SIZE = 300;

/**
 * Most buckets asked for when collecting the matched books. This is the search
 * proxy's limit; a search matching more books than this undercounts its facets.
 */
export const MAX_MATCHED_BOOKS = 500;

/**
 * Most values in one `terms` clause, the search proxy's limit. Longer id lists are
 * split over several clauses.
 */
export const MAX_TERMS_VALUES = 256;

/** Aggregation of the books of matching pages. */
export const MATCHED_PAGE_BOOKS_AGG = 'matched_page_books';

/** Aggregation of the books that matched directly. */
export const MATCHED_BOOKS_AGG = 'matched_books';

/**
 * Build the query for one page of the facet fields of every book in a collection,
 * with the collections each book is in.
 *
 * @param {Object} options
 * @param {string} options.collectionId - Collection ID
 * @param {Object[]} options.categories - Category definitions from jhsearch.json
 * @param {number} [options.page] - Page number (0-based)
 * @returns {Object} Opensearch query body for the manifest index
 */
export function buildBookFacetQuery({ collectionId, categories, page = 0 }) {
  return {
    query: {
      bool: {
        filter: [{ terms: { collection_id: [collectionId] } }],
      },
    },
    from: page * BOOK_FACET_PAGE_SIZE,
    size: BOOK_FACET_PAGE_SIZE,
    sort: [{ id: 'asc' }],
    _source: ['id', 'collection_id', ...categories.map((category) => category.name)],
    track_total_hits: true,
  };
}

/**
 * The books in a collection, picked by their `collection_id` from books loaded for
 * a collection that contains it.
 *
 * @param {Object[]} books - Loaded books, each with its `collection_id` list
 * @param {string} collectionId - Collection ID
 * @returns {Object[]} The books in that collection
 */
export function booksInCollection(books, collectionId) {
  return books.filter((book) => fieldValues(book, 'collection_id').includes(collectionId));
}

/**
 * Aggregations collecting the ids of the books a search matched. Pages give their
 * book in `manifest_id`; books are the hits without one and give their own `id`.
 *
 * @returns {Object} Aggregations for a search over both indexes
 */
export function buildMatchedBookAggregations() {
  return {
    [MATCHED_PAGE_BOOKS_AGG]: {
      terms: { field: 'manifest_id', size: MAX_MATCHED_BOOKS },
    },
    [MATCHED_BOOKS_AGG]: {
      filter: { bool: { must_not: [{ exists: { field: 'manifest_id' } }] } },
      aggs: {
        ids: { terms: { field: 'id', size: MAX_MATCHED_BOOKS } },
      },
    },
  };
}

/**
 * Read the matched book ids out of a response to a search that used
 * buildMatchedBookAggregations.
 *
 * @param {Object} response - Raw Opensearch response
 * @returns {Set<string>} Ids of the books matched directly or through a page
 */
export function getMatchedBookIds(response) {
  const aggs = response?.aggregations || {};
  const buckets = [
    ...(aggs[MATCHED_PAGE_BOOKS_AGG]?.buckets || []),
    ...(aggs[MATCHED_BOOKS_AGG]?.ids?.buckets || []),
  ];
  return new Set(buckets.map((bucket) => String(bucket.key)));
}

/** The values of a field as an array: absent is none, an array is its items. */
function fieldValues(book, name) {
  const value = book[name];
  if (value === undefined || value === null) {
    return [];
  }
  return Array.isArray(value) ? value : [value];
}

/**
 * The facet value a field value is counted under: the lower bound of its bucket for
 * a quantized category, the value itself otherwise.
 */
function facetKey(value, interval) {
  if (interval === null) {
    return value;
  }
  const number = Number(value);
  return Number.isFinite(number) ? Math.floor(number / interval) * interval : null;
}

/**
 * Whether a field value falls under a selected facet value. Selected values restored
 * from a URL are strings, so plain values compare as strings.
 */
function valueMatches(value, selected, interval) {
  if (interval === null) {
    return String(value) === String(selected);
  }
  const range = getBucketRange(selected, interval);
  const number = Number(value);
  return range !== null && Number.isFinite(number) && number >= range.gte && number < range.lt;
}

/**
 * Whether a book matches the selected facet values: any selected value within a
 * category, and every category with a selection.
 *
 * @param {Object} book - A book's facet fields, keyed by category name
 * @param {Object} facets - Category name to selected values
 * @param {Object[]} categories - Category definitions from jhsearch.json
 * @returns {boolean}
 */
export function bookMatchesFacets(book, facets, categories) {
  return Object.entries(facets).every(([name, selected]) => {
    if (!selected || selected.length === 0) {
      return true;
    }
    const interval = getQuantizeInterval(categories.find((c) => c.name === name));
    return fieldValues(book, name).some((value) =>
      selected.some((s) => valueMatches(value, s, interval))
    );
  });
}

/**
 * Count, for each category, the matched books with each value. The result has the
 * shape parseSearchResults gives aggregations, so the facet panel shows it as is.
 *
 * @param {Object[]} books - Facet fields of every book in the collection
 * @param {Set<string>} matchedIds - Ids of the books the search matched
 * @param {Object[]} categories - Category definitions from jhsearch.json
 * @returns {Object} Category name to [{key, count}]
 */
export function computeBookFacetCounts(books, matchedIds, categories) {
  const matched = books.filter((book) => matchedIds.has(String(book.id)));
  const aggregations = {};

  for (const category of categories) {
    const interval = getQuantizeInterval(category);
    const counts = new Map();

    for (const book of matched) {
      // A book with a value twice is still one book under it.
      const keys = new Set(
        fieldValues(book, category.name)
          .map((value) => facetKey(value, interval))
          .filter((key) => key !== null)
      );
      for (const key of keys) {
        counts.set(key, (counts.get(key) || 0) + 1);
      }
    }

    aggregations[category.name] = [...counts].map(([key, count]) => ({ key, count }));
  }

  return aggregations;
}

/** `terms` clauses on `field` for `values`, split to stay within the proxy's limit. */
function termsClauses(field, values) {
  const clauses = [];
  for (let i = 0; i < values.length; i += MAX_TERMS_VALUES) {
    clauses.push({ terms: { [field]: values.slice(i, i + MAX_TERMS_VALUES) } });
  }
  return clauses;
}

/**
 * Build the filter limiting a search over both indexes to the books that match the
 * selected facet values, and to their pages.
 *
 * The books are named by id, so the clause lists whichever is shorter: the allowed
 * books, or the excluded ones in a `must_not`.
 *
 * @param {Object[]} books - Facet fields of every book in the collection
 * @param {Object} facets - Category name to selected values; the Name filter excluded
 * @param {Object[]} categories - Category definitions from jhsearch.json
 * @returns {Object|null} Filter clause, or null when nothing is selected or excluded
 */
export function buildAllowedBooksFilter(books, facets, categories) {
  const active = Object.entries(facets).filter(([, values]) => values && values.length > 0);
  if (active.length === 0) {
    return null;
  }

  const selected = Object.fromEntries(active);
  const allowed = [];
  const excluded = [];
  for (const book of books) {
    (bookMatchesFacets(book, selected, categories) ? allowed : excluded).push(String(book.id));
  }

  if (allowed.length === 0) {
    return { match_none: {} };
  }
  if (excluded.length === 0) {
    return null;
  }

  const ids = allowed.length <= excluded.length ? allowed : excluded;
  const clauses = [...termsClauses('id', ids), ...termsClauses('manifest_id', ids)];

  return allowed.length <= excluded.length
    ? { bool: { should: clauses, minimum_should_match: 1 } }
    : { bool: { must_not: clauses } };
}
