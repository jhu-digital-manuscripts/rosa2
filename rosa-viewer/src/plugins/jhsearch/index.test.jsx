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
const isHighlighted = (button) => getComputedStyle(button).cursor === 'default';
const visible = (store) => store.getState().workspace.isWorkspaceAddVisible;

describe('JHSearchButton plugin in Mirador', () => {
  it('is a valid Mirador plugin', () => {
    expect(validatePlugin(buttonPlugin)).toBe(true);
  });

  it('is highlighted while the search view is showing', () => {
    const store = renderButtons();
    expect(visible(store)).toBe(true);

    const style = getComputedStyle(searchButton());
    expect(style.cursor).toBe('default');
    expect(style.backgroundColor).toBe('#757575');
    expect(style.color).toBe('#fff');
  });

  it('is not highlighted while a book is shown', () => {
    const store = renderButtons();
    act(() => {
      store.dispatch(addWindow({ manifestId: MANIFEST }));
      store.dispatch(setWorkspaceAddVisibility(false));
    });

    const style = getComputedStyle(searchButton());
    expect(style.cursor).not.toBe('default');
    expect(style.backgroundColor).not.toBe('#757575');
  });

  it('shows the search view when clicked and becomes highlighted', () => {
    const store = renderButtons();
    act(() => {
      store.dispatch(addWindow({ manifestId: MANIFEST }));
      store.dispatch(setWorkspaceAddVisibility(false));
    });
    expect(isHighlighted(searchButton())).toBe(false);

    fireEvent.click(searchButton());
    expect(visible(store)).toBe(true);
    expect(isHighlighted(searchButton())).toBe(true);
  });
});
