/**
 * Tests for pagination selectors.
 */

import { describe, it, expect } from 'vitest';
import { getTotalPages, isResultWindowExceeded } from './selectors';
import { MAX_RESULT_WINDOW } from '../utils/queryBuilder';

function stateWith(total, pageSize) {
  return { jhsearch: { pagination: { page: 0, pageSize, total } } };
}

describe('getTotalPages', () => {
  it('counts pages for small result sets', () => {
    expect(getTotalPages(stateWith(0, 20))).toBe(0);
    expect(getTotalPages(stateWith(1, 20))).toBe(1);
    expect(getTotalPages(stateWith(100, 30))).toBe(4);
  });

  it.each([20, 30, 40, 50])(
    'keeps the last page inside the result window at page size %i',
    (pageSize) => {
      const pages = getTotalPages(stateWith(MAX_RESULT_WINDOW + 1, pageSize));
      const lastFrom = (pages - 1) * pageSize;

      expect(lastFrom + pageSize).toBeLessThanOrEqual(MAX_RESULT_WINDOW);
      expect(lastFrom + 2 * pageSize).toBeGreaterThan(MAX_RESULT_WINDOW);
    }
  );

  it('clamps a 10,001 result search at page size 30 to 333 pages', () => {
    expect(getTotalPages(stateWith(10001, 30))).toBe(333);
    expect(getTotalPages(stateWith(10001, 20))).toBe(500);
  });
});

describe('isResultWindowExceeded', () => {
  it('is set only when the total is past the window', () => {
    expect(isResultWindowExceeded(stateWith(MAX_RESULT_WINDOW, 20))).toBe(false);
    expect(isResultWindowExceeded(stateWith(MAX_RESULT_WINDOW + 1, 20))).toBe(true);
  });
});
