/**
 * Tests for loading the facet fields of every book in a collection.
 */

import { describe, it, expect, vi, beforeEach } from 'vitest';

vi.mock('./opensearch', () => ({ searchManifests: vi.fn() }));

import { searchManifests } from './opensearch';
import { clearBookFacets, loadBookFacets } from './bookFacets';
import { BOOK_FACET_PAGE_SIZE } from '../utils/bookFacets';

const categories = [{ name: 'has_transcription' }];
const URL = 'http://localhost:9200/_search';

function page(ids, total) {
  return {
    hits: {
      total: { value: total },
      hits: ids.map((id) => ({ _source: { id, has_transcription: true } })),
    },
  };
}

describe('loadBookFacets', () => {
  beforeEach(() => {
    clearBookFacets();
    searchManifests.mockReset();
  });

  it('fetches every page and returns the books', async () => {
    const total = BOOK_FACET_PAGE_SIZE + 2;
    searchManifests
      .mockResolvedValueOnce(page(['a', 'b'], total))
      .mockResolvedValueOnce(page(['c'], total));

    const books = await loadBookFacets(URL, 'dlmm', categories);

    expect(books.map((b) => b.id)).toEqual(['a', 'b', 'c']);
    expect(searchManifests).toHaveBeenCalledTimes(2);
    expect(searchManifests.mock.calls[1][1].from).toBe(BOOK_FACET_PAGE_SIZE);
  });

  it('loads a collection once', async () => {
    searchManifests.mockResolvedValue(page(['a'], 1));

    await Promise.all([
      loadBookFacets(URL, 'dlmm', categories),
      loadBookFacets(URL, 'dlmm', categories),
    ]);
    await loadBookFacets(URL, 'dlmm', categories);

    expect(searchManifests).toHaveBeenCalledTimes(1);
  });

  it('tries again after a failed load', async () => {
    searchManifests.mockRejectedValueOnce(new Error('down')).mockResolvedValueOnce(page(['a'], 1));

    await expect(loadBookFacets(URL, 'dlmm', categories)).rejects.toThrow('down');
    await expect(loadBookFacets(URL, 'dlmm', categories)).resolves.toHaveLength(1);
  });
});
