/**
 * Tests for what clicking a search result opens, and the scroll position when
 * new results arrive.
 */

import React from 'react';
import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent, act } from '@testing-library/react';
import { Provider } from 'react-redux';

import { SearchResults } from './SearchResults';
import { getBookClickView } from '../state/selectors';
import { mockReduxState } from '../../../test/mocks';

vi.mock('mirador', () => ({
  addWindow: (config) => ({ type: 'mirador/ADD_WINDOW', config }),
  setWorkspaceAddVisibility: (isWorkspaceAddVisible) => ({
    type: 'mirador/SET_WORKSPACE_ADD_VISIBILITY',
    isWorkspaceAddVisible,
  }),
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
    manifest_label: 'MS. Douce 195',
    page_num: 22,
  },
};

function renderResults(configOverrides = {}, total = 2, resultOverrides = {}) {
  const state = {
    ...mockReduxState,
    jhsearch: {
      ...mockReduxState.jhsearch,
      config: { ...mockReduxState.jhsearch.config, ...configOverrides },
      pagination: { ...mockReduxState.jhsearch.pagination, total },
      results: {
        ...mockReduxState.jhsearch.results,
        items: [bookResult, pageResult],
        ...resultOverrides,
      },
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

describe('SearchResults loading', () => {
  it('shows progress over results that a running search will replace', () => {
    renderResults({}, 2, { loading: true });

    expect(screen.getByRole('progressbar', { name: 'Searching' })).toBeInTheDocument();
    expect(screen.getByText('MS. Douce 195: 10r')).toBeInTheDocument();
  });

  it('shows no progress once the results have arrived', () => {
    renderResults();

    expect(screen.queryByRole('progressbar')).toBeNull();
  });
});

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
    fireEvent.click(screen.getByText('MS. Douce 195: 10r'));

    expect(addWindowConfigs(store)).toEqual([
      {
        manifestId: `${IIIF}/rose/Douce195/manifest.json`,
        canvasId: `${IIIF}/rose/Douce195/canvas/22`,
        view: 'single',
      },
    ]);
  });
});

describe('SearchResults page rows', () => {
  it('titles a page as book label: page label', () => {
    renderResults();
    expect(screen.getByText('MS. Douce 195: 10r')).toBeInTheDocument();
  });

  it('falls back to the book ID when the index has no book label', () => {
    const source = { ...pageResult.source, manifest_label: undefined };
    const state = {
      ...mockReduxState,
      jhsearch: {
        ...mockReduxState.jhsearch,
        pagination: { ...mockReduxState.jhsearch.pagination, total: 1 },
        results: { ...mockReduxState.jhsearch.results, items: [{ ...pageResult, source }] },
      },
    };
    const store = { getState: () => state, dispatch: vi.fn(), subscribe: () => () => {} };
    render(
      <Provider store={store}>
        <SearchResults />
      </Provider>
    );
    expect(screen.getByText('Douce195: 10r')).toBeInTheDocument();
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

describe('SearchResults header', () => {
  it('says when only the first results can be viewed', () => {
    renderResults({}, 12345);
    expect(screen.getByText(/Only the first 10,000 results/)).toBeInTheDocument();
  });

  it('says nothing about the limit for smaller result sets', () => {
    renderResults({}, 10000);
    expect(screen.queryByText(/Only the first/)).toBeNull();
  });
});

describe('SearchResults scroll position', () => {
  /** Store whose results can be replaced, as a search or Back/Forward would. */
  function createResultsStore(items) {
    let state = {
      ...mockReduxState,
      jhsearch: {
        ...mockReduxState.jhsearch,
        pagination: { ...mockReduxState.jhsearch.pagination, total: 2 },
        results: { ...mockReduxState.jhsearch.results, items },
      },
    };
    const listeners = new Set();
    return {
      getState: () => state,
      dispatch: vi.fn(),
      subscribe: (listener) => {
        listeners.add(listener);
        return () => listeners.delete(listener);
      },
      updateResults(changes) {
        const results = { ...state.jhsearch.results, ...changes };
        state = { ...state, jhsearch: { ...state.jhsearch, results } };
        act(() => listeners.forEach((listener) => listener()));
      },
    };
  }

  /**
   * Render the results inside a stand-in for JHSearchView, the element that
   * scrolls in the narrow layout. Returns the view, the results area and the
   * results list (the area's third child, after the header and the progress bar).
   */
  function renderInView(store) {
    render(
      <Provider store={store}>
        <div data-testid="view">
          <SearchResults />
        </div>
      </Provider>
    );
    const view = screen.getByTestId('view');
    const area = view.firstElementChild;
    return { view, area, list: area.children[2] };
  }

  /** Place the view and the top of the results area, as layout would. */
  function placeTops(view, area, { viewTop, areaTop }) {
    vi.spyOn(view, 'getBoundingClientRect').mockReturnValue({ top: viewTop });
    vi.spyOn(area, 'getBoundingClientRect').mockReturnValue({ top: areaTop });
  }

  it('scrolls the list to the top when new results arrive', () => {
    const store = createResultsStore([bookResult]);
    const { list } = renderInView(store);
    list.scrollTop = 400;
    expect(list.scrollTop).toBe(400);

    store.updateResults({ items: [pageResult] });

    expect(list.scrollTop).toBe(0);
  });

  it('keeps the scroll position while the next page loads', () => {
    const store = createResultsStore([bookResult]);
    const { list } = renderInView(store);
    list.scrollTop = 400;

    store.updateResults({ loading: true });

    expect(list.scrollTop).toBe(400);
  });

  it('scrolls the view back to the results when their top has scrolled above it', () => {
    const store = createResultsStore([bookResult]);
    const { view, area } = renderInView(store);
    view.scrollTop = 1000;
    placeTops(view, area, { viewTop: 100, areaTop: -200 });

    store.updateResults({ items: [pageResult] });

    // The area's top was 300px above the view's, so the view scrolls up by 300
    expect(view.scrollTop).toBe(700);
  });

  it('leaves the view on the sidebar when the top of the results is in view', () => {
    const store = createResultsStore([bookResult]);
    const { view, area } = renderInView(store);
    view.scrollTop = 0;
    placeTops(view, area, { viewTop: 100, areaTop: 429 });

    // e.g. a sort or filter change made in the sidebar above the results
    store.updateResults({ items: [pageResult] });

    expect(view.scrollTop).toBe(0);
  });
});
