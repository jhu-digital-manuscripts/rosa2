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
import { runSearch } from '../services/search';

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
  const enableFacets = useSelector(selectors.areFacetsEnabled);
  const rootCollectionId = useSelector(selectors.getRootCollectionId);
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
      const parsed = await runSearch({
        opensearchUrl,
        collectionId: selectedCollectionId,
        rootCollectionId,
        viewMode,
        searchMode,
        simpleQuery,
        advancedRows,
        facets,
        page: currentPage,
        pageSize,
        sortField,
        sortOrder,
        categories,
        fields,
        defaultFields,
        enableFacets,
      });
      dispatch(searchSuccess(parsed));
    } catch (err) {
      dispatch(searchFailure(err.message || 'Search failed'));
    }
  }, [
    dispatch,
    opensearchUrl,
    enableFacets,
    rootCollectionId,
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
