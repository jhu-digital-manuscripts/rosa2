/**
 * Tests for the search response cache.
 */

import { describe, it, expect } from 'vitest';
import { SearchCache, searchCacheKey } from './searchCache';

describe('SearchCache', () => {
  it('should return what was stored', () => {
    const cache = new SearchCache();
    cache.set('a', 1);

    expect(cache.get('a')).toBe(1);
    expect(cache.get('b')).toBeUndefined();
  });

  it('should expire entries after the ttl', () => {
    let now = 0;
    const cache = new SearchCache({ ttl: 100, now: () => now });
    cache.set('a', 1);

    now = 99;
    expect(cache.get('a')).toBe(1);
    now = 100;
    expect(cache.get('a')).toBeUndefined();
    expect(cache.size).toBe(0);
  });

  it('should evict the least recently used entry when full', () => {
    const cache = new SearchCache({ maxEntries: 2 });
    cache.set('a', 1);
    cache.set('b', 2);
    cache.get('a');
    cache.set('c', 3);

    expect(cache.get('a')).toBe(1);
    expect(cache.get('b')).toBeUndefined();
    expect(cache.get('c')).toBe(3);
  });

  it('should only delete a matching value when one is given', () => {
    const cache = new SearchCache();
    cache.set('a', 'new');

    cache.delete('a', 'old');
    expect(cache.get('a')).toBe('new');

    cache.delete('a', 'new');
    expect(cache.get('a')).toBeUndefined();
  });

  it('should clear every entry', () => {
    const cache = new SearchCache();
    cache.set('a', 1);
    cache.set('b', 2);
    cache.clear();

    expect(cache.size).toBe(0);
  });
});

describe('searchCacheKey', () => {
  it('should distinguish endpoint and query', () => {
    const query = { from: 0 };

    expect(searchCacheKey('/_search', query)).toBe(searchCacheKey('/_search', { from: 0 }));
    expect(searchCacheKey('/_search', query)).not.toBe(searchCacheKey('/manifest/_search', query));
    expect(searchCacheKey('/_search', query)).not.toBe(searchCacheKey('/_search', { from: 20 }));
  });
});
