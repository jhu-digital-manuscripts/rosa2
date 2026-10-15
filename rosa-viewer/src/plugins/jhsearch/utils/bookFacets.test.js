/**
 * Tests for locally computed book facets.
 */

import { describe, it, expect } from 'vitest';
import {
  BOOK_FACET_PAGE_SIZE,
  MATCHED_BOOKS_AGG,
  MATCHED_PAGE_BOOKS_AGG,
  MAX_TERMS_VALUES,
  bookMatchesFacets,
  booksInCollection,
  buildAllowedBooksFilter,
  buildBookFacetQuery,
  buildMatchedBookAggregations,
  computeBookFacetCounts,
  getMatchedBookIds,
} from './bookFacets';

const categories = [
  { name: 'has_transcription', label: 'Transcription' },
  { name: 'origin', label: 'Origin' },
  { name: 'num_pages', label: 'Number of Pages', 'quantize-interval': 100 },
];

const books = [
  { id: 'rose.A', has_transcription: true, origin: 'Paris', num_pages: 120 },
  { id: 'rose.B', has_transcription: false, origin: 'Paris', num_pages: 250 },
  { id: 'rose.C', has_transcription: true, origin: ['Lyon', 'Paris'], num_pages: 199 },
  { id: 'pizan.D', has_transcription: false, num_pages: 40 },
];

describe('buildBookFacetQuery', () => {
  it('pages through the collection, fetching only id and the category fields', () => {
    const query = buildBookFacetQuery({ collectionId: 'dlmm', categories, page: 2 });

    expect(query.query.bool.filter).toEqual([{ terms: { collection_id: ['dlmm'] } }]);
    expect(query.from).toBe(2 * BOOK_FACET_PAGE_SIZE);
    expect(query.size).toBe(BOOK_FACET_PAGE_SIZE);
    expect(query._source).toEqual([
      'id',
      'collection_id',
      'has_transcription',
      'origin',
      'num_pages',
    ]);
    expect(query.sort).toEqual([{ id: 'asc' }]);
  });
});

describe('booksInCollection', () => {
  it('picks the books of a child collection from those of the top-level one', () => {
    const loaded = [
      { id: 'rose.A', collection_id: ['rose', 'dlmm'] },
      { id: 'pizan.D', collection_id: ['pizan', 'dlmm'] },
      { id: 'aor.X', collection_id: 'aor' },
    ];

    expect(booksInCollection(loaded, 'rose').map((b) => b.id)).toEqual(['rose.A']);
    expect(booksInCollection(loaded, 'dlmm').map((b) => b.id)).toEqual(['rose.A', 'pizan.D']);
    expect(booksInCollection(loaded, 'aor').map((b) => b.id)).toEqual(['aor.X']);
  });
});

describe('getMatchedBookIds', () => {
  it('collects the books of matching pages and the books that matched directly', () => {
    expect(buildMatchedBookAggregations()).toHaveProperty(MATCHED_PAGE_BOOKS_AGG);

    const ids = getMatchedBookIds({
      aggregations: {
        [MATCHED_PAGE_BOOKS_AGG]: { buckets: [{ key: 'rose.A', doc_count: 3 }] },
        [MATCHED_BOOKS_AGG]: {
          doc_count: 2,
          ids: { buckets: [{ key: 'rose.A' }, { key: 'rose.B' }] },
        },
      },
    });

    expect([...ids].sort()).toEqual(['rose.A', 'rose.B']);
  });

  it('is empty without aggregations', () => {
    expect(getMatchedBookIds({}).size).toBe(0);
  });
});

describe('computeBookFacetCounts', () => {
  it('counts matched books by value, so a transcription search shows Yes', () => {
    const counts = computeBookFacetCounts(books, new Set(['rose.A', 'rose.C']), categories);

    expect(counts.has_transcription).toEqual([{ key: true, count: 2 }]);
  });

  it('counts each value of a multi-valued field and buckets quantized fields', () => {
    const counts = computeBookFacetCounts(
      books,
      new Set(['rose.A', 'rose.B', 'rose.C', 'pizan.D']),
      categories
    );

    expect(counts.origin).toEqual(
      expect.arrayContaining([
        { key: 'Paris', count: 3 },
        { key: 'Lyon', count: 1 },
      ])
    );
    expect(counts.origin).toHaveLength(2);
    expect(counts.num_pages).toEqual(
      expect.arrayContaining([
        { key: 0, count: 1 },
        { key: 100, count: 2 },
        { key: 200, count: 1 },
      ])
    );
  });

  it('gives every category an empty list when nothing matched', () => {
    expect(computeBookFacetCounts(books, new Set(), categories)).toEqual({
      has_transcription: [],
      origin: [],
      num_pages: [],
    });
  });
});

describe('bookMatchesFacets', () => {
  it('ORs values within a category and ANDs categories', () => {
    const facets = { origin: ['Lyon', 'Paris'], has_transcription: [true] };

    expect(bookMatchesFacets(books[0], facets, categories)).toBe(true);
    expect(bookMatchesFacets(books[1], facets, categories)).toBe(false);
    expect(bookMatchesFacets(books[3], facets, categories)).toBe(false);
  });

  it('accepts values restored from a URL as strings', () => {
    expect(bookMatchesFacets(books[0], { has_transcription: ['true'] }, categories)).toBe(true);
    expect(bookMatchesFacets(books[2], { num_pages: ['100'] }, categories)).toBe(true);
    expect(bookMatchesFacets(books[1], { num_pages: ['100'] }, categories)).toBe(false);
  });
});

describe('buildAllowedBooksFilter', () => {
  it('returns null when nothing is selected', () => {
    expect(buildAllowedBooksFilter(books, {}, categories)).toBeNull();
    expect(buildAllowedBooksFilter(books, { origin: [] }, categories)).toBeNull();
  });

  it('lists the allowed books by id and manifest_id when they are fewer', () => {
    expect(buildAllowedBooksFilter(books, { origin: ['Lyon'] }, categories)).toEqual({
      bool: {
        should: [{ terms: { id: ['rose.C'] } }, { terms: { manifest_id: ['rose.C'] } }],
        minimum_should_match: 1,
      },
    });
  });

  it('excludes the other books when they are fewer', () => {
    expect(
      buildAllowedBooksFilter(books, { num_pages: ['0', '100', '200'] }, categories)
    ).toBeNull();
    expect(buildAllowedBooksFilter(books, { num_pages: ['100', '200'] }, categories)).toEqual({
      bool: {
        must_not: [{ terms: { id: ['pizan.D'] } }, { terms: { manifest_id: ['pizan.D'] } }],
      },
    });
  });

  it('matches nothing when no book is allowed', () => {
    expect(buildAllowedBooksFilter(books, { origin: ['Rome'] }, categories)).toEqual({
      match_none: {},
    });
  });

  it('splits long id lists to stay within the proxy terms limit', () => {
    const many = Array.from({ length: 2 * MAX_TERMS_VALUES + 10 }, (_, i) => ({
      id: `rose.B${i}`,
      origin: i % 2 === 0 ? 'Paris' : 'Lyon',
    }));

    const filter = buildAllowedBooksFilter(many, { origin: ['Paris'] }, categories);
    const clauses = filter.bool.should;

    expect(clauses).toHaveLength(4);
    for (const clause of clauses) {
      const values = Object.values(clause.terms)[0];
      expect(values.length).toBeLessThanOrEqual(MAX_TERMS_VALUES);
    }
  });
});
