/**
 * Tests for the Name filter and its place in CategoryFacets.
 */

import React from 'react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, act, waitFor } from '@testing-library/react';
import { Provider } from 'react-redux';

vi.mock('../services/opensearch', () => ({
  searchManifests: vi.fn(),
}));

import { searchManifests } from '../services/opensearch';
import { CategoryFacets } from './CategoryFacets';
import { jhsearchReducer, initialState } from '../state/reducers';

function createStore({ enableFacets = true, categories = [], facets = {}, nameLabels = {} } = {}) {
  let state = {
    jhsearch: {
      ...initialState,
      config: { ...initialState.config, enableFacets, logoBaseUrl: '/logo' },
      service: { ...initialState.service, opensearchUrl: '/_search', categories },
      ui: { ...initialState.ui, selectedCollectionId: 'dlmm' },
      facets,
      nameLabels,
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

function renderFacets(store) {
  return render(
    <Provider store={store}>
      <CategoryFacets />
    </Provider>
  );
}

const hits = (...sources) => ({ hits: { hits: sources.map((s) => ({ _source: s })) } });
const douce195 = { id: 'rose.Douce195', label: 'Douce 195', logo: 'rose.jpg' };
const harley = { id: 'pizan.Harley4431', label: 'Harley 4431', logo: 'pizan.jpg' };

/** Type into the Name input and let the debounced request finish. */
async function typeName(text) {
  const input = screen.getByRole('combobox', { name: 'Name' });
  fireEvent.focus(input);
  fireEvent.change(input, { target: { value: text } });
  await waitFor(() => expect(searchManifests).toHaveBeenCalled(), { timeout: 2000 });
  return input;
}

describe('NameFilter in CategoryFacets', () => {
  beforeEach(() => {
    searchManifests.mockReset();
  });

  it('renders Name even when the collection has no categories', () => {
    renderFacets(createStore({ categories: [] }));
    expect(screen.getByText('Filter By')).toBeInTheDocument();
    expect(screen.getByRole('combobox', { name: 'Name' })).toBeInTheDocument();
  });

  it('puts Name before the categories', () => {
    renderFacets(createStore({ categories: [{ name: 'origin', label: 'Origin' }] }));
    const name = screen.getByRole('combobox', { name: 'Name' });
    const origin = screen.getByText('Origin');
    expect(name.compareDocumentPosition(origin) & Node.DOCUMENT_POSITION_FOLLOWING).toBeTruthy();
  });

  it('renders nothing when facets are disabled', () => {
    const { container } = renderFacets(createStore({ enableFacets: false }));
    expect(container).toBeEmptyDOMElement();
  });

  it('shows suggestions with collection logos and selects by id', async () => {
    searchManifests.mockResolvedValue(hits(douce195, harley));
    const store = createStore();
    renderFacets(store);

    await typeName('ha');
    const option = await screen.findByRole('option', { name: 'Harley 4431' });
    expect(screen.getByRole('option', { name: 'Douce 195' })).toBeInTheDocument();
    const logo = option.querySelector('img');
    expect(logo).toHaveAttribute('src', '/logo/pizan.jpg');
    expect(logo).toHaveAttribute('alt', '');

    await act(async () => {
      fireEvent.click(option);
    });

    const { facets, nameLabels } = store.getState().jhsearch;
    expect(facets).toEqual({ id: ['pizan.Harley4431'] });
    expect(nameLabels['pizan.Harley4431']).toEqual({ label: 'Harley 4431', logo: 'pizan.jpg' });

    // A chip with its logo, and no duplicate "id:" chip in Active Filters
    const chip = screen.getByRole('button', { name: /Harley 4431/ });
    expect(chip.querySelector('img')).toHaveAttribute('src', '/logo/pizan.jpg');
    expect(screen.queryByText(/^id:/)).not.toBeInTheDocument();
  });

  it('removes a selection when its chip is deleted', async () => {
    const store = createStore({
      facets: { id: ['rose.Douce195', 'pizan.Harley4431'] },
      nameLabels: {
        'rose.Douce195': { label: 'Douce 195', logo: 'rose.jpg' },
        'pizan.Harley4431': { label: 'Harley 4431', logo: 'pizan.jpg' },
      },
    });
    renderFacets(store);

    const chip = screen.getByRole('button', { name: /Douce 195/ });
    await act(async () => {
      fireEvent.click(chip.querySelector('.MuiChip-deleteIcon'));
    });

    expect(store.getState().jhsearch.facets).toEqual({ id: ['pizan.Harley4431'] });
    expect(searchManifests).not.toHaveBeenCalled();
  });

  it('looks up labels for selections restored from a URL', async () => {
    searchManifests.mockResolvedValue(hits(douce195));
    const store = createStore({ facets: { id: ['rose.Douce195'] } });
    renderFacets(store);

    // Shown by id until the lookup answers
    expect(screen.getByRole('button', { name: /rose\.Douce195/ })).toBeInTheDocument();
    expect(await screen.findByRole('button', { name: /Douce 195/ })).toBeInTheDocument();
    expect(searchManifests).toHaveBeenCalledTimes(1);
  });
});
