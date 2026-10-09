/**
 * Tests for collections utility.
 */

import { describe, it, expect } from 'vitest';
import { buildCollectionItems } from './collections';

describe('buildCollectionItems', () => {
  it('uses collection ids as labels when no labels are configured', () => {
    expect(buildCollectionItems({ collectionId: 'dlmm', childCollectionIds: ['rose'] })).toEqual([
      { id: 'dlmm', label: 'dlmm' },
      { id: 'rose', label: 'rose' },
    ]);
  });

  it('handles missing child collection ids', () => {
    expect(buildCollectionItems({ collectionId: 'aor' })).toEqual([{ id: 'aor', label: 'aor' }]);
  });

  it('uses configured labels and id-based logos', () => {
    const items = buildCollectionItems({
      collectionId: 'dlmm',
      childCollectionIds: ['rose', 'pizan'],
      collectionLabels: {
        dlmm: 'Digital Library of Medieval Manuscripts',
        rose: 'Roman de la Rose Digital Library',
      },
    });
    expect(items).toEqual([
      { id: 'dlmm', label: 'Digital Library of Medieval Manuscripts', logo: 'dlmm.jpg' },
      { id: 'rose', label: 'Roman de la Rose Digital Library', logo: 'rose.jpg' },
      { id: 'pizan', label: 'pizan' },
    ]);
  });
});
