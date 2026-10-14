/**
 * Redux action types and action creators for JHSearch plugin.
 */

// Action type prefix
const PREFIX = 'jhsearch';

// ============================================================================
// Action Types
// ============================================================================

// Configuration
export const LOAD_CONFIG_REQUEST = `${PREFIX}/LOAD_CONFIG_REQUEST`;
export const LOAD_CONFIG_SUCCESS = `${PREFIX}/LOAD_CONFIG_SUCCESS`;
export const LOAD_CONFIG_FAILURE = `${PREFIX}/LOAD_CONFIG_FAILURE`;

// Service (jhsearch.json)
export const LOAD_SERVICE_REQUEST = `${PREFIX}/LOAD_SERVICE_REQUEST`;
export const LOAD_SERVICE_SUCCESS = `${PREFIX}/LOAD_SERVICE_SUCCESS`;
export const LOAD_SERVICE_FAILURE = `${PREFIX}/LOAD_SERVICE_FAILURE`;

// Collections
export const LOAD_COLLECTIONS_REQUEST = `${PREFIX}/LOAD_COLLECTIONS_REQUEST`;
export const LOAD_COLLECTIONS_SUCCESS = `${PREFIX}/LOAD_COLLECTIONS_SUCCESS`;
export const LOAD_COLLECTIONS_FAILURE = `${PREFIX}/LOAD_COLLECTIONS_FAILURE`;

// UI State
export const SELECT_COLLECTION = `${PREFIX}/SELECT_COLLECTION`;
export const SET_VIEW_MODE = `${PREFIX}/SET_VIEW_MODE`;
export const SET_SEARCH_MODE = `${PREFIX}/SET_SEARCH_MODE`;
export const APPLY_SEARCH_STATE = `${PREFIX}/APPLY_SEARCH_STATE`;

// Facets
export const TOGGLE_FACET = `${PREFIX}/TOGGLE_FACET`;
export const CLEAR_FACETS = `${PREFIX}/CLEAR_FACETS`;

// Name filter display cache
export const SET_NAME_LABELS = `${PREFIX}/SET_NAME_LABELS`;

// Search
export const SET_SIMPLE_QUERY = `${PREFIX}/SET_SIMPLE_QUERY`;
export const SET_ADVANCED_ROW = `${PREFIX}/SET_ADVANCED_ROW`;
export const SET_ADVANCED_ROWS = `${PREFIX}/SET_ADVANCED_ROWS`;
export const ADD_ADVANCED_ROW = `${PREFIX}/ADD_ADVANCED_ROW`;
export const REMOVE_ADVANCED_ROW = `${PREFIX}/REMOVE_ADVANCED_ROW`;
export const CLEAR_SEARCH = `${PREFIX}/CLEAR_SEARCH`;

// Sort & Pagination
export const SET_SORT = `${PREFIX}/SET_SORT`;
export const SET_PAGE = `${PREFIX}/SET_PAGE`;
export const SET_PAGE_SIZE = `${PREFIX}/SET_PAGE_SIZE`;

// Results
export const SEARCH_REQUEST = `${PREFIX}/SEARCH_REQUEST`;
export const SEARCH_SUCCESS = `${PREFIX}/SEARCH_SUCCESS`;
export const SEARCH_FAILURE = `${PREFIX}/SEARCH_FAILURE`;

// ============================================================================
// Action Creators
// ============================================================================

// Configuration
export const loadConfigRequest = () => ({
  type: LOAD_CONFIG_REQUEST,
});

export const loadConfigSuccess = (config) => ({
  type: LOAD_CONFIG_SUCCESS,
  payload: config,
});

export const loadConfigFailure = (error) => ({
  type: LOAD_CONFIG_FAILURE,
  payload: error,
  error: true,
});

// Service
export const loadServiceRequest = () => ({
  type: LOAD_SERVICE_REQUEST,
});

export const loadServiceSuccess = (service) => ({
  type: LOAD_SERVICE_SUCCESS,
  payload: service,
});

export const loadServiceFailure = (error) => ({
  type: LOAD_SERVICE_FAILURE,
  payload: error,
  error: true,
});

// Collections
export const loadCollectionsRequest = () => ({
  type: LOAD_COLLECTIONS_REQUEST,
});

export const loadCollectionsSuccess = (collections) => ({
  type: LOAD_COLLECTIONS_SUCCESS,
  payload: collections,
});

export const loadCollectionsFailure = (error) => ({
  type: LOAD_COLLECTIONS_FAILURE,
  payload: error,
  error: true,
});

// UI State
export const selectCollection = (collectionId) => ({
  type: SELECT_COLLECTION,
  payload: collectionId,
});

export const setViewMode = (mode) => ({
  type: SET_VIEW_MODE,
  payload: mode, // 'browse' | 'search'
});

export const setSearchMode = (mode) => ({
  type: SET_SEARCH_MODE,
  payload: mode, // 'simple' | 'advanced'
});

/**
 * Replace the browse/search state in one step, e.g. when restoring it from a URL.
 *
 * @param {Object} searchState
 * @param {string} searchState.collectionId
 * @param {string} searchState.viewMode - 'browse' | 'search'
 * @param {string} [searchState.searchMode] - 'simple' | 'advanced'; unchanged when omitted
 * @param {string} [searchState.simpleQuery]
 * @param {Object[]} [searchState.advancedRows]
 * @param {Object} [searchState.facets]
 * @param {{field: string, order: string}} searchState.sort
 * @param {number} [searchState.page]
 * @param {number} [searchState.pageSize]
 */
export const applySearchState = (searchState) => ({
  type: APPLY_SEARCH_STATE,
  payload: searchState,
});

// Facets
export const toggleFacet = (category, value) => ({
  type: TOGGLE_FACET,
  payload: { category, value },
});

export const clearFacets = () => ({
  type: CLEAR_FACETS,
});

/**
 * Remember how to show Name filter selections.
 * @param {Object<string, {label: string, logo?: string}>} labels - By manifest id
 */
export const setNameLabels = (labels) => ({
  type: SET_NAME_LABELS,
  payload: labels,
});

// Search
export const setSimpleQuery = (query) => ({
  type: SET_SIMPLE_QUERY,
  payload: query,
});

export const setAdvancedRow = (index, row) => ({
  type: SET_ADVANCED_ROW,
  payload: { index, row },
});

/**
 * Replace all advanced search rows in one step, e.g. when the form is submitted.
 *
 * @param {Object[]} rows - Rows with field, value and operator
 */
export const setAdvancedRows = (rows) => ({
  type: SET_ADVANCED_ROWS,
  payload: rows,
});

export const addAdvancedRow = (row) => ({
  type: ADD_ADVANCED_ROW,
  payload: row || { field: '', value: '', operator: 'AND' },
});

export const removeAdvancedRow = (index) => ({
  type: REMOVE_ADVANCED_ROW,
  payload: index,
});

export const clearSearch = () => ({
  type: CLEAR_SEARCH,
});

// Sort & Pagination
export const setSort = (field, order) => ({
  type: SET_SORT,
  payload: { field, order },
});

export const setPage = (page) => ({
  type: SET_PAGE,
  payload: page,
});

export const setPageSize = (pageSize) => ({
  type: SET_PAGE_SIZE,
  payload: pageSize,
});

// Results
export const searchRequest = () => ({
  type: SEARCH_REQUEST,
});

export const searchSuccess = (results) => ({
  type: SEARCH_SUCCESS,
  payload: results,
});

export const searchFailure = (error) => ({
  type: SEARCH_FAILURE,
  payload: error,
  error: true,
});
