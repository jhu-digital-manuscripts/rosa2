/**
 * Redux selectors for JHSearch plugin.
 */

// ============================================================================
// Base Selectors
// ============================================================================

/**
 * Get the entire JHSearch state.
 */
export const getJHSearchState = (state) => state.jhsearch || {};

/**
 * Get the config state.
 */
export const getConfig = (state) => getJHSearchState(state).config || {};

/**
 * Get the service state.
 */
export const getService = (state) => getJHSearchState(state).service || {};

/**
 * Get the collections state.
 */
export const getCollections = (state) => getJHSearchState(state).collections || {};

/**
 * Get the UI state.
 */
export const getUI = (state) => getJHSearchState(state).ui || {};

/**
 * Get the facets state.
 */
export const getFacets = (state) => getJHSearchState(state).facets || {};

/**
 * Get the simple search state.
 */
export const getSimpleSearch = (state) => getJHSearchState(state).simpleSearch || {};

/**
 * Get the advanced search state.
 */
export const getAdvancedSearch = (state) => getJHSearchState(state).advancedSearch || {};

/**
 * Get the sort state.
 */
export const getSort = (state) => getJHSearchState(state).sort || {};

/**
 * Get the pagination state.
 */
export const getPagination = (state) => getJHSearchState(state).pagination || {};

/**
 * Get the results state.
 */
export const getResults = (state) => getJHSearchState(state).results || {};

// ============================================================================
// Derived Selectors
// ============================================================================

/**
 * Check if the plugin is fully initialized.
 */
export const isInitialized = (state) => {
  const config = getConfig(state);
  const service = getService(state);
  return config.loaded && service.loaded;
};

/**
 * Check if there's any loading in progress.
 */
export const isLoading = (state) => {
  const config = getConfig(state);
  const service = getService(state);
  const collections = getCollections(state);
  const results = getResults(state);
  return config.loading || service.loading || collections.loading || results.loading;
};

/**
 * Get any initialization error.
 */
export const getInitializationError = (state) => {
  const config = getConfig(state);
  const service = getService(state);
  return config.error || service.error;
};

/**
 * Get the currently selected collection ID.
 */
export const getSelectedCollectionId = (state) => getUI(state).selectedCollectionId;

/**
 * Get the current view mode ('browse' or 'search').
 */
export const getViewMode = (state) => getUI(state).viewMode;

/**
 * Get the current search mode ('simple' or 'advanced').
 */
export const getSearchMode = (state) => getUI(state).searchMode;

/**
 * Check if facets are enabled.
 */
export const areFacetsEnabled = (state) => getConfig(state).enableFacets !== false;

/**
 * Get the available categories for faceting.
 */
export const getCategories = (state) => getService(state).categories || [];

/**
 * Get the available search fields.
 */
export const getFields = (state) => getService(state).fields || [];

/**
 * Get the default search fields.
 */
export const getDefaultFields = (state) => getService(state).defaultFields || [];

/**
 * Get the Opensearch URL.
 */
export const getOpensearchUrl = (state) => getService(state).opensearchUrl;

/**
 * Get the IIIF base URL.
 */
export const getIiifBaseUrl = (state) => getConfig(state).iiifBaseUrl;

/**
 * Get the simple search query.
 */
export const getSimpleQuery = (state) => getSimpleSearch(state).query || '';

/**
 * Get the advanced search rows.
 */
export const getAdvancedRows = (state) => getAdvancedSearch(state).rows || [];

/**
 * Check if there's an active search query.
 */
export const hasActiveSearch = (state) => {
  const viewMode = getViewMode(state);
  if (viewMode !== 'search') return false;

  const searchMode = getSearchMode(state);
  if (searchMode === 'simple') {
    return getSimpleQuery(state).trim().length > 0;
  } else {
    const rows = getAdvancedRows(state);
    return rows.some((row) => row.value && row.value.trim().length > 0);
  }
};

/**
 * Check if any facets are selected.
 */
export const hasSelectedFacets = (state) => {
  const facets = getFacets(state);
  return Object.keys(facets).length > 0;
};

/**
 * Get the current page number.
 */
export const getCurrentPage = (state) => getPagination(state).page || 0;

/**
 * Get the current page size.
 */
export const getPageSize = (state) => getPagination(state).pageSize || 20;

/**
 * Get the total number of results.
 */
export const getTotalResults = (state) => getPagination(state).total || 0;

/**
 * Get the total number of pages.
 */
export const getTotalPages = (state) => {
  const total = getTotalResults(state);
  const pageSize = getPageSize(state);
  return Math.ceil(total / pageSize);
};

/**
 * Get the search results items.
 */
export const getResultItems = (state) => getResults(state).items || [];

/**
 * Get the aggregations from search results.
 */
export const getAggregations = (state) => getResults(state).aggregations || {};

/**
 * Get the search results error.
 */
export const getResultsError = (state) => getResults(state).error;

/**
 * Check if results are loading.
 */
export const isResultsLoading = (state) => getResults(state).loading;

/**
 * Get thumbnail configuration.
 */
export const getThumbnailConfig = (state) => {
  const config = getConfig(state);
  return {
    imageBaseUrl: config.imageBaseUrl,
    thumbnailTemplate: config.thumbnailTemplate,
    thumbnailWidth: config.thumbnailWidth,
    logoBaseUrl: config.logoBaseUrl,
  };
};

/**
 * Get collection items.
 */
export const getCollectionItems = (state) => getCollections(state).items || [];

/**
 * Get the current sort field.
 */
export const getSortField = (state) => getSort(state).field || '_score';

/**
 * Get the current sort order.
 */
export const getSortOrder = (state) => getSort(state).order || 'desc';

/**
 * Get page size options.
 */
export const getPageSizeOptions = (state) => getConfig(state).pageSizeOptions || [20, 30, 40, 50];
