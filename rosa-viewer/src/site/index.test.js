import { describe, it, expect, vi } from 'vitest';

// The site entry imports src/index.js, which imports Mirador and the plugin.
// Neither is exercised here, so stub them out.
vi.mock('../index', () => ({ createViewer: vi.fn(() => ({ store: {} })) }));

import { createSiteViewer } from './index';
import { createViewer } from '../index';

const site = {
  id: 'aor',
  title: 'The Archaeology of Reading',
  jhsearch: { collectionId: 'aor', childCollectionIds: [], enableFacets: false },
};

describe('createSiteViewer', () => {
  it('resolves the config from the site and environment and creates a viewer', () => {
    const env = { iiifBaseUrl: 'http://localhost:3000/iiif/' };
    const viewer = createSiteViewer(site, env);

    expect(viewer).toEqual({ store: {} });
    expect(createViewer).toHaveBeenCalledWith(
      expect.objectContaining({
        id: 'rosa-viewer',
        jhsearch: expect.objectContaining({
          iiifBaseUrl: 'http://localhost:3000/iiif',
          collectionId: 'aor',
          enableFacets: false,
        }),
      })
    );
    expect(document.title).toBe('The Archaeology of Reading');
  });

  it('reads the environment from window.rosaSiteEnv by default', () => {
    const factory = vi.fn(() => 'viewer');
    window.rosaSiteEnv = { iiifBaseUrl: 'https://stage.example.org/iiif' };
    try {
      createSiteViewer(site, undefined, { viewerFactory: factory });
    } finally {
      delete window.rosaSiteEnv;
    }
    expect(factory).toHaveBeenCalledWith(
      expect.objectContaining({
        jhsearch: expect.objectContaining({ iiifBaseUrl: 'https://stage.example.org/iiif' }),
      })
    );
  });

  it('logs an error when iiifBaseUrl is missing but still creates the viewer', () => {
    const error = vi.spyOn(console, 'error').mockImplementation(() => {});
    const factory = vi.fn(() => 'viewer');
    createSiteViewer(site, {}, { viewerFactory: factory });
    expect(error).toHaveBeenCalledWith(expect.stringContaining('iiifBaseUrl is not set'));
    expect(factory).toHaveBeenCalled();
    error.mockRestore();
  });
});
