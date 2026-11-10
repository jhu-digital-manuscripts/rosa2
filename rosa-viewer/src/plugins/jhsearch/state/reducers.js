/**
 * Redux reducers for JHSearch plugin.
 */

import * as actions from './actions';
import { rowHasQuery, MAX_RESULT_WINDOW } from '../utils/queryBuilder';

// ============================================================================
// Initial State
// ============================================================================

export const initialState = {
  // Plugin configuration (from Mirador config)
  config: {
    loaded: false,
    loading: false,
    error: null,
    iiifBaseUrl: '',
    collectionId: '',
    childCollectionIds: [],
    imageBaseUrl: '',
    thumbnailTemplate: '{imageBaseUrl}/{iiifImageId}/full/{width},/0/default.jpg',
    thumbnailWidth: 80,
    logoBaseUrl: '/logo',
    enableFacets: true,
    bookClickView: 'book',
    pageSize: 20,
    pageSizeOptions: [20, 30, 40, 50],
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

  // Collection hierarchy
  collections: {
    loaded: false,
    loading: false,
    error: null,
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
    rows: [{ field: '', value: '', operator: 'AND' }],
  },

  // Sort order
  sort: {
    field: 'label.keyword',
    order: 'asc',
  },

  // Pagination
  pagination: {
    page: 0,
    pageSize: 20,
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
      rows: keptRows.length > 0 ? keptRows : [{ field: '', value: '', operator: 'AND' }],
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
    case actions.LOAD_CONFIG_REQUEST:
      return {
        ...state,
        config: {
          ...state.config,
          loading: true,
          error: null,
        },
      };

    case actions.LOAD_CONFIG_SUCCESS:
      return {
        ...state,
        config: {
          ...state.config,
          ...action.payload,
          loaded: true,
          loading: false,
          error: null,
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

    case actions.LOAD_CONFIG_FAILURE:
      return {
        ...state,
        config: {
          ...state.config,
          loading: false,
          error: action.payload,
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
    case actions.LOAD_COLLECTIONS_REQUEST:
      return {
        ...state,
        collections: {
          ...state.collections,
          loading: true,
          error: null,
        },
      };

    case actions.LOAD_COLLECTIONS_SUCCESS:
      return {
        ...state,
        collections: {
          ...state.collections,
          items: action.payload,
          loaded: true,
          loading: false,
          error: null,
        },
      };

    case actions.LOAD_COLLECTIONS_FAILURE:
      return {
        ...state,
        collections: {
          ...state.collections,
          loading: false,
          error: action.payload,
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
        // Set default sort for the new mode:
        // - browse: Title (A-Z)
        // - search: Relevance
        sort:
          action.payload === 'browse'
            ? { field: 'label.keyword', order: 'asc' }
            : { field: '_score', order: 'desc' },
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
          rows:
            p.advancedRows && p.advancedRows.length > 0
              ? p.advancedRows
              : [{ field: '', value: '', operator: 'AND' }],
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

    case actions.CLEAR_FACETS:
      return {
        ...state,
        facets: {},
        pagination: {
          ...state.pagination,
          page: 0,
        },
      };

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
        ui: {
          ...state.ui,
          viewMode: action.payload ? 'search' : 'browse',
        },
        pagination: {
          ...state.pagination,
          page: 0,
        },
      };

    case actions.SET_ADVANCED_ROW: {
      const { index, row } = action.payload;
      const newRows = [...state.advancedSearch.rows];
      newRows[index] = { ...newRows[index], ...row };

      return {
        ...state,
        advancedSearch: {
          ...state.advancedSearch,
          rows: newRows,
        },
        pagination: {
          ...state.pagination,
          page: 0,
        },
      };
    }

    case actions.SET_ADVANCED_ROWS:
      return {
        ...state,
        advancedSearch: {
          ...state.advancedSearch,
          rows:
            action.payload && action.payload.length > 0
              ? action.payload.map((row) => ({ ...row }))
              : [{ field: '', value: '', operator: 'AND' }],
        },
        pagination: {
          ...state.pagination,
          page: 0,
        },
      };

    case actions.ADD_ADVANCED_ROW:
      return {
        ...state,
        advancedSearch: {
          ...state.advancedSearch,
          rows: [...state.advancedSearch.rows, action.payload],
        },
      };

    case actions.REMOVE_ADVANCED_ROW: {
      const newRows = state.advancedSearch.rows.filter((_, i) => i !== action.payload);
      // Ensure at least one row remains
      if (newRows.length === 0) {
        newRows.push({ field: '', value: '', operator: 'AND' });
      }

      return {
        ...state,
        advancedSearch: {
          ...state.advancedSearch,
          rows: newRows,
        },
        pagination: {
          ...state.pagination,
          page: 0,
        },
      };
    }

    case actions.CLEAR_SEARCH:
      return {
        ...state,
        simpleSearch: {
          query: '',
        },
        advancedSearch: {
          rows: [{ field: '', value: '', operator: 'AND' }],
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
      // again for it.
      const { total } = action.payload;
      const { page, pageSize } = state.pagination;
      const pageCount = Math.min(
        Math.ceil(total / pageSize),
        Math.floor(MAX_RESULT_WINDOW / pageSize)
      );
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
          page: pageCount > 0 && page >= pageCount ? pageCount - 1 : page,
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
