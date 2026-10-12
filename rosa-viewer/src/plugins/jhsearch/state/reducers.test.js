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

    it('should handle SET_ADVANCED_ROWS', () => {
      const rows = [
        { field: 'drawing', value: 'pyramid', operator: 'AND' },
        { field: 'mark', value: 'dagger', operator: 'OR' },
      ];
      const state = jhsearchReducer(undefined, actions.setAdvancedRows(rows));

      expect(state.advancedSearch.rows).toEqual(rows);
      expect(state.pagination.page).toBe(0);
    });

    it('should keep one empty row when SET_ADVANCED_ROWS is given none', () => {
      const state = jhsearchReducer(undefined, actions.setAdvancedRows([]));

      expect(state.advancedSearch.rows).toEqual([{ field: '', value: '', operator: 'AND' }]);
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

  describe('restoring state', () => {
    const restored = {
      collectionId: 'rose',
      viewMode: 'search',
      searchMode: 'advanced',
      advancedRows: [{ field: 'description', value: 'sword', operator: 'AND' }],
      facets: { date: [1350] },
      sort: { field: 'page_num', order: 'asc' },
      page: 2,
      pageSize: 30,
    };

    it('should handle APPLY_SEARCH_STATE', () => {
      const state = jhsearchReducer(undefined, actions.applySearchState(restored));

      expect(state.ui).toMatchObject({
        selectedCollectionId: 'rose',
        viewMode: 'search',
        searchMode: 'advanced',
      });
      expect(state.advancedSearch.rows).toEqual(restored.advancedRows);
      expect(state.simpleSearch.query).toBe('');
      expect(state.facets).toEqual({ date: [1350] });
      expect(state.sort).toEqual({ field: 'page_num', order: 'asc' });
      expect(state.pagination).toMatchObject({ page: 2, pageSize: 30 });
    });

    it('should reset the queries and facets when restoring browsing', () => {
      let state = jhsearchReducer(undefined, actions.applySearchState(restored));
      state = jhsearchReducer(
        state,
        actions.applySearchState({
          collectionId: 'rose',
          viewMode: 'browse',
          sort: { field: 'label.keyword', order: 'asc' },
        })
      );

      expect(state.ui.viewMode).toBe('browse');
      expect(state.ui.searchMode).toBe('advanced');
      expect(state.advancedSearch.rows).toEqual([{ field: '', value: '', operator: 'AND' }]);
      expect(state.facets).toEqual({});
      expect(state.pagination).toMatchObject({ page: 0, pageSize: 30 });
    });

    it('should keep a restored collection and page size when the config loads', () => {
      let state = jhsearchReducer(undefined, actions.applySearchState(restored));
      state = jhsearchReducer(
        state,
        actions.loadConfigSuccess({ collectionId: 'dlmm', pageSize: 20 })
      );

      expect(state.ui.selectedCollectionId).toBe('rose');
      expect(state.pagination.pageSize).toBe(30);
    });

    it('should use the configured collection and page size otherwise', () => {
      const state = jhsearchReducer(
        undefined,
        actions.loadConfigSuccess({ collectionId: 'dlmm', pageSize: 40 })
      );

      expect(state.ui.selectedCollectionId).toBe('dlmm');
      expect(state.pagination.pageSize).toBe(40);
    });

    it('should match restored facet values as strings when toggling', () => {
      let state = jhsearchReducer(
        undefined,
        actions.applySearchState({ ...restored, facets: { date: ['1350'] } })
      );
      state = jhsearchReducer(state, actions.toggleFacet('date', '1350'));
      expect(state.facets).toEqual({});

      state = jhsearchReducer(
        state,
        actions.applySearchState({ ...restored, facets: { n: [10] } })
      );
      state = jhsearchReducer(state, actions.toggleFacet('n', '10'));
      expect(state.facets).toEqual({});
    });
  });
});
