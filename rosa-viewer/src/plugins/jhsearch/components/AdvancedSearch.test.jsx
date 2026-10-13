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
function createStore(rows, fields = mockReduxState.jhsearch.service.fields) {
  let state = {
    jhsearch: {
      ...initialState,
      service: { ...initialState.service, fields },
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
const descriptionRow = { field: 'description', value: '', operator: 'AND' };

// Field definitions out of label order, with one enumerated field that has
// sub-fields (mark), one that does not (method), and a free text field
const enumeratedFields = [
  { name: 'mark', label: 'Mark', has_subfields: true, values: [{ value: 'dash', label: 'Dash' }] },
  { name: 'method', label: 'Method', values: [{ value: 'pen', label: 'Pen' }] },
  { name: 'description', label: 'Description' },
];

function renderWithStore(store) {
  return render(
    <Provider store={store}>
      <AdvancedSearch />
    </Provider>
  );
}

function fieldSelects() {
  return screen.getAllByRole('combobox', { name: /field/i });
}

function chooseField(index, label) {
  fireEvent.mouseDown(fieldSelects()[index]);
  fireEvent.click(screen.getByRole('option', { name: label }));
}

function typeInRow(index, value) {
  fireEvent.change(screen.getAllByPlaceholderText('Search term...')[index], {
    target: { value },
  });
}

describe('AdvancedSearch', () => {
  it('commits an added row once, without duplicates', () => {
    const store = createStore([descriptionRow]);
    renderWithStore(store);

    typeInRow(0, 'sword');
    fireEvent.click(screen.getByRole('button', { name: /add field/i }));
    chooseField(1, 'Description');
    typeInRow(1, 'pyramid');
    fireEvent.click(screen.getByRole('button', { name: /^search$/i }));

    const values = store.getState().jhsearch.advancedSearch.rows.map((r) => r.value);
    expect(values).toEqual(['sword', 'pyramid']);
    expect(screen.getAllByPlaceholderText('Search term...')).toHaveLength(2);
  });

  it('shows the same rows after switching tabs away and back', () => {
    const store = createStore([descriptionRow]);
    const { unmount } = renderWithStore(store);

    typeInRow(0, 'sword');
    fireEvent.click(screen.getByRole('button', { name: /add field/i }));
    chooseField(1, 'Description');
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
      { field: 'description', value: 'sword', operator: 'AND' },
      { field: 'description', value: 'pyramid', operator: 'AND' },
    ]);
    renderWithStore(store);

    act(() => {
      fireEvent.click(screen.getAllByRole('button', { name: /remove search row/i })[0]);
    });
    fireEvent.click(screen.getByRole('button', { name: /^search$/i }));

    const values = store.getState().jhsearch.advancedSearch.rows.map((r) => r.value);
    expect(values).toEqual(['pyramid']);
  });

  it('shows no value input until a field is chosen', () => {
    renderWithStore(createStore([emptyRow]));

    expect(screen.queryByPlaceholderText('Search term...')).toBeNull();
    expect(screen.queryByRole('combobox', { name: /value/i })).toBeNull();

    chooseField(0, 'Description');
    expect(screen.getByPlaceholderText('Search term...')).toBeInTheDocument();
  });

  it('lists fields in alphabetical order of label', () => {
    renderWithStore(createStore([emptyRow], enumeratedFields));

    fireEvent.mouseDown(fieldSelects()[0]);
    const options = screen.getAllByRole('option').map((o) => o.textContent);
    expect(options).toEqual(['Description', 'Mark', 'Method']);
  });

  it('offers extra text only for enumerated fields with sub-fields', () => {
    renderWithStore(createStore([emptyRow], enumeratedFields));

    chooseField(0, 'Method');
    expect(screen.queryByLabelText(/additional .* search text/i)).toBeNull();

    chooseField(0, 'Mark');
    expect(screen.getByLabelText('Additional Mark search text')).toBeInTheDocument();
  });

  it('commits the extra text with the row', () => {
    const store = createStore(
      [{ field: 'mark', value: 'dash', operator: 'AND' }],
      enumeratedFields
    );
    renderWithStore(store);

    fireEvent.change(screen.getByLabelText('Additional Mark search text'), {
      target: { value: 'regnum' },
    });
    fireEvent.click(screen.getByRole('button', { name: /^search$/i }));

    expect(store.getState().jhsearch.advancedSearch.rows).toEqual([
      { field: 'mark', value: 'dash', text: 'regnum', operator: 'AND' },
    ]);
    expect(store.getState().jhsearch.ui.viewMode).toBe('search');
  });

  it('drops extra text and an unknown value when the field changes', () => {
    const store = createStore(
      [{ field: 'mark', value: 'dash', text: 'regnum', operator: 'AND' }],
      enumeratedFields
    );
    renderWithStore(store);

    chooseField(0, 'Method');
    fireEvent.click(screen.getByRole('button', { name: /^search$/i }));

    expect(store.getState().jhsearch.advancedSearch.rows).toEqual([
      { field: 'method', value: '', operator: 'AND' },
    ]);
  });
});
