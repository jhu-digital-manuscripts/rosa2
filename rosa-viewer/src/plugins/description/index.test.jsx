import { describe, it, expect, vi } from 'vitest';

vi.mock('mirador', async () => {
  const React = await import('react');
  return {
    PluginContext: React.createContext({}),
    ConnectedCompanionWindow: ({ children }) => children,
    getManifest: (state, { windowId }) => state.manifests[state.windows[windowId]?.manifestId],
    setCanvas: vi.fn(),
  };
});

import {
  withoutDescriptionButton,
  mapSideBarStateToProps,
  mapPanelStateToProps,
  rosaDescriptionPlugin,
  BookDescriptionIcon,
  BOOK_DESCRIPTION_PANEL,
} from './index';

const state = (json) => ({
  windows: { w1: { manifestId: 'm1' } },
  manifests: { m1: { json } },
});

describe('description plugin definitions', () => {
  it('adds a tab, filters it, and registers the panel', () => {
    const [button, filter, panel] = rosaDescriptionPlugin;
    expect(button).toMatchObject({ mode: 'add', target: 'WindowSideBarButtons' });
    expect(button.mapStateToProps).toBeUndefined(); // keeps the static value
    expect(BookDescriptionIcon.value).toBe(BOOK_DESCRIPTION_PANEL);
    expect(filter).toMatchObject({ mode: 'wrap', target: 'WindowSideBarButtons' });
    expect(panel.companionWindowKey).toBe(BOOK_DESCRIPTION_PANEL);
    expect(panel.mode).toBeUndefined();
  });

  it('maps whether the window manifest has a TEI description', () => {
    const tei = { id: 'd', format: 'application/tei+xml' };
    expect(mapSideBarStateToProps(state({ seeAlso: [tei] }), { windowId: 'w1' })).toEqual({
      hasDescription: true,
    });
    expect(mapSideBarStateToProps(state({}), { windowId: 'w1' })).toEqual({
      hasDescription: false,
    });
    expect(mapSideBarStateToProps(state(undefined), { windowId: 'w1' }).hasDescription).toBe(false);
    expect(
      mapPanelStateToProps(state({ seeAlso: [tei] }), { windowId: 'w1' }).manifest.seeAlso
    ).toEqual([tei]);
  });
});

describe('withoutDescriptionButton', () => {
  it('removes only the description tab', () => {
    const other = { name: 'Other' };
    const map = {
      WindowSideBarButtons: { add: [other, { name: 'RosaBookDescriptionButton' }], wrap: ['w'] },
      Window: { add: ['x'] },
    };
    const filtered = withoutDescriptionButton(map);
    expect(filtered.WindowSideBarButtons).toEqual({ add: [other], wrap: ['w'] });
    expect(filtered.Window).toBe(map.Window);
    expect(map.WindowSideBarButtons.add).toHaveLength(2);
  });

  it('returns the map unchanged without the tab', () => {
    const map = { WindowSideBarButtons: { add: [{ name: 'Other' }] } };
    expect(withoutDescriptionButton(map)).toBe(map);
    expect(withoutDescriptionButton({})).toEqual({});
  });
});
