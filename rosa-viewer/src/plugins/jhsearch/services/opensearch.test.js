/**
 * Tests for opensearch service.
 */

import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { executeSearch, parseSearchResults } from '../services/opensearch';
import { mockSearchResponse, createFetchResponse } from '../../../test/mocks';

/** Build a Response-like object with a status and a readable body. */
function errorResponse(status, body = 'error') {
  return {
    ok: false,
    status,
    statusText: 'Error',
    headers: { get: () => null },
    text: () => Promise.resolve(body),
  };
}

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
      global.fetch = vi.fn().mockResolvedValue(errorResponse(500, 'Server error'));

      const query = { query: { match_all: {} } };
      await expect(executeSearch(opensearchUrl, query)).rejects.toThrow();
    });

    it('should not retry a non-transient status', async () => {
      global.fetch = vi.fn().mockResolvedValue(errorResponse(400, 'bad query'));

      await expect(executeSearch(opensearchUrl, {})).rejects.toThrow();
      expect(global.fetch).toHaveBeenCalledTimes(1);
    });

    it('should not retry a success', async () => {
      global.fetch = vi.fn().mockResolvedValue(createFetchResponse(mockSearchResponse));

      await executeSearch(opensearchUrl, {});
      expect(global.fetch).toHaveBeenCalledTimes(1);
    });
  });

  describe('executeSearch retries', () => {
    beforeEach(() => {
      vi.resetAllMocks();
      vi.useFakeTimers();
    });

    afterEach(() => {
      vi.useRealTimers();
    });

    /** Run a search to completion, advancing past any retry delay. */
    async function runWithTimers(promise) {
      await vi.runAllTimersAsync();
      return promise;
    }

    it.each([502, 503, 504])('should retry once on %i and succeed', async (status) => {
      global.fetch = vi
        .fn()
        .mockResolvedValueOnce(errorResponse(status))
        .mockResolvedValueOnce(createFetchResponse(mockSearchResponse));

      const result = await runWithTimers(executeSearch(opensearchUrl, {}));

      expect(global.fetch).toHaveBeenCalledTimes(2);
      expect(result).toEqual(mockSearchResponse);
    });

    it('should give up after the retry and report the last status', async () => {
      global.fetch = vi.fn().mockResolvedValue(errorResponse(503));

      const promise = executeSearch(opensearchUrl, {});
      const assertion = expect(promise).rejects.toMatchObject({ status: 503 });
      await vi.runAllTimersAsync();
      await assertion;

      expect(global.fetch).toHaveBeenCalledTimes(2);
    });

    it('should not retry 429, which retrying cannot help', async () => {
      global.fetch = vi.fn().mockResolvedValue(errorResponse(429));

      const promise = executeSearch(opensearchUrl, {});
      const assertion = expect(promise).rejects.toMatchObject({ status: 429 });
      await vi.runAllTimersAsync();
      await assertion;

      expect(global.fetch).toHaveBeenCalledTimes(1);
    });

    it('should retry a network error once', async () => {
      global.fetch = vi
        .fn()
        .mockRejectedValueOnce(new TypeError('Failed to fetch'))
        .mockResolvedValueOnce(createFetchResponse(mockSearchResponse));

      const result = await runWithTimers(executeSearch(opensearchUrl, {}));

      expect(global.fetch).toHaveBeenCalledTimes(2);
      expect(result).toEqual(mockSearchResponse);
    });

    it('should not retry a timeout', async () => {
      const abortError = new Error('aborted');
      abortError.name = 'AbortError';
      global.fetch = vi.fn().mockRejectedValue(abortError);

      const promise = executeSearch(opensearchUrl, {});
      const assertion = expect(promise).rejects.toMatchObject({ status: 408 });
      await vi.runAllTimersAsync();
      await assertion;

      expect(global.fetch).toHaveBeenCalledTimes(1);
    });

    it('should honour attempts: 1 to disable retrying', async () => {
      global.fetch = vi.fn().mockResolvedValue(errorResponse(503));

      const promise = executeSearch(opensearchUrl, {}, { attempts: 1 });
      const assertion = expect(promise).rejects.toMatchObject({ status: 503 });
      await vi.runAllTimersAsync();
      await assertion;

      expect(global.fetch).toHaveBeenCalledTimes(1);
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

    it('should key boolean buckets by true and false', () => {
      const parsed = parseSearchResults({
        hits: { hits: [], total: { value: 0 } },
        aggregations: {
          has_transcription: {
            buckets: [
              { key: 0, key_as_string: 'false', doc_count: 243 },
              { key: 1, key_as_string: 'true', doc_count: 7 },
            ],
          },
        },
      });

      expect(parsed.aggregations.has_transcription).toEqual([
        { key: false, count: 243 },
        { key: true, count: 7 },
      ]);
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

  describe('parseSearchResults partial responses', () => {
    const complete = {
      _shards: { total: 10, successful: 10, failed: 0 },
      timed_out: false,
      hits: { total: { value: 1 }, hits: [] },
    };

    beforeEach(() => {
      vi.spyOn(console, 'warn').mockImplementation(() => {});
    });

    afterEach(() => {
      vi.restoreAllMocks();
    });

    it('should report no partial flag for a complete response', () => {
      expect(parseSearchResults(complete).partial).toBeNull();
    });

    it('should report failed shards', () => {
      const parsed = parseSearchResults({
        ...complete,
        _shards: {
          total: 10,
          successful: 5,
          failed: 5,
          failures: [{ index: 'manifest', reason: { type: 'query_shard_exception' } }],
        },
      });

      expect(parsed.partial).toMatchObject({ failedShards: 5, totalShards: 10, timedOut: false });
      expect(parsed.partial.message).toContain('5 of 10 shards failed');
    });

    it('should report a timed-out search', () => {
      const parsed = parseSearchResults({ ...complete, timed_out: true });

      expect(parsed.partial).toMatchObject({ failedShards: 0, timedOut: true });
      expect(parsed.partial.message).toContain('timed out');
    });

    it('should report both causes together', () => {
      const parsed = parseSearchResults({
        ...complete,
        _shards: { total: 10, successful: 5, failed: 5 },
        timed_out: true,
      });

      expect(parsed.partial.message).toContain('shards failed');
      expect(parsed.partial.message).toContain('timed out');
    });

    it('should still return the hits it did get', () => {
      const parsed = parseSearchResults({
        _shards: { total: 10, successful: 5, failed: 5 },
        hits: { total: { value: 1 }, hits: [{ _id: 'aor.Ha2.001r', _index: 'canvas' }] },
      });

      expect(parsed.items).toHaveLength(1);
      expect(parsed.partial).not.toBeNull();
    });

    it('should not treat a response without _shards as partial', () => {
      expect(parseSearchResults({ hits: { total: { value: 0 }, hits: [] } }).partial).toBeNull();
    });
  });
});
