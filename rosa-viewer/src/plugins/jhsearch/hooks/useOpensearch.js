/**
 * useOpensearch hook - execute searches and access results.
 */

import { useCallback } from 'react';
import { useSelector, useDispatch } from 'react-redux';
import * as selectors from '../state/selectors';
import {
  searchRequest,
  searchSuccess,
  searchFailure,
  setPage,
  setPageSize,
  setSort,
} from '../state/actions';
import { searchManifests, searchAll, parseSearchResults } from '../services/opensearch';
import {
  buildBrowseQuery,
  buildSimpleSearchQuery,
  buildAdvancedSearchQuery,
} from '../utils/queryBuilder';

/**
 * Hook for executing searches and managing search state.
 *
 * @returns {Object} Search state and methods
 */
export function useOpensearch() {
  const dispatch = useDispatch();

  // Selectors
  const results = useSelector(selectors.getResultItems);
  const isLoading = useSelector(selectors.isResultsLoading);
  const error = useSelector(selectors.getResultsError);
  const total = useSelector(selectors.getTotalResults);
  const currentPage = useSelector(selectors.getCurrentPage);
  const pageSize = useSelector(selectors.getPageSize);
  const totalPages = useSelector(selectors.getTotalPages);
  const aggregations = useSelector(selectors.getAggregations);

  const viewMode = useSelector(selectors.getViewMode);
  const searchMode = useSelector(selectors.getSearchMode);
  const simpleQuery = useSelector(selectors.getSimpleQuery);
  const advancedRows = useSelector(selectors.getAdvancedRows);
  const facets = useSelector(selectors.getFacets);
  const selectedCollectionId = useSelector(selectors.getSelectedCollectionId);
  const sortField = useSelector(selectors.getSortField);
  const sortOrder = useSelector(selectors.getSortOrder);

  const opensearchUrl = useSelector(selectors.getOpensearchUrl);
  const categories = useSelector(selectors.getCategories);
  const fields = useSelector(selectors.getFields);
  const defaultFields = useSelector(selectors.getDefaultFields);

  /**
   * Execute a search with the current state.
   */
  const executeSearch = useCallback(async () => {
    if (!opensearchUrl || !selectedCollectionId) {
      return;
    }

    dispatch(searchRequest());

    try {
      let query;
      let searchFn = searchManifests;

      if (viewMode === 'browse') {
        query = buildBrowseQuery({
          collectionId: selectedCollectionId,
          facets,
          page: currentPage,
          pageSize,
          categories,
          sortField: sortField === '_score' ? 'label.keyword' : sortField,
          sortOrder: sortField === '_score' ? 'asc' : sortOrder,
        });
      } else if (searchMode === 'simple') {
        query = buildSimpleSearchQuery({
          query: simpleQuery,
          defaultFields,
          fieldDefinitions: fields,
          collectionId: selectedCollectionId,
          facets,
          page: currentPage,
          pageSize,
          categories,
          sortField,
          sortOrder,
        });
        searchFn = searchAll;
      } else {
        query = buildAdvancedSearchQuery({
          rows: advancedRows,
          fieldDefinitions: fields,
          collectionId: selectedCollectionId,
          facets,
          page: currentPage,
          pageSize,
          categories,
          sortField,
          sortOrder,
        });
        searchFn = searchAll;
      }

      const response = await searchFn(opensearchUrl, query);
      const parsed = parseSearchResults(response);
      dispatch(searchSuccess(parsed));
    } catch (err) {
      dispatch(searchFailure(err.message || 'Search failed'));
    }
  }, [
    dispatch,
    opensearchUrl,
    selectedCollectionId,
    viewMode,
    searchMode,
    simpleQuery,
    advancedRows,
    facets,
    currentPage,
    pageSize,
    sortField,
    sortOrder,
    categories,
    fields,
    defaultFields,
  ]);

  /**
   * Navigate to a specific page.
   */
  const goToPage = useCallback(
    (page) => {
      dispatch(setPage(page));
    },
    [dispatch]
  );

  /**
   * Change the page size.
   */
  const changePageSize = useCallback(
    (size) => {
      dispatch(setPageSize(size));
    },
    [dispatch]
  );

  /**
   * Change the sort order.
   */
  const changeSort = useCallback(
    (field, order) => {
      dispatch(setSort(field, order));
    },
    [dispatch]
  );

  return {
    // State
    results,
    isLoading,
    error,
    total,
    currentPage,
    pageSize,
    totalPages,
    aggregations,

    // Methods
    executeSearch,
    goToPage,
    changePageSize,
    changeSort,
  };
}

export default useOpensearch;
