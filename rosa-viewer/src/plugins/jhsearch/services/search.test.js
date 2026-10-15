/**
 * Tests for running a search with locally computed book facets.
 */

import { describe, it, expect, vi, beforeEach } from 'vitest';

vi.mock('./opensearch', async (importOriginal) => ({
  ...(await importOriginal()),
  searchManifests: vi.fn(),
  searchAll: vi.fn(),
}));
vi.mock('./bookFacets', () => ({ loadBookFacets: vi.fn() }));

import { searchAll, searchManifests } from './opensearch';
import { loadBookFacets } from './bookFacets';
import { runSearch } from './search';
import { MATCHED_BOOKS_AGG, MATCHED_PAGE_BOOKS_AGG } from '../utils/bookFacets';

const categories = [{ name: 'has_transcription', label: 'Transcription' }];
const books = [
  { id: 'rose.A', collection_id: ['rose', 'dlmm'], has_transcription: true },
  { id: 'rose.B', collection_id: ['rose', 'dlmm'], has_transcription: true },
  { id: 'rose.C', collection_id: ['rose', 'dlmm'], has_transcription: false },
  { id: 'pizan.D', collection_id: ['pizan', 'dlmm'], has_transcription: true },
  { id: 'pizan.E', collection_id: ['pizan', 'dlmm'], has_transcription: false },
];

const params = {
  opensearchUrl: 'http://localhost:9200/_search',
  collectionId: 'dlmm',
  rootCollectionId: 'dlmm',
  viewMode: 'search',
  searchMode: 'advanced',
  simpleQuery: '',
  advancedRows: [{ field: 'transcription', value: 'red', operator: 'AND' }],
  facets: {},
  page: 0,
  pageSize: 20,
  sortField: '_score',
  sortOrder: 'desc',
  categories,
  fields: [{ name: 'transcription', has_subfields: true }],
  defaultFields: ['transcription'],
};

/** A response with page hits from rose.A (twice) and rose.B. */
const pageMatches = {
  hits: { total: { value: 3 }, hits: [] },
  aggregations: {
    [MATCHED_PAGE_BOOKS_AGG]: {
      buckets: [
        { key: 'rose.A', doc_count: 2 },
        { key: 'rose.B', doc_count: 1 },
      ],
    },
    [MATCHED_BOOKS_AGG]: { doc_count: 0, ids: { buckets: [] } },
  },
};

describe('runSearch', () => {
  beforeEach(() => {
    searchAll.mockReset();
    searchManifests.mockReset();
    loadBookFacets.mockReset();
  });

  it('counts the distinct books of page matches in an advanced search', async () => {
    loadBookFacets.mockResolvedValue(books);
    searchAll.mockResolvedValue(pageMatches);

    const results = await runSearch(params);

    expect(searchAll.mock.calls[0][1].aggs).toHaveProperty(MATCHED_PAGE_BOOKS_AGG);
    expect(results.aggregations.has_transcription).toEqual([{ key: true, count: 2 }]);
  });

  it('uses the books of the top-level collection for a child collection', async () => {
    loadBookFacets.mockResolvedValue(books);
    searchAll.mockResolvedValue(pageMatches);

    await runSearch({ ...params, collectionId: 'pizan', facets: { has_transcription: [true] } });

    expect(loadBookFacets).toHaveBeenCalledWith(params.opensearchUrl, 'dlmm', categories);
    // Only pizan.D is a pizan book with a transcription
    expect(searchAll.mock.calls[0][1].query.bool.filter).toContainEqual({
      bool: {
        should: [{ terms: { id: ['pizan.D'] } }, { terms: { manifest_id: ['pizan.D'] } }],
        minimum_should_match: 1,
      },
    });
  });

  it('falls back to Opensearch aggregations when the books cannot be loaded', async () => {
    vi.spyOn(console, 'warn').mockImplementation(() => {});
    loadBookFacets.mockRejectedValue(new Error('down'));
    searchAll.mockResolvedValue({ hits: { total: { value: 0 }, hits: [] } });

    const results = await runSearch(params);

    expect(searchAll.mock.calls[0][1].aggs).toHaveProperty('has_transcription');
    expect(results.aggregations).toEqual({});
  });

  it('does not load books when facets are disabled', async () => {
    searchAll.mockResolvedValue({ hits: { total: { value: 0 }, hits: [] } });

    await runSearch({ ...params, enableFacets: false });

    expect(loadBookFacets).not.toHaveBeenCalled();
  });

  it('browses the manifest index with Opensearch aggregations', async () => {
    searchManifests.mockResolvedValue({
      hits: { total: { value: 0 }, hits: [] },
      aggregations: {
        has_transcription: { buckets: [{ key: 1, key_as_string: 'true', doc_count: 4 }] },
      },
    });

    const results = await runSearch({ ...params, viewMode: 'browse' });

    expect(loadBookFacets).not.toHaveBeenCalled();
    expect(results.aggregations.has_transcription).toEqual([{ key: true, count: 4 }]);
  });
});
