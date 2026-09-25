/**
 * Tests for facet value semantics.
 */

import { describe, it, expect } from 'vitest';
import {
  getQuantizeInterval,
  getBucketRange,
  formatFacetValue,
  buildCategoryFilter,
} from '../utils/facets';

const numIllustrations = {
  name: 'num_illustrations',
  label: 'Number of Illustrations',
  'quantize-interval': 10,
};
const numPages = { name: 'num_pages', label: 'Number of Pages', 'quantize-interval': 100 };
const origin = { name: 'origin', label: 'Origin' };

describe('getQuantizeInterval', () => {
  it('returns the interval for a quantized category', () => {
    expect(getQuantizeInterval(numIllustrations)).toBe(10);
    expect(getQuantizeInterval(numPages)).toBe(100);
  });

  it('returns null for a plain terms category', () => {
    expect(getQuantizeInterval(origin)).toBeNull();
  });

  it('returns null for missing or nonsensical intervals', () => {
    expect(getQuantizeInterval(undefined)).toBeNull();
    expect(getQuantizeInterval({})).toBeNull();
    expect(getQuantizeInterval({ 'quantize-interval': 0 })).toBeNull();
    expect(getQuantizeInterval({ 'quantize-interval': -10 })).toBeNull();
    expect(getQuantizeInterval({ 'quantize-interval': 'abc' })).toBeNull();
  });
});

describe('getBucketRange', () => {
  it('treats the bucket key as the lower bound of a half-open range', () => {
    expect(getBucketRange(20, 10)).toEqual({ gte: 20, lt: 30 });
    expect(getBucketRange(0, 20)).toEqual({ gte: 0, lt: 20 });
    expect(getBucketRange(100, 100)).toEqual({ gte: 100, lt: 200 });
  });

  it('accepts the float keys Opensearch returns for histogram buckets', () => {
    expect(getBucketRange(20.0, 10)).toEqual({ gte: 20, lt: 30 });
    expect(getBucketRange('30', 10)).toEqual({ gte: 30, lt: 40 });
  });

  it('returns null for non-numeric keys', () => {
    expect(getBucketRange('not a number', 10)).toBeNull();
    expect(getBucketRange(null, 10)).toBeNull();
    expect(getBucketRange(undefined, 10)).toBeNull();
    expect(getBucketRange('', 10)).toBeNull();
    expect(getBucketRange(true, 10)).toBeNull();
  });
});

describe('formatFacetValue', () => {
  it('formats quantized buckets as inclusive ranges', () => {
    expect(formatFacetValue(0, numIllustrations)).toBe('0-9');
    expect(formatFacetValue(10, numIllustrations)).toBe('10-19');
    expect(formatFacetValue(260, numIllustrations)).toBe('260-269');
    expect(formatFacetValue(0, numPages)).toBe('0-99');
    expect(formatFacetValue(100, numPages)).toBe('100-199');
  });

  it('does not special-case the zero bucket', () => {
    // Zero shares the first bucket with the rest of the interval.
    expect(formatFacetValue(0, { 'quantize-interval': 20 })).toBe('0-19');
  });

  it('normalizes the float keys Opensearch returns', () => {
    expect(formatFacetValue(20.0, numIllustrations)).toBe('20-29');
  });

  it('formats booleans as Yes/No', () => {
    expect(formatFacetValue(true, { name: 'has_transcription' })).toBe('Yes');
    expect(formatFacetValue(false, { name: 'has_transcription' })).toBe('No');
  });

  it('passes other values through as strings', () => {
    expect(formatFacetValue('France', origin)).toBe('France');
    expect(formatFacetValue('', origin)).toBe('(empty)');
  });
});

describe('buildCategoryFilter', () => {
  it('uses a terms filter for plain categories', () => {
    expect(buildCategoryFilter('origin', ['France', 'Italy'], origin)).toEqual({
      terms: { origin: ['France', 'Italy'] },
    });
  });

  it('uses a range filter covering the whole bucket for quantized categories', () => {
    // The bug this replaces: terms {num_illustrations: [20]} matched only the single
    // value 20, not the 20-29 bucket the user clicked.
    expect(buildCategoryFilter('num_illustrations', [20], numIllustrations)).toEqual({
      bool: {
        should: [{ range: { num_illustrations: { gte: 20, lt: 30 } } }],
        minimum_should_match: 1,
      },
    });
  });

  it('ORs multiple selected buckets together', () => {
    expect(buildCategoryFilter('num_pages', [0, 200], numPages)).toEqual({
      bool: {
        should: [
          { range: { num_pages: { gte: 0, lt: 100 } } },
          { range: { num_pages: { gte: 200, lt: 300 } } },
        ],
        minimum_should_match: 1,
      },
    });
  });

  it('returns null when there is nothing selected', () => {
    expect(buildCategoryFilter('origin', [], origin)).toBeNull();
    expect(buildCategoryFilter('origin', undefined, origin)).toBeNull();
  });

  it('falls back to a terms filter when the category is unknown', () => {
    expect(buildCategoryFilter('origin', ['France'], undefined)).toEqual({
      terms: { origin: ['France'] },
    });
  });
});
