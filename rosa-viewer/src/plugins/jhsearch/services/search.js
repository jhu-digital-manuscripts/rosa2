/**
 * Run the search for the current JHSearch state: build the query for the view and
 * search mode, send it, and parse the results.
 */

import { searchManifests, searchAll, parseSearchResults } from './opensearch';
import { loadBookFacets } from './bookFacets';
import { booksInCollection, computeBookFacetCounts, getMatchedBookIds } from '../utils/bookFacets';
import {
  buildBrowseQuery,
  buildSimpleSearchQuery,
  buildAdvancedSearchQuery,
} from '../utils/queryBuilder';

/**
 * Run a browse, simple or advanced search.
 *
 * Searches match pages as well as books, and the facet fields are on books only, so
 * their facets are worked out from every book's fields (utils/bookFacets.js): the
 * counts are of the distinct books matched directly or through their pages. Browsing
 * returns books only and uses Opensearch's aggregations. If the books cannot be
 * loaded, searches fall back to those too, and facets then miss the books of
 * matching pages.
 *
 * A search with nothing to search for (text of only quotes, or advanced rows that all
 * name fields the collection lacks) is a browse: sent to both indexes, a query
 * without a search clause would list every page of the collection beside its books.
 *
 * @param {Object} params
 * @param {string} params.opensearchUrl - Base Opensearch URL
 * @param {string} params.collectionId - Selected collection
 * @param {string} [params.rootCollectionId] - The site's top-level collection, whose
 *   books are loaded once for it and all its children. Defaults to collectionId
 * @param {string} params.viewMode - 'browse' or a search view
 * @param {string} params.searchMode - 'simple' or 'advanced'
 * @param {string} params.simpleQuery - Simple search text
 * @param {Object[]} params.advancedRows - Advanced search rows
 * @param {Object} params.facets - Selected facet values
 * @param {number} params.page - Page number (0-based)
 * @param {number} params.pageSize - Results per page
 * @param {string} params.sortField - Sort field
 * @param {string} params.sortOrder - Sort order
 * @param {Object[]} params.categories - Category definitions from jhsearch.json
 * @param {Object[]} params.fields - Field definitions from jhsearch.json
 * @param {string[]} params.defaultFields - Fields a simple search covers
 * @param {boolean} [params.enableFacets] - Whether the site shows facets
 * @returns {Promise<Object>} Parsed results, see parseSearchResults
 */
export async function runSearch({
  opensearchUrl,
  collectionId,
  rootCollectionId,
  viewMode,
  searchMode,
  simpleQuery,
  advancedRows,
  facets,
  page,
  pageSize,
  sortField,
  sortOrder,
  categories,
  fields,
  defaultFields,
  enableFacets = true,
}) {
  const browse = async () => {
    // Books have no relevance or page order; those sorts fall back to the title
    const byTitle = sortField === 'label.keyword';
    const query = buildBrowseQuery({
      collectionId,
      facets,
      page,
      pageSize,
      categories,
      sortField: 'label.keyword',
      sortOrder: byTitle ? sortOrder : 'asc',
    });
    return parseSearchResults(await searchManifests(opensearchUrl, query));
  };

  if (viewMode === 'browse') {
    return browse();
  }

  let books = null;
  if (enableFacets && categories.length > 0) {
    try {
      const root = rootCollectionId || collectionId;
      const all = await loadBookFacets(opensearchUrl, root, categories);
      books = booksInCollection(all, collectionId);
    } catch (err) {
      console.warn('Could not load book facets; counting only matching books', err);
    }
  }

  const common = {
    fieldDefinitions: fields,
    collectionId,
    facets,
    page,
    pageSize,
    categories,
    books,
    sortField,
    sortOrder,
  };
  const query =
    searchMode === 'simple'
      ? buildSimpleSearchQuery({ ...common, query: simpleQuery, defaultFields })
      : buildAdvancedSearchQuery({ ...common, rows: advancedRows });
  if (!query) {
    return browse();
  }

  // Search both indexes
  const response = await searchAll(opensearchUrl, query);
  const results = parseSearchResults(response);
  if (books) {
    results.aggregations = computeBookFacetCounts(books, getMatchedBookIds(response), categories);
  }
  return results;
}
