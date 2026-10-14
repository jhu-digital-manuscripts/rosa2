/**
 * Tests for the Name filter hooks.
 */

import React from 'react';
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { renderHook, act } from '@testing-library/react';
import { Provider } from 'react-redux';

vi.mock('../services/opensearch', () => ({
  searchManifests: vi.fn(),
}));

import { searchManifests } from '../services/opensearch';
import { useNameSuggestions, useNameLabelRestore, SUGGEST_DEBOUNCE_MS } from './useNameSuggestions';
import { jhsearchReducer, initialState } from '../state/reducers';

/** Minimal store that runs the real jhsearch reducer. */
function createStore(overrides = {}) {
  let state = {
    jhsearch: {
      ...initialState,
      service: { ...initialState.service, opensearchUrl: '/_search', categories: [] },
      ui: { ...initialState.ui, selectedCollectionId: 'dlmm' },
      ...overrides,
    },
  };
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

const wrapperFor = (store) =>
  function Wrapper({ children }) {
    return <Provider store={store}>{children}</Provider>;
  };

const hits = (...sources) => ({
  hits: { total: { value: sources.length }, hits: sources.map((s) => ({ _source: s })) },
});

const douce195 = { id: 'rose.Douce195', label: 'Douce 195', logo: 'rose.jpg' };
const douce332 = { id: 'rose.Douce332', label: 'Douce 332', logo: 'rose.jpg' };

describe('useNameSuggestions', () => {
  beforeEach(() => {
    vi.useFakeTimers();
    searchManifests.mockReset();
  });
  afterEach(() => {
    vi.useRealTimers();
  });

  it('sends nothing for fewer than two characters', async () => {
    const store = createStore();
    const { result, rerender } = renderHook(({ text }) => useNameSuggestions(text), {
      initialProps: { text: '' },
      wrapper: wrapperFor(store),
    });
    rerender({ text: 'd' });
    rerender({ text: ' d ' });
    await act(async () => {
      vi.advanceTimersByTime(SUGGEST_DEBOUNCE_MS * 4);
    });
    expect(searchManifests).not.toHaveBeenCalled();
    expect(result.current.options).toEqual([]);
    expect(result.current.loading).toBe(false);
  });

  it('debounces typing into one request with the suggest query', async () => {
    searchManifests.mockResolvedValue(hits(douce195, douce332));
    const store = createStore({ facets: { id: ['pizan.X'], material: ['parchment'] } });
    const { result, rerender } = renderHook(({ text }) => useNameSuggestions(text), {
      initialProps: { text: 'do' },
      wrapper: wrapperFor(store),
    });
    rerender({ text: 'dou' });
    rerender({ text: 'douc' });
    expect(result.current.loading).toBe(true);

    await act(async () => {
      vi.advanceTimersByTime(SUGGEST_DEBOUNCE_MS);
    });

    expect(searchManifests).toHaveBeenCalledTimes(1);
    const [url, query] = searchManifests.mock.calls[0];
    expect(url).toBe('/_search');
    expect(query.query.bool.must[0].multi_match.query).toBe('douc');
    expect(query.size).toBe(10);
    // Other facets narrow suggestions, the Name selection itself does not
    expect(query.query.bool.filter).toEqual([
      { terms: { collection_id: ['dlmm'] } },
      { terms: { material: ['parchment'] } },
    ]);
    expect(result.current).toEqual({
      options: [douce195, douce332],
      loading: false,
      error: null,
    });
  });

  it('ignores a response for an older text', async () => {
    let resolveFirst;
    searchManifests
      .mockImplementationOnce(() => new Promise((resolve) => (resolveFirst = resolve)))
      .mockResolvedValueOnce(hits(douce332));
    const store = createStore();
    const { result, rerender } = renderHook(({ text }) => useNameSuggestions(text), {
      initialProps: { text: 'douce 1' },
      wrapper: wrapperFor(store),
    });
    await act(async () => {
      vi.advanceTimersByTime(SUGGEST_DEBOUNCE_MS);
    });
    rerender({ text: 'douce 3' });
    await act(async () => {
      vi.advanceTimersByTime(SUGGEST_DEBOUNCE_MS);
    });
    expect(result.current.options).toEqual([douce332]);

    await act(async () => {
      resolveFirst(hits(douce195));
    });
    expect(result.current.options).toEqual([douce332]);
  });

  it('reports an error', async () => {
    searchManifests.mockRejectedValue(new Error('boom'));
    const store = createStore();
    const { result } = renderHook(() => useNameSuggestions('douce'), {
      wrapper: wrapperFor(store),
    });
    await act(async () => {
      vi.advanceTimersByTime(SUGGEST_DEBOUNCE_MS);
    });
    expect(result.current).toEqual({ options: [], loading: false, error: 'boom' });
  });
});

describe('useNameLabelRestore', () => {
  beforeEach(() => {
    searchManifests.mockReset();
  });

  it('looks up only ids without a known label, in one request', async () => {
    searchManifests.mockResolvedValue(hits(douce195));
    const store = createStore({
      facets: { id: ['rose.Douce195', 'rose.Douce332', 'rose.Gone'] },
      nameLabels: { 'rose.Douce332': { label: 'Douce 332', logo: 'rose.jpg' } },
    });

    await act(async () => {
      renderHook(() => useNameLabelRestore(), { wrapper: wrapperFor(store) });
    });

    expect(searchManifests).toHaveBeenCalledTimes(1);
    const [, query] = searchManifests.mock.calls[0];
    expect(query.query.bool.filter).toEqual([{ terms: { id: ['rose.Douce195', 'rose.Gone'] } }]);
    expect(store.getState().jhsearch.nameLabels).toEqual({
      'rose.Douce195': { label: 'Douce 195', logo: 'rose.jpg' },
      'rose.Douce332': { label: 'Douce 332', logo: 'rose.jpg' },
      // Not found: remembered as its id so it is not requested again
      'rose.Gone': { label: 'rose.Gone', logo: null },
    });
  });

  it('sends nothing when every label is known', async () => {
    const store = createStore({
      facets: { id: ['rose.Douce195'] },
      nameLabels: { 'rose.Douce195': { label: 'Douce 195' } },
    });
    await act(async () => {
      renderHook(() => useNameLabelRestore(), { wrapper: wrapperFor(store) });
    });
    expect(searchManifests).not.toHaveBeenCalled();
  });
});
