/**
 * Redux reducers for JHSearch plugin.
 */

import * as actions from './actions';

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
    pageSize: 20,
    pageSizeOptions: [20, 30, 40, 50],
  },

  // JHSearch service configuration (from jhsearch.json)
  service: {
    loaded: false,
    loading: false,
    error: null,
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
    field: '_score',
    order: 'desc',
  },

  // Pagination
  pagination: {
    page: 0,
    pageSize: 20,
    total: 0,
  },

  // Search results
  results: {
    loading: false,
    error: null,
    items: [],
    aggregations: {},
  },
};

// ============================================================================
// Reducer
// ============================================================================

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
        ui: {
          ...state.ui,
          selectedCollectionId: action.payload.collectionId,
        },
        pagination: {
          ...state.pagination,
          pageSize: action.payload.pageSize || state.pagination.pageSize,
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

    case actions.LOAD_SERVICE_SUCCESS:
      return {
        ...state,
        service: {
          ...state.service,
          ...action.payload,
          loaded: true,
          loading: false,
          error: null,
        },
      };

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

    // Facets
    case actions.TOGGLE_FACET: {
      const { category, value } = action.payload;
      const currentValues = state.facets[category] || [];
      const valueIndex = currentValues.indexOf(value);

      let newValues;
      if (valueIndex === -1) {
        newValues = [...currentValues, value];
      } else {
        newValues = currentValues.filter((v) => v !== value);
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
        },
      };

    case actions.SEARCH_SUCCESS:
      return {
        ...state,
        results: {
          ...state.results,
          loading: false,
          error: null,
          items: action.payload.items,
          aggregations: action.payload.aggregations,
        },
        pagination: {
          ...state.pagination,
          total: action.payload.total,
        },
      };

    case actions.SEARCH_FAILURE:
      return {
        ...state,
        results: {
          ...state.results,
          loading: false,
          error: action.payload,
        },
      };

    default:
      return state;
  }
}

export default jhsearchReducer;
