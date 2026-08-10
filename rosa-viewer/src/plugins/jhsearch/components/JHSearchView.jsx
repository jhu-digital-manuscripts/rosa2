/**
 * JHSearchView - Main container component for the JHSearch plugin.
 * This replaces Mirador's default WorkspaceAdd component.
 */

import React, { useEffect, useCallback } from 'react';
import { useSelector, useDispatch } from 'react-redux';
import Box from '@mui/material/Box';
import CircularProgress from '@mui/material/CircularProgress';
import Typography from '@mui/material/Typography';

import { JHSearchSidebar } from './JHSearchSidebar';
import { SearchResults } from './SearchResults';
import { InitializationError } from './ErrorDisplay';

import * as selectors from '../state/selectors';
import {
  loadConfigSuccess,
  loadServiceRequest,
  loadServiceSuccess,
  loadServiceFailure,
  loadCollectionsSuccess,
  searchRequest,
  searchSuccess,
  searchFailure,
} from '../state/actions';
import { initializeJHSearch } from '../services/jhsearch';
import { searchManifests, searchAll, parseSearchResults } from '../services/opensearch';
import {
  buildBrowseQuery,
  buildSimpleSearchQuery,
  buildAdvancedSearchQuery,
} from '../utils/queryBuilder';

/**
 * JHSearchView is the main container for the search/browse interface.
 */
export function JHSearchView({ jhsearchConfig }) {
  const dispatch = useDispatch();

  const isInitialized = useSelector(selectors.isInitialized);
  const initError = useSelector(selectors.getInitializationError);
  const config = useSelector(selectors.getConfig);
  const service = useSelector(selectors.getService);
  
  const selectedCollectionId = useSelector(selectors.getSelectedCollectionId);
  const viewMode = useSelector(selectors.getViewMode);
  const searchMode = useSelector(selectors.getSearchMode);
  const facets = useSelector(selectors.getFacets);
  const simpleQuery = useSelector(selectors.getSimpleQuery);
  const advancedRows = useSelector(selectors.getAdvancedRows);
  const currentPage = useSelector(selectors.getCurrentPage);
  const pageSize = useSelector(selectors.getPageSize);
  const sortField = useSelector(selectors.getSortField);
  const sortOrder = useSelector(selectors.getSortOrder);
  const categories = useSelector(selectors.getCategories);
  const fields = useSelector(selectors.getFields);
  const defaultFields = useSelector(selectors.getDefaultFields);
  const opensearchUrl = useSelector(selectors.getOpensearchUrl);

  // Initialize on mount
  useEffect(() => {
    if (!jhsearchConfig) return;

    const initialize = async () => {
      // Store the config in Redux
      dispatch(loadConfigSuccess(jhsearchConfig));
      dispatch(loadServiceRequest());

      try {
        const { service: serviceConfig } = await initializeJHSearch(
          jhsearchConfig.iiifBaseUrl,
          jhsearchConfig.collectionId
        );

        dispatch(loadServiceSuccess(serviceConfig));

        // Build collection list from config
        const collections = [
          { id: jhsearchConfig.collectionId, label: jhsearchConfig.collectionId },
          ...(jhsearchConfig.childCollectionIds || []).map((id) => ({
            id,
            label: id,
          })),
        ];
        dispatch(loadCollectionsSuccess(collections));
      } catch (error) {
        dispatch(loadServiceFailure(error.message || 'Failed to load search configuration'));
      }
    };

    initialize();
  }, [dispatch, jhsearchConfig]);

  // Execute search when parameters change
  const executeSearch = useCallback(async () => {
    if (!isInitialized || !opensearchUrl || !selectedCollectionId) return;

    dispatch(searchRequest());

    try {
      let query;
      let searchFn = searchManifests;

      if (viewMode === 'browse') {
        // Browse mode - just filter by collection and facets
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
        // Simple search
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
        searchFn = searchAll; // Search both indexes
      } else {
        // Advanced search
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
      const results = parseSearchResults(response);
      dispatch(searchSuccess(results));
    } catch (error) {
      dispatch(searchFailure(error.message || 'Search failed'));
    }
  }, [
    isInitialized,
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
    dispatch,
  ]);

  // Trigger search when dependencies change
  useEffect(() => {
    executeSearch();
  }, [executeSearch]);

  // Handle retry
  const handleRetry = () => {
    window.location.reload();
  };

  // Loading state
  if (!isInitialized && !initError) {
    return (
      <Box
        sx={{
          display: 'flex',
          flexDirection: 'column',
          alignItems: 'center',
          justifyContent: 'center',
          height: '100%',
          gap: 2,
        }}
      >
        <CircularProgress />
        <Typography variant="body1" color="text.secondary">
          Loading search...
        </Typography>
      </Box>
    );
  }

  // Error state
  if (initError) {
    return <InitializationError error={initError} onRetry={handleRetry} />;
  }

  // Main view
  return (
    <Box
      sx={{
        display: 'flex',
        height: '100%',
        overflow: 'hidden',
      }}
      role="main"
      aria-label="Collection browser"
    >
      <JHSearchSidebar />
      <SearchResults onRetry={executeSearch} />
    </Box>
  );
}

/**
 * Connected JHSearchView with config from Mirador state.
 */
export function ConnectedJHSearchView(props) {
  // Get jhsearch config from Mirador's state
  const jhsearchConfig = useSelector((state) => state.config?.jhsearch);

  return <JHSearchView jhsearchConfig={jhsearchConfig} {...props} />;
}

export default ConnectedJHSearchView;
