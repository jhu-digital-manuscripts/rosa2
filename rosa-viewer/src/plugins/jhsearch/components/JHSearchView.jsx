/**
 * JHSearchView - Main container component for the JHSearch plugin.
 * This replaces Mirador's default WorkspaceAdd component.
 */

import React, { useEffect, useCallback, useRef } from 'react';
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
import { buildCollectionItems } from '../utils/collections';
import { runSearch } from '../services/search';

/**
 * JHSearchView is the main container for the search/browse interface.
 */
export function JHSearchView({ jhsearchConfig }) {
  const dispatch = useDispatch();

  const isInitialized = useSelector(selectors.isInitialized);
  const initError = useSelector(selectors.getInitializationError);

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
  const enableFacets = useSelector(selectors.areFacetsEnabled);
  const rootCollectionId = useSelector(selectors.getRootCollectionId);
  const serviceCollectionId = useSelector(selectors.getServiceCollectionId);

  // Store the plugin config and the collection list on mount
  useEffect(() => {
    if (!jhsearchConfig) return;
    dispatch(loadConfigSuccess(jhsearchConfig));
    dispatch(loadCollectionsSuccess(buildCollectionItems(jhsearchConfig)));
  }, [dispatch, jhsearchConfig]);

  // Each collection has its own jhsearch.json with its own fields and categories, so
  // the service is loaded for the selected collection and again whenever it changes.
  // Before the config is stored, the selection may still be empty; the site's
  // collection is the target then.
  const targetCollectionId = selectedCollectionId || jhsearchConfig?.collectionId;

  // Number of the latest service load, so a slow response for an earlier collection
  // does not replace the one selected since
  const serviceCounter = useRef(0);

  useEffect(() => {
    if (!jhsearchConfig || !targetCollectionId || serviceCollectionId === targetCollectionId) {
      return;
    }

    const loadNumber = ++serviceCounter.current;
    dispatch(loadServiceRequest());

    const load = async () => {
      try {
        const { service: serviceConfig } = await initializeJHSearch(
          jhsearchConfig.iiifBaseUrl,
          targetCollectionId
        );
        if (loadNumber !== serviceCounter.current) return;
        dispatch(loadServiceSuccess({ ...serviceConfig, collectionId: targetCollectionId }));
      } catch (error) {
        if (loadNumber !== serviceCounter.current) return;
        dispatch(loadServiceFailure(error.message || 'Failed to load search configuration'));
      }
    };

    load();
  }, [dispatch, jhsearchConfig, targetCollectionId, serviceCollectionId]);

  // Number of the latest search, so a slow response to an earlier search (e.g.
  // after quickly going back and forward) does not replace newer results
  const searchCounter = useRef(0);

  // Execute search when parameters change. Waits for the selected collection's
  // service, whose fields and categories the search is built from.
  const executeSearch = useCallback(async () => {
    if (!isInitialized || !opensearchUrl || !selectedCollectionId) return;
    if (serviceCollectionId !== selectedCollectionId) return;

    const searchNumber = ++searchCounter.current;
    dispatch(searchRequest());

    try {
      const results = await runSearch({
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
      if (searchNumber !== searchCounter.current) return;
      dispatch(searchSuccess(results));
    } catch (error) {
      if (searchNumber !== searchCounter.current) return;
      dispatch(searchFailure(error.message || 'Search failed'));
    }
  }, [
    isInitialized,
    opensearchUrl,
    enableFacets,
    rootCollectionId,
    selectedCollectionId,
    serviceCollectionId,
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

  // Main view. Positioned over Mirador's viewer area rather than in its flow: the
  // area is a flex item with min-width: auto, so in-flow content would set its
  // minimum width to that of the widest unbreakable result and stop it shrinking.
  // Below md the sidebar sits above the results and the whole view scrolls.
  return (
    <Box
      data-jhsearch-view
      sx={{
        position: 'absolute',
        inset: 0,
        display: 'flex',
        flexDirection: { xs: 'column', md: 'row' },
        overflowX: 'hidden',
        overflowY: { xs: 'auto', md: 'hidden' },
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
