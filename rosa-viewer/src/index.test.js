import { describe, it, expect, vi } from 'vitest';

const viewer = { store: { getState: () => ({}) } };
vi.mock('mirador', () => ({ default: { viewer: vi.fn(() => viewer) } }));
vi.mock('mirador-image-tools', () => ({ miradorImageToolsPlugin: [{ name: 'imageTools' }] }));
vi.mock('./plugins/jhsearch', () => ({ jhsearchPlugin: [{ name: 'jhsearch' }] }));
vi.mock('./plugins/annotations', () => ({
  rosaAnnotationsPlugin: [{ name: 'annotations' }],
  openAnnotationsByDefault: vi.fn(),
}));
vi.mock('./plugins/description', () => ({ rosaDescriptionPlugin: [{ name: 'description' }] }));

import Mirador from 'mirador';
import { openAnnotationsByDefault } from './plugins/annotations';
import { createViewer } from './index';

describe('createViewer', () => {
  it('registers the search, annotation, book description and image tools plugins', () => {
    expect(createViewer({ jhsearch: { iiifBaseUrl: 'x', collectionId: 'dlmm' } })).toBe(viewer);
    expect(Mirador.viewer).toHaveBeenCalledWith(expect.objectContaining({ id: 'rosa-viewer' }), [
      { name: 'jhsearch' },
      { name: 'annotations' },
      { name: 'description' },
      { name: 'imageTools' },
    ]);
  });

  it('starts with the sidebar closed, enables the image tools and watches for annotations', () => {
    createViewer({ jhsearch: { iiifBaseUrl: 'x', collectionId: 'dlmm' } });
    expect(Mirador.viewer).toHaveBeenCalledWith(
      expect.objectContaining({
        window: expect.objectContaining({ sideBarOpen: false, imageToolsEnabled: true }),
      }),
      expect.any(Array)
    );
    expect(openAnnotationsByDefault).toHaveBeenCalledWith(viewer.store);
  });
});
