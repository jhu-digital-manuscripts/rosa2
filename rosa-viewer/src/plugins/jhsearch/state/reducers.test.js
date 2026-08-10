/**
 * Tests for Redux reducers.
 */

import { describe, it, expect } from 'vitest';
import { jhsearchReducer, initialState } from './reducers';
import * as actions from './actions';

describe('jhsearchReducer', () => {
  describe('config actions', () => {
    it('should handle LOAD_CONFIG_REQUEST', () => {
      const action = actions.loadConfigRequest();
      const state = jhsearchReducer(undefined, action);

      expect(state.config.loading).toBe(true);
      expect(state.config.error).toBeNull();
    });

    it('should handle LOAD_CONFIG_SUCCESS', () => {
      const config = { iiifBaseUrl: 'http://test.com', collectionId: 'test' };
      const action = actions.loadConfigSuccess(config);
      const state = jhsearchReducer(undefined, action);

      expect(state.config.iiifBaseUrl).toBe('http://test.com');
      expect(state.config.collectionId).toBe('test');
      expect(state.config.loaded).toBe(true);
      expect(state.config.loading).toBe(false);
    });

    it('should handle LOAD_CONFIG_FAILURE', () => {
      const action = actions.loadConfigFailure('Failed to load');
      const state = jhsearchReducer(undefined, action);

      expect(state.config.loading).toBe(false);
      expect(state.config.error).toBe('Failed to load');
    });
  });

  describe('UI actions', () => {
    it('should handle SET_VIEW_MODE', () => {
      const action = actions.setViewMode('search');
      const state = jhsearchReducer(undefined, action);

      expect(state.ui.viewMode).toBe('search');
    });

    it('should handle SET_SEARCH_MODE', () => {
      const action = actions.setSearchMode('advanced');
      const state = jhsearchReducer(undefined, action);

      expect(state.ui.searchMode).toBe('advanced');
    });

    it('should handle SELECT_COLLECTION', () => {
      const action = actions.selectCollection('test-collection');
      const state = jhsearchReducer(undefined, action);

      expect(state.ui.selectedCollectionId).toBe('test-collection');
    });
  });

  describe('search actions', () => {
    it('should handle SET_SIMPLE_QUERY', () => {
      const action = actions.setSimpleQuery('test query');
      const state = jhsearchReducer(undefined, action);

      expect(state.simpleSearch.query).toBe('test query');
    });

    it('should handle ADD_ADVANCED_ROW', () => {
      const action = actions.addAdvancedRow({ field: 'label', value: 'test', operator: 'AND' });
      const state = jhsearchReducer(undefined, action);

      expect(state.advancedSearch.rows).toHaveLength(2);
    });

    it('should handle REMOVE_ADVANCED_ROW', () => {
      const startState = {
        ...initialState,
        advancedSearch: {
          rows: [
            { field: 'a', value: 'a', operator: 'AND' },
            { field: 'b', value: 'b', operator: 'AND' },
          ],
        },
      };
      const action = actions.removeAdvancedRow(0);
      const state = jhsearchReducer(startState, action);

      expect(state.advancedSearch.rows).toHaveLength(1);
      expect(state.advancedSearch.rows[0].field).toBe('b');
    });

    it('should handle SET_ADVANCED_ROW', () => {
      const action = actions.setAdvancedRow(0, { field: 'label', value: 'test' });
      const state = jhsearchReducer(undefined, action);

      expect(state.advancedSearch.rows[0].field).toBe('label');
      expect(state.advancedSearch.rows[0].value).toBe('test');
    });
  });

  describe('facet actions', () => {
    it('should handle TOGGLE_FACET - add value', () => {
      const action = actions.toggleFacet('origin', 'France');
      const state = jhsearchReducer(undefined, action);

      expect(state.facets.origin).toContain('France');
    });

    it('should handle TOGGLE_FACET - remove value', () => {
      const startState = {
        ...initialState,
        facets: { origin: ['France', 'Italy'] },
      };
      const action = actions.toggleFacet('origin', 'France');
      const state = jhsearchReducer(startState, action);

      expect(state.facets.origin).not.toContain('France');
      expect(state.facets.origin).toContain('Italy');
    });

    it('should handle CLEAR_FACETS', () => {
      const startState = {
        ...initialState,
        facets: { origin: ['France', 'Italy'] },
      };
      const action = actions.clearFacets();
      const state = jhsearchReducer(startState, action);

      expect(state.facets).toEqual({});
    });
  });

  describe('pagination actions', () => {
    it('should handle SET_PAGE', () => {
      const action = actions.setPage(5);
      const state = jhsearchReducer(undefined, action);

      expect(state.pagination.page).toBe(5);
    });

    it('should handle SET_PAGE_SIZE and reset page', () => {
      const startState = {
        ...initialState,
        pagination: { ...initialState.pagination, page: 3 },
      };
      const action = actions.setPageSize(50);
      const state = jhsearchReducer(startState, action);

      expect(state.pagination.pageSize).toBe(50);
      expect(state.pagination.page).toBe(0);
    });
  });

  describe('results actions', () => {
    it('should handle SEARCH_REQUEST', () => {
      const action = actions.searchRequest();
      const state = jhsearchReducer(undefined, action);

      expect(state.results.loading).toBe(true);
      expect(state.results.error).toBeNull();
    });

    it('should handle SEARCH_SUCCESS', () => {
      const results = {
        items: [{ id: '1' }],
        total: 1,
        aggregations: { origin: [] },
      };
      const action = actions.searchSuccess(results);
      const state = jhsearchReducer(undefined, action);

      expect(state.results.loading).toBe(false);
      expect(state.results.items).toEqual([{ id: '1' }]);
      expect(state.pagination.total).toBe(1);
    });

    it('should handle SEARCH_FAILURE', () => {
      const action = actions.searchFailure('Search failed');
      const state = jhsearchReducer(undefined, action);

      expect(state.results.loading).toBe(false);
      expect(state.results.error).toBe('Search failed');
    });
  });
});
