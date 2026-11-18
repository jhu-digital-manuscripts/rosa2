import React from 'react';
import { describe, it, expect } from 'vitest';
import { act, fireEvent, render, screen } from '@testing-library/react';
import { Provider } from 'react-redux';
import {
  addWindow,
  ConnectedWorkspaceControlPanelButtons,
  createPluggableStore,
  PluginProvider,
  setWorkspaceAddVisibility,
  validatePlugin,
} from 'mirador';
import { mergeConfig } from '../../config';
import { jhsearchPlugin } from './index';

const MANIFEST = 'https://example.org/iiif/rose/Douce195/manifest.json';
const buttonPlugin = jhsearchPlugin.find((plugin) => plugin.name === 'JHSearchButton');
const plugins = [buttonPlugin];

function renderButtons() {
  // mergeConfig opens the workspace on the search view (isWorkspaceAddVisible)
  const store = createPluggableStore(
    mergeConfig({ jhsearch: { iiifBaseUrl: 'https://example.org/iiif', collectionId: 'rose' } }),
    plugins
  );
  render(
    <Provider store={store}>
      <PluginProvider plugins={plugins}>
        <ConnectedWorkspaceControlPanelButtons />
      </PluginProvider>
    </Provider>
  );
  return store;
}

const searchButton = () => screen.getByRole('button', { name: 'Search collections' });
const isHighlighted = (button) => getComputedStyle(button).backgroundColor === '#757575';
const visible = (store) => store.getState().workspace.isWorkspaceAddVisible;

/** Opens a book window and closes the search view, as opening a book does. */
function openBook(store) {
  act(() => {
    store.dispatch(addWindow({ manifestId: MANIFEST }));
    store.dispatch(setWorkspaceAddVisibility(false));
  });
}

describe('JHSearchButton plugin in Mirador', () => {
  it('is a valid Mirador plugin', () => {
    expect(validatePlugin(buttonPlugin)).toBe(true);
  });

  it('is highlighted and cannot close the search view while no book is open', () => {
    const store = renderButtons();
    expect(visible(store)).toBe(true);

    const style = getComputedStyle(searchButton());
    expect(style.cursor).toBe('default');
    expect(style.backgroundColor).toBe('#757575');
    expect(style.color).toBe('#fff');

    fireEvent.click(searchButton());
    expect(visible(store)).toBe(true);
    expect(isHighlighted(searchButton())).toBe(true);
  });

  it('is not highlighted while a book is shown', () => {
    const store = renderButtons();
    openBook(store);

    const style = getComputedStyle(searchButton());
    expect(style.cursor).toBe('pointer');
    expect(isHighlighted(searchButton())).toBe(false);
  });

  it('toggles the search view while a book is open', () => {
    const store = renderButtons();
    openBook(store);

    fireEvent.click(searchButton());
    expect(visible(store)).toBe(true);
    expect(isHighlighted(searchButton())).toBe(true);
    expect(getComputedStyle(searchButton()).cursor).toBe('pointer');

    fireEvent.click(searchButton());
    expect(visible(store)).toBe(false);
    expect(isHighlighted(searchButton())).toBe(false);
  });
});
