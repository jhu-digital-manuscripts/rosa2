import React from 'react';
import { describe, it, expect } from 'vitest';
import { render, screen, waitFor, act } from '@testing-library/react';
import { Provider } from 'react-redux';
// Mirador's own dependencies, so its button gets a translated label
import i18next from 'i18next';
import { I18nextProvider } from 'react-i18next';
import {
  addWindow,
  ConnectedWorkspaceAddButton,
  createPluggableStore,
  PluginProvider,
  removeWindow,
  setWorkspaceAddVisibility,
  validatePlugin,
} from 'mirador';
import { mergeConfig } from '../../config';
import { keepWorkspaceAddVisible, isWorkspaceEmpty, workspaceAddButtonPlugin } from './workspace';

const MANIFEST = 'https://example.org/iiif/rose/Douce195/manifest.json';
const plugins = [workspaceAddButtonPlugin];

const i18n = i18next.createInstance();
i18n.init({ lng: 'en', resources: { en: { translation: { closeAddResourceMenu: 'Close' } } } });

function createStore() {
  return createPluggableStore(
    mergeConfig({ jhsearch: { iiifBaseUrl: 'https://example.org/iiif', collectionId: 'rose' } }),
    plugins
  );
}

const visible = (store) => store.getState().workspace.isWorkspaceAddVisible;
const windowIds = (store) => Object.keys(store.getState().windows);

describe('keepWorkspaceAddVisible', () => {
  it('shows the add resource view when the last window closes', () => {
    const store = createStore();
    keepWorkspaceAddVisible(store);
    expect(visible(store)).toBe(true);

    store.dispatch(addWindow({ manifestId: MANIFEST }));
    store.dispatch(setWorkspaceAddVisibility(false));
    expect(visible(store)).toBe(false);

    store.dispatch(removeWindow(windowIds(store)[0]));
    expect(windowIds(store)).toHaveLength(0);
    expect(visible(store)).toBe(true);
  });

  it('keeps the view open when asked to close it with no windows', () => {
    const store = createStore();
    keepWorkspaceAddVisible(store);
    store.dispatch(setWorkspaceAddVisibility(false));
    expect(visible(store)).toBe(true);
  });

  it('lets the view close while a window is open', () => {
    const store = createStore();
    keepWorkspaceAddVisible(store);
    store.dispatch(addWindow({ manifestId: MANIFEST }));
    store.dispatch(setWorkspaceAddVisibility(false));
    expect(visible(store)).toBe(false);
    expect(isWorkspaceEmpty(store.getState())).toBe(false);
  });

  it('fixes a workspace that starts empty and hidden', () => {
    const store = createStore();
    store.dispatch(setWorkspaceAddVisibility(false));
    expect(isWorkspaceEmpty(store.getState())).toBe(true);
    keepWorkspaceAddVisible(store);
    expect(visible(store)).toBe(true);
  });
});

describe('WorkspaceAddButton plugin in Mirador', () => {
  function renderButton(store) {
    render(
      <I18nextProvider i18n={i18n}>
        <Provider store={store}>
          <PluginProvider plugins={plugins}>
            <ConnectedWorkspaceAddButton />
          </PluginProvider>
        </Provider>
      </I18nextProvider>
    );
  }

  it('is a valid Mirador plugin', () => {
    expect(validatePlugin(workspaceAddButtonPlugin)).toBe(true);
  });

  it('shows the close button only while a window is open', async () => {
    const store = createStore();
    renderButton(store);
    act(() => {
      store.dispatch(addWindow({ manifestId: MANIFEST }));
      store.dispatch(setWorkspaceAddVisibility(true));
    });
    expect(await screen.findByRole('button', { name: 'Close' })).toBeInTheDocument();

    act(() => {
      store.dispatch(removeWindow(windowIds(store)[0]));
    });
    await waitFor(() => expect(screen.queryByRole('button')).toBeNull());
  });
});
