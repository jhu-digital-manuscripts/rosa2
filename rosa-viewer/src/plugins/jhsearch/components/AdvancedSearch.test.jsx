/**
 * Tests for committing advanced search rows to the store.
 */

import React from 'react';
import { describe, it, expect } from 'vitest';
import { render, screen, fireEvent, act } from '@testing-library/react';
import { Provider } from 'react-redux';

import { AdvancedSearch } from './AdvancedSearch';
import { jhsearchReducer, initialState } from '../state/reducers';
import { mockReduxState } from '../../../test/mocks';

/** Minimal store that runs the real jhsearch reducer. */
function createStore(rows) {
  let state = {
    jhsearch: {
      ...initialState,
      service: { ...initialState.service, fields: mockReduxState.jhsearch.service.fields },
      advancedSearch: { rows },
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

const emptyRow = { field: '', value: '', operator: 'AND' };

function renderWithStore(store) {
  return render(
    <Provider store={store}>
      <AdvancedSearch />
    </Provider>
  );
}

function typeInRow(index, value) {
  fireEvent.change(screen.getAllByPlaceholderText('Search term...')[index], {
    target: { value },
  });
}

describe('AdvancedSearch', () => {
  it('commits an added row once, without duplicates', () => {
    const store = createStore([emptyRow]);
    renderWithStore(store);

    typeInRow(0, 'sword');
    fireEvent.click(screen.getByRole('button', { name: /add field/i }));
    typeInRow(1, 'pyramid');
    fireEvent.click(screen.getByRole('button', { name: /^search$/i }));

    const values = store.getState().jhsearch.advancedSearch.rows.map((r) => r.value);
    expect(values).toEqual(['sword', 'pyramid']);
    expect(screen.getAllByPlaceholderText('Search term...')).toHaveLength(2);
  });

  it('shows the same rows after switching tabs away and back', () => {
    const store = createStore([emptyRow]);
    const { unmount } = renderWithStore(store);

    typeInRow(0, 'sword');
    fireEvent.click(screen.getByRole('button', { name: /add field/i }));
    typeInRow(1, 'pyramid');
    fireEvent.click(screen.getByRole('button', { name: /^search$/i }));

    // Switching to Basic Search unmounts the form; switching back remounts it
    unmount();
    renderWithStore(store);

    const inputs = screen.getAllByPlaceholderText('Search term...');
    expect(inputs.map((i) => i.value)).toEqual(['sword', 'pyramid']);
  });

  it('commits removed rows', () => {
    const store = createStore([
      { field: '', value: 'sword', operator: 'AND' },
      { field: '', value: 'pyramid', operator: 'AND' },
    ]);
    renderWithStore(store);

    act(() => {
      fireEvent.click(screen.getAllByRole('button', { name: /remove search row/i })[0]);
    });
    fireEvent.click(screen.getByRole('button', { name: /^search$/i }));

    const values = store.getState().jhsearch.advancedSearch.rows.map((r) => r.value);
    expect(values).toEqual(['pyramid']);
  });
});
