/**
 * Tests for loading the selected collection's jhsearch.json.
 */

import React from 'react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, act, waitFor } from '@testing-library/react';
import { Provider } from 'react-redux';

import { JHSearchView } from './JHSearchView';
import { jhsearchReducer, initialState } from '../state/reducers';
import { selectCollection } from '../state/actions';
import * as selectors from '../state/selectors';
import { initializeJHSearch } from '../services/jhsearch';
import { runSearch } from '../services/search';

vi.mock('../services/jhsearch', () => ({ initializeJHSearch: vi.fn() }));
vi.mock('../services/search', () => ({ runSearch: vi.fn() }));
vi.mock('./JHSearchSidebar', () => ({ JHSearchSidebar: () => <div>sidebar</div> }));
vi.mock('./SearchResults', () => ({ SearchResults: () => <div>results</div> }));

const IIIF = 'http://localhost:3000/iiif';

const config = {
  iiifBaseUrl: IIIF,
  collectionId: 'dlmm',
  childCollectionIds: ['rose', 'pizan'],
};

/** A jhsearch.json as the service loader returns it, with one field named after the collection. */
function serviceFor(collectionId) {
  return {
    fields: [{ name: `${collectionId}_field`, label: collectionId }],
    categories: [],
    defaultFields: [`${collectionId}_field`],
    opensearchUrl: 'http://localhost:9200/_search',
  };
}

/** Minimal store that runs the real jhsearch reducer. */
function createStore() {
  let state = { jhsearch: initialState };
  const listeners = new Set();
  return {
    getState: () => state,
    dispatch: (action) => {
      state = { ...state, jhsearch: jhsearchReducer(state.jhsearch, action) };
      listeners.forEach((l) => l());
      return action;
    },
    subscribe: (listener) => {
      listeners.add(listener);
      return () => listeners.delete(listener);
    },
  };
}

const emptyResults = { items: [], total: 0, aggregations: {} };

describe('JHSearchView', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    initializeJHSearch.mockImplementation(async (iiifBaseUrl, collectionId) => ({
      collection: { id: collectionId, label: collectionId, url: `${iiifBaseUrl}/${collectionId}` },
      service: serviceFor(collectionId),
    }));
    runSearch.mockResolvedValue(emptyResults);
  });

  it("loads the site collection's service and searches with its fields", async () => {
    const store = createStore();
    render(
      <Provider store={store}>
        <JHSearchView jhsearchConfig={config} />
      </Provider>
    );

    await waitFor(() => expect(runSearch).toHaveBeenCalled());

    expect(initializeJHSearch).toHaveBeenCalledTimes(1);
    expect(initializeJHSearch).toHaveBeenCalledWith(IIIF, 'dlmm');
    expect(selectors.getServiceCollectionId(store.getState())).toBe('dlmm');
    expect(runSearch).toHaveBeenCalledWith(
      expect.objectContaining({ collectionId: 'dlmm', fields: serviceFor('dlmm').fields })
    );
  });

  it("loads the selected collection's service before searching it", async () => {
    const store = createStore();
    render(
      <Provider store={store}>
        <JHSearchView jhsearchConfig={config} />
      </Provider>
    );
    await waitFor(() => expect(runSearch).toHaveBeenCalled());
    runSearch.mockClear();

    act(() => {
      store.dispatch(selectCollection('rose'));
    });

    await waitFor(() => expect(runSearch).toHaveBeenCalled());

    expect(initializeJHSearch).toHaveBeenLastCalledWith(IIIF, 'rose');
    expect(selectors.getServiceCollectionId(store.getState())).toBe('rose');
    expect(selectors.getFields(store.getState())).toEqual(serviceFor('rose').fields);
    // Nothing was searched with the previous collection's fields
    expect(runSearch).toHaveBeenCalledTimes(1);
    expect(runSearch).toHaveBeenCalledWith(
      expect.objectContaining({ collectionId: 'rose', fields: serviceFor('rose').fields })
    );
  });

  it('does not reload the service when the same collection is selected again', async () => {
    const store = createStore();
    render(
      <Provider store={store}>
        <JHSearchView jhsearchConfig={config} />
      </Provider>
    );
    await waitFor(() => expect(runSearch).toHaveBeenCalled());

    act(() => {
      store.dispatch(selectCollection('dlmm'));
    });

    expect(initializeJHSearch).toHaveBeenCalledTimes(1);
  });

  it('keeps the service of the collection selected last when an earlier load is slow', async () => {
    const pending = {};
    initializeJHSearch.mockImplementation(
      (iiifBaseUrl, collectionId) =>
        new Promise((resolve) => {
          pending[collectionId] = () =>
            resolve({
              collection: { id: collectionId, label: collectionId, url: '' },
              service: serviceFor(collectionId),
            });
        })
    );

    const store = createStore();
    render(
      <Provider store={store}>
        <JHSearchView jhsearchConfig={config} />
      </Provider>
    );
    await waitFor(() => expect(pending.dlmm).toBeDefined());
    await act(async () => pending.dlmm());

    act(() => {
      store.dispatch(selectCollection('rose'));
    });
    await waitFor(() => expect(pending.rose).toBeDefined());
    act(() => {
      store.dispatch(selectCollection('pizan'));
    });
    await waitFor(() => expect(pending.pizan).toBeDefined());

    // pizan answers first, then the slow rose load arrives
    await act(async () => pending.pizan());
    await act(async () => pending.rose());

    expect(selectors.getServiceCollectionId(store.getState())).toBe('pizan');
    expect(selectors.getFields(store.getState())).toEqual(serviceFor('pizan').fields);
    expect(runSearch).not.toHaveBeenCalledWith(expect.objectContaining({ collectionId: 'rose' }));
  });
});
