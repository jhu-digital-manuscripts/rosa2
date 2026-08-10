/**
 * Tests for opensearch service.
 */

import { describe, it, expect, vi, beforeEach } from 'vitest';
import { executeSearch, parseSearchResults } from '../services/opensearch';
import { mockSearchResponse, createFetchResponse } from '../../../test/mocks';

describe('opensearch service', () => {
  const opensearchUrl = 'http://localhost:9200/_search';

  beforeEach(() => {
    vi.resetAllMocks();
  });

  describe('executeSearch', () => {
    it('should execute search query', async () => {
      global.fetch = vi.fn().mockResolvedValue(createFetchResponse(mockSearchResponse));

      const query = { query: { match_all: {} }, size: 20 };
      const result = await executeSearch(opensearchUrl, query);

      expect(global.fetch).toHaveBeenCalledWith(
        opensearchUrl,
        expect.objectContaining({ method: 'POST' })
      );
      expect(result).toEqual(mockSearchResponse);
    });

    it('should throw on fetch error', async () => {
      global.fetch = vi.fn().mockResolvedValue({
        ok: false,
        status: 500,
        text: () => Promise.resolve('Server error'),
      });

      const query = { query: { match_all: {} } };
      await expect(executeSearch(opensearchUrl, query)).rejects.toThrow();
    });
  });

  describe('parseSearchResults', () => {
    it('should parse manifest results correctly', () => {
      const parsed = parseSearchResults(mockSearchResponse);

      expect(parsed.total).toBe(2);
      expect(parsed.items).toHaveLength(2);
      expect(parsed.items[0]).toMatchObject({
        id: 'rose/Douce195',
        index: 'manifest',
        score: 1.5,
      });
    });

    it('should parse aggregations', () => {
      const parsed = parseSearchResults(mockSearchResponse);

      expect(parsed.aggregations).toHaveProperty('origin');
      expect(parsed.aggregations.origin).toEqual([
        { key: 'France', count: 15 },
        { key: 'Italy', count: 5 },
      ]);
    });

    it('should handle empty results', () => {
      const emptyResponse = { hits: { total: { value: 0 }, hits: [] } };
      const parsed = parseSearchResults(emptyResponse);

      expect(parsed.total).toBe(0);
      expect(parsed.items).toHaveLength(0);
    });
  });
});
