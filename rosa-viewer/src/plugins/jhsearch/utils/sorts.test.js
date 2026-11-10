import { describe, it, expect } from 'vitest';
import { DEFAULT_SORT, resolveSort, sortToken, sortsForMode } from './sorts';

describe('sorts', () => {
  it('offers titles when browsing and relevance and page order when searching', () => {
    expect(sortsForMode('browse').map((s) => s.label)).toEqual(['Title (A-Z)', 'Title (Z-A)']);
    expect(sortsForMode('search').map((s) => s.label)).toEqual(['Relevance', 'Page Order']);
  });

  it("lists each mode's default sort first", () => {
    for (const mode of ['browse', 'search']) {
      expect(sortsForMode(mode)[0].key).toBe(DEFAULT_SORT[mode]);
    }
  });

  it('resolves a sort key within its mode and falls back to the default', () => {
    expect(resolveSort('search', 'page')).toEqual({ field: 'page_num', order: 'asc' });
    expect(resolveSort('browse', 'page')).toEqual({ field: 'label.keyword', order: 'asc' });
    expect(resolveSort('search')).toEqual({ field: '_score', order: 'desc' });
  });

  it('finds the key of a sort', () => {
    expect(sortToken('label.keyword', 'desc')).toBe('title_desc');
    expect(sortToken('label.keyword', 'sideways')).toBeNull();
  });
});
