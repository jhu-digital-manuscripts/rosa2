import { describe, it, expect, vi } from 'vitest';

vi.mock('mirador', () => ({ default: { viewer: vi.fn(() => 'viewer') } }));
vi.mock('./plugins/jhsearch', () => ({ jhsearchPlugin: [{ name: 'jhsearch' }] }));
vi.mock('./plugins/annotations', () => ({ rosaAnnotationsPlugin: [{ name: 'annotations' }] }));
vi.mock('./plugins/description', () => ({ rosaDescriptionPlugin: [{ name: 'description' }] }));

import Mirador from 'mirador';
import { createViewer } from './index';

describe('createViewer', () => {
  it('registers the search, annotation and book description plugins', () => {
    expect(createViewer({ jhsearch: { iiifBaseUrl: 'x', collectionId: 'dlmm' } })).toBe('viewer');
    expect(Mirador.viewer).toHaveBeenCalledWith(expect.objectContaining({ id: 'rosa-viewer' }), [
      { name: 'jhsearch' },
      { name: 'annotations' },
      { name: 'description' },
    ]);
  });
});
