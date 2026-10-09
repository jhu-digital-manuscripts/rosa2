/**
 * Tests for what clicking a search result opens.
 */

import React from 'react';
import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { Provider } from 'react-redux';

import { SearchResults } from './SearchResults';
import { getBookClickView } from '../state/selectors';
import { mockReduxState } from '../../../test/mocks';

vi.mock('mirador', () => ({
  addWindow: (config) => ({ type: 'mirador/ADD_WINDOW', config }),
}));

const IIIF = 'http://localhost:3000/iiif';

const bookResult = {
  id: 'rose.Douce195',
  index: 'manifest',
  source: {
    id: 'rose.Douce195',
    label: 'Douce 195',
    num_pages: 300,
    thumbnail: [
      { iiif_image_id: 'rose%2FDouce195%2FDouce195.001r', page_num: 4 },
      { iiif_image_id: 'rose%2FDouce195%2FDouce195.001v', page_num: 5 },
    ],
  },
};

const pageResult = {
  id: 'rose.Douce195.010r',
  index: 'canvas',
  source: {
    id: 'rose.Douce195.010r',
    label: '10r',
    manifest_id: 'rose.Douce195',
    page_num: 22,
  },
};

function renderResults(configOverrides = {}) {
  const state = {
    ...mockReduxState,
    jhsearch: {
      ...mockReduxState.jhsearch,
      config: { ...mockReduxState.jhsearch.config, ...configOverrides },
      pagination: { ...mockReduxState.jhsearch.pagination, total: 2 },
      results: { ...mockReduxState.jhsearch.results, items: [bookResult, pageResult] },
    },
  };
  const store = { getState: () => state, dispatch: vi.fn(), subscribe: () => () => {} };
  render(
    <Provider store={store}>
      <SearchResults />
    </Provider>
  );
  return store;
}

function addWindowConfigs(store) {
  return store.dispatch.mock.calls
    .map(([action]) => action)
    .filter((action) => action.type === 'mirador/ADD_WINDOW')
    .map((action) => action.config);
}

describe('SearchResults clicks', () => {
  it.each(['single', 'book', 'gallery'])(
    'opens a clicked book row in the configured %s view at the start',
    (view) => {
      const store = renderResults({ bookClickView: view });
      fireEvent.click(screen.getByText('Douce 195'));

      expect(addWindowConfigs(store)).toEqual([
        { manifestId: `${IIIF}/rose/Douce195/manifest.json`, view },
      ]);
      expect(store.dispatch).toHaveBeenCalledWith({
        type: 'mirador/SET_WORKSPACE_ADD_VISIBILITY',
        isWorkspaceAddVisible: false,
      });
    }
  );

  it('opens a book row in book view when not configured', () => {
    const store = renderResults({ bookClickView: undefined });
    fireEvent.click(screen.getByText('Douce 195'));

    expect(addWindowConfigs(store)[0].view).toBe('book');
  });

  it('opens a clicked thumbnail at its page in single image view', () => {
    const store = renderResults({ bookClickView: 'gallery' });
    fireEvent.click(screen.getByRole('button', { name: 'Open page 6' }));

    expect(addWindowConfigs(store)).toEqual([
      {
        manifestId: `${IIIF}/rose/Douce195/manifest.json`,
        canvasId: `${IIIF}/rose/Douce195/canvas/5`,
        view: 'single',
      },
    ]);
  });

  it('opens a clicked page row in single image view', () => {
    const store = renderResults({ bookClickView: 'gallery' });
    fireEvent.click(screen.getByText('10r'));

    expect(addWindowConfigs(store)).toEqual([
      {
        manifestId: `${IIIF}/rose/Douce195/manifest.json`,
        canvasId: `${IIIF}/rose/Douce195/canvas/22`,
        view: 'single',
      },
    ]);
  });
});

describe('getBookClickView', () => {
  const stateWith = (bookClickView) => ({ jhsearch: { config: { bookClickView } } });

  it('returns a known view', () => {
    expect(getBookClickView(stateWith('gallery'))).toBe('gallery');
    expect(getBookClickView(stateWith('single'))).toBe('single');
  });

  it('falls back to book for a missing or unknown view', () => {
    expect(getBookClickView(stateWith(undefined))).toBe('book');
    expect(getBookClickView(stateWith('scroll'))).toBe('book');
  });
});
