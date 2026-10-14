/**
 * The plugin with the real Mirador plugin machinery: the description tab is
 * shown after the built-in tabs only for manifests with a TEI seeAlso.
 */

import React from 'react';
import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { Provider } from 'react-redux';
// Mirador's own dependencies, used to check the translated tab label
import i18next from 'i18next';
import { I18nextProvider } from 'react-i18next';
import {
  PluginProvider,
  WindowSideBarButtons,
  withPlugins,
  validatePlugin,
  getConfigFromPlugins,
  CompanionWindowRegistry,
} from 'mirador';
import { rosaDescriptionPlugin, BOOK_DESCRIPTION_PANEL } from './index';

// The translations Mirador merges from plugin config (see createPluggableStore)
const i18n = i18next.createInstance();
i18n.init({
  lng: 'en',
  resources: {
    en: { translation: getConfigFromPlugins(rosaDescriptionPlugin).translations.en },
  },
});

const Buttons = withPlugins('WindowSideBarButtons', WindowSideBarButtons);

const TEI_SEE_ALSO = {
  id: 'https://x/rose/Douce332/description.xml',
  type: 'Dataset',
  format: 'application/tei+xml',
  profile: 'http://www.tei-c.org/ns/1.0',
};

function storeFor(manifestJson) {
  const state = {
    windows: { w1: { id: 'w1', manifestId: 'm1' } },
    manifests: { m1: { id: 'm1', json: manifestJson } },
  };
  return { getState: () => state, dispatch: vi.fn(), subscribe: () => () => {} };
}

function renderButtons(manifestJson) {
  const addCompanionWindow = vi.fn();
  render(
    <I18nextProvider i18n={i18n}>
      <Provider store={storeFor(manifestJson)}>
        <PluginProvider plugins={rosaDescriptionPlugin}>
          <Buttons
            windowId="w1"
            addCompanionWindow={addCompanionWindow}
            panels={{ info: true, canvas: true, annotations: true }}
            hasAnyAnnotations
          />
        </PluginProvider>
      </Provider>
    </I18nextProvider>
  );
  return { addCompanionWindow };
}

describe('rosaDescriptionPlugin in Mirador', () => {
  it('is a valid set of Mirador plugins', () => {
    expect(rosaDescriptionPlugin.every(validatePlugin)).toBe(true);
  });

  it('adds the tab last for a manifest with a TEI description and opens the panel', async () => {
    const { addCompanionWindow } = renderButtons({ seeAlso: [TEI_SEE_ALSO] });
    await waitFor(() => expect(screen.getAllByRole('tab')).toHaveLength(4));
    const last = screen.getAllByRole('tab').at(-1);
    expect(last).toHaveAttribute('aria-label', 'Book description');
    expect(last.querySelector('[data-testid="CoffeeSharpIcon"]')).not.toBeNull();
    fireEvent.click(last);
    expect(addCompanionWindow).toHaveBeenCalledWith(BOOK_DESCRIPTION_PANEL);
  });

  it('provides the tab tooltip through Mirador config', () => {
    expect(getConfigFromPlugins(rosaDescriptionPlugin)).toEqual({
      translations: { en: { openCompanionWindow_bookDescription: 'Book description' } },
    });
  });

  it('has no tab for a manifest without a TEI description', async () => {
    renderButtons({ seeAlso: [{ id: 'x', format: 'text/html' }] });
    await waitFor(() => expect(screen.getAllByRole('tab')).toHaveLength(3));
    expect(screen.queryByTestId('CoffeeSharpIcon')).toBeNull();
  });

  it('registers the panel under the tab value', async () => {
    render(
      <Provider store={storeFor({})}>
        <PluginProvider plugins={rosaDescriptionPlugin}>
          <span />
        </PluginProvider>
      </Provider>
    );
    await waitFor(() =>
      expect(CompanionWindowRegistry[BOOK_DESCRIPTION_PANEL]).toEqual(expect.anything())
    );
  });
});
