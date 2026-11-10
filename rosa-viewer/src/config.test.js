import { describe, it, expect } from 'vitest';
import { defaultJHSearchConfig, defaultMiradorConfig, mergeConfig } from './config';

describe('mergeConfig', () => {
  it('fills in the Mirador and JHSearch defaults', () => {
    const config = mergeConfig({ jhsearch: { iiifBaseUrl: 'x', collectionId: 'rose' } });

    expect(config.window).toEqual(defaultMiradorConfig.window);
    expect(config.jhsearch).toEqual({
      ...defaultJHSearchConfig,
      iiifBaseUrl: 'x',
      collectionId: 'rose',
    });
  });

  it('keeps the other defaults of a settings object when one key is set', () => {
    const config = mergeConfig({
      window: { imageToolsOpen: true, sideBarOpen: true },
      galleryView: { height: 200 },
    });

    expect(config.window).toEqual({
      ...defaultMiradorConfig.window,
      imageToolsOpen: true,
      sideBarOpen: true,
    });
    expect(config.galleryView).toEqual({ height: 200, width: null });
    expect(config.workspace).toEqual(defaultMiradorConfig.workspace);
  });

  it('passes other settings through and lets plain values replace defaults', () => {
    const config = mergeConfig({ id: 'other', selectedTheme: 'light', theme: { palette: {} } });

    expect(config.id).toBe('other');
    expect(config.selectedTheme).toBe('light');
    expect(config.theme).toEqual({ palette: {} });
  });
});
