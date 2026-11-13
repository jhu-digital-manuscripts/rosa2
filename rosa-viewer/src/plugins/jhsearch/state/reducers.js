/**
 * Redux reducers for JHSearch plugin.
 */

import * as actions from './actions';
import { rowHasQuery, pageCount } from '../utils/queryBuilder';
import { resolveSort } from '../utils/sorts';
import { defaultJHSearchConfig } from '../../../config';

/** A blank advanced search row; the form always has at least one. */
export const emptyAdvancedRow = () => ({ field: '', value: '', operator: 'AND' });

// ============================================================================
// Initial State
// ============================================================================

export const initialState = {
  // Plugin configuration (from Mirador config)
  config: {
    loaded: false,
    iiifBaseUrl: '',
    collectionId: '',
    childCollectionIds: [],
    imageBaseUrl: '',
    ...defaultJHSearchConfig,
  },

  // JHSearch service configuration (from the selected collection's jhsearch.json)
  service: {
    loaded: false,
    loading: false,
    error: null,
    // Collection whose jhsearch.json this is; reloaded when another is selected
    collectionId: null,
    fields: [],
    categories: [],
    defaultFields: [],
    opensearchUrl: '',
  },

  // Collections offered in the selector, built from the plugin config
  collections: {
    items: [],
  },

  // Current UI state
  ui: {
    selectedCollectionId: null,
    viewMode: 'browse', // 'browse' | 'search'
    searchMode: 'simple', // 'simple' | 'advanced'
  },

  // Facet selections
  facets: {},

  // Label and logo of books picked in the Name filter, by manifest id. Selections
  // themselves are in facets.id; this is only a display cache, so it is never cleared.
  nameLabels: {},

  // Simple search
  simpleSearch: {
    query: '',
  },

  // Advanced search
  advancedSearch: {
    rows: [emptyAdvancedRow()],
  },

  // Sort order
  sort: resolveSort('browse'),

  // Pagination
  pagination: {
    page: 0,
    pageSize: defaultJHSearchConfig.pageSize,
    // Set once the page size is chosen by the user or restored from the URL, so
    // the configured default no longer overrides it
    pageSizeSelected: false,
    total: 0,
  },

  // Search results
  results: {
    loading: false,
    error: null,
    items: [],
    aggregations: {},
    // Set when Opensearch answered 200 but not every shard contributed. See
    // services/opensearch.js describePartialResults.
    partial: null,
  },
};

// ============================================================================
// Reducer
// ============================================================================

/**
 * Drop advanced search rows for fields the loaded service does not have.
 *
 * Each collection has its own fields, so rows can name a field the collection lacks:
 * rose's Character after switching to dlmm, or a field from an old bookmark. The
 * query builder skips them, so keeping them left a row that searched for nothing. An
 * advanced search left with nothing to search for becomes a browse, as when the form
 * is submitted empty.
 */
function dropUnknownFieldRows(state) {
  const fieldNames = new Set((state.service.fields || []).map((f) => f.name));
  const rows = state.advancedSearch.rows;
  const keptRows =
    fieldNames.size === 0 ? rows : rows.filter((r) => !r.field || fieldNames.has(r.field));
  if (keptRows.length === rows.length) {
    return state;
  }

  const next = {
    ...state,
    advancedSearch: {
      rows: keptRows.length > 0 ? keptRows : [emptyAdvancedRow()],
    },
  };

  const searchLeft = keptRows.some((r) => r.field && rowHasQuery(r));
  if (state.ui.searchMode === 'advanced' && state.ui.viewMode === 'search' && !searchLeft) {
    return jhsearchReducer(next, actions.setViewMode('browse'));
  }
  return next;
}

export function jhsearchReducer(state = initialState, action) {
  switch (action.type) {
    // Configuration
    case actions.LOAD_CONFIG_SUCCESS:
      return {
        ...state,
        config: {
          ...state.config,
          ...action.payload,
          loaded: true,
        },
        // Keep a collection and page size that were already chosen, e.g. restored
        // from the URL before the search view was first shown, or chosen before it
        // was hidden and shown again.
        ui: {
          ...state.ui,
          selectedCollectionId: state.ui.selectedCollectionId || action.payload.collectionId,
        },
        pagination: {
          ...state.pagination,
          pageSize: state.pagination.pageSizeSelected
            ? state.pagination.pageSize
            : action.payload.pageSize || state.pagination.pageSize,
        },
      };

    // Service
    case actions.LOAD_SERVICE_REQUEST:
      return {
        ...state,
        service: {
          ...state.service,
          loading: true,
          error: null,
        },
      };

    case actions.LOAD_SERVICE_SUCCESS: {
      const service = {
        ...state.service,
        ...action.payload,
        loaded: true,
        loading: false,
        error: null,
      };

      return dropUnknownFieldRows({ ...state, service });
    }

    case actions.LOAD_SERVICE_FAILURE:
      return {
        ...state,
        service: {
          ...state.service,
          loading: false,
          error: action.payload,
        },
      };

    // Collections
    case actions.LOAD_COLLECTIONS_SUCCESS:
      return {
        ...state,
        collections: {
          ...state.collections,
          items: action.payload,
        },
      };

    // UI State
    case actions.SELECT_COLLECTION:
      return {
        ...state,
        ui: {
          ...state.ui,
          selectedCollectionId: action.payload,
        },
        pagination: {
          ...state.pagination,
          page: 0, // Reset to first page when collection changes
        },
      };

    case actions.SET_VIEW_MODE:
      return {
        ...state,
        ui: {
          ...state.ui,
          viewMode: action.payload,
        },
        // Each mode has its own sorts; start from the new mode's default
        sort: resolveSort(action.payload),
        pagination: {
          ...state.pagination,
          page: 0,
        },
      };

    case actions.SET_SEARCH_MODE:
      return {
        ...state,
        ui: {
          ...state.ui,
          searchMode: action.payload,
        },
      };

    case actions.APPLY_SEARCH_STATE: {
      const p = action.payload;
      const next = {
        ...state,
        ui: {
          ...state.ui,
          selectedCollectionId: p.collectionId || state.ui.selectedCollectionId,
          viewMode: p.viewMode,
          searchMode: p.searchMode || state.ui.searchMode,
        },
        facets: p.facets || {},
        simpleSearch: {
          query: p.simpleQuery || '',
        },
        advancedSearch: {
          rows: p.advancedRows && p.advancedRows.length > 0 ? p.advancedRows : [emptyAdvancedRow()],
        },
        sort: p.sort || state.sort,
        pagination: {
          ...state.pagination,
          page: p.page || 0,
          ...(p.pageSize ? { pageSize: p.pageSize, pageSizeSelected: true } : {}),
        },
      };
      // Before the collection's service has loaded its fields are unknown; the rows
      // are checked when it arrives (LOAD_SERVICE_SUCCESS)
      const serviceLoaded =
        next.service.loaded && next.service.collectionId === next.ui.selectedCollectionId;
      return serviceLoaded ? dropUnknownFieldRows(next) : next;
    }

    // Facets
    case actions.TOGGLE_FACET: {
      const { category, value } = action.payload;
      const currentValues = state.facets[category] || [];
      // Values restored from a URL may be strings where bucket keys are numbers
      const valueIndex = currentValues.findIndex((v) => String(v) === String(value));

      let newValues;
      if (valueIndex === -1) {
        newValues = [...currentValues, value];
      } else {
        newValues = currentValues.filter((_, i) => i !== valueIndex);
      }

      const newFacets = { ...state.facets };
      if (newValues.length > 0) {
        newFacets[category] = newValues;
      } else {
        delete newFacets[category];
      }

      return {
        ...state,
        facets: newFacets,
        pagination: {
          ...state.pagination,
          page: 0, // Reset to first page when facets change
        },
      };
    }

    case actions.SET_NAME_LABELS:
      return {
        ...state,
        nameLabels: {
          ...state.nameLabels,
          ...action.payload,
        },
      };

    // Search
    case actions.SET_SIMPLE_QUERY:
      return {
        ...state,
        simpleSearch: {
          ...state.simpleSearch,
          query: action.payload,
        },
        pagination: {
          ...state.pagination,
          page: 0,
        },
      };

    case actions.SET_ADVANCED_ROWS:
      return {
        ...state,
        advancedSearch: {
          ...state.advancedSearch,
          rows:
            action.payload && action.payload.length > 0
              ? action.payload.map((row) => ({ ...row }))
              : [emptyAdvancedRow()],
        },
        pagination: {
          ...state.pagination,
          page: 0,
        },
      };

    case actions.CLEAR_SEARCH:
      return {
        ...state,
        simpleSearch: {
          query: '',
        },
        advancedSearch: {
          rows: [emptyAdvancedRow()],
        },
        facets: {},
        ui: {
          ...state.ui,
          viewMode: 'browse',
        },
        pagination: {
          ...state.pagination,
          page: 0,
        },
      };

    // Sort & Pagination
    case actions.SET_SORT:
      return {
        ...state,
        sort: {
          field: action.payload.field,
          order: action.payload.order,
        },
        pagination: {
          ...state.pagination,
          page: 0,
        },
      };

    case actions.SET_PAGE:
      return {
        ...state,
        pagination: {
          ...state.pagination,
          page: action.payload,
        },
      };

    case actions.SET_PAGE_SIZE:
      return {
        ...state,
        pagination: {
          ...state.pagination,
          pageSize: action.payload,
          pageSizeSelected: true,
          page: 0, // Reset to first page when page size changes
        },
      };

    // Results
    case actions.SEARCH_REQUEST:
      return {
        ...state,
        results: {
          ...state.results,
          loading: true,
          error: null,
          partial: null,
        },
      };

    case actions.SEARCH_SUCCESS: {
      // A page past the last one, e.g. from an offset in a URL, has no results to
      // show although the search found some. Move to the last page; the search runs
      // again for it. The history replaces the URL of the page that was asked for
      // (site/history/historySync.js, isPageCorrection).
      const { total } = action.payload;
      const { page, pageSize } = state.pagination;
      const pages = pageCount(total, pageSize);
      return {
        ...state,
        results: {
          ...state.results,
          loading: false,
          error: null,
          items: action.payload.items,
          aggregations: action.payload.aggregations,
          partial: action.payload.partial || null,
        },
        pagination: {
          ...state.pagination,
          total,
          page: pages > 0 && page >= pages ? pages - 1 : page,
        },
      };
    }

    case actions.SEARCH_FAILURE:
      return {
        ...state,
        results: {
          ...state.results,
          loading: false,
          error: action.payload,
          partial: null,
        },
      };

    default:
      return state;
  }
}

export default jhsearchReducer;
