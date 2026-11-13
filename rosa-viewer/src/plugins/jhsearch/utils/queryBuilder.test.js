/**
 * Tests for queryBuilder utility.
 */

import { describe, it, expect } from 'vitest';
import {
  buildBrowseQuery,
  buildSimpleSearchQuery,
  buildAdvancedSearchQuery,
  buildTextClause,
  buildFacetAggregations,
  buildSortClause,
  rowHasQuery,
  buildNameFilter,
  buildNameSuggestQuery,
  buildNameLookupQuery,
  pageWindow,
  pageCount,
  MAX_RESULT_WINDOW,
  MAX_PAGE_SIZE,
  NAME_FACET,
  NAME_SUGGEST_FIELDS,
} from '../utils/queryBuilder';
import { buildMatchedBookAggregations, MAX_TERMS_VALUES } from './bookFacets';
import { mockJHSearchService } from '../../../test/mocks';

describe('queryBuilder', () => {
  const fieldDefinitions = mockJHSearchService.fields;
  const categories = mockJHSearchService.categories;
  const defaultFields = mockJHSearchService['default-fields'];

  describe('buildBrowseQuery', () => {
    it('should build a basic browse query with 0-based pagination', () => {
      const query = buildBrowseQuery({
        collectionId: 'rose',
        facets: {},
        page: 0,
        pageSize: 20,
        categories,
      });

      expect(query).toHaveProperty('query');
      expect(query).toHaveProperty('from', 0);
      expect(query).toHaveProperty('size', 20);
    });

    it('should include facet filters when selected', () => {
      const query = buildBrowseQuery({
        collectionId: 'rose',
        facets: { origin: ['France', 'Italy'] },
        page: 0,
        pageSize: 20,
        categories,
      });

      expect(query.query.bool.filter).toEqual(
        expect.arrayContaining([
          expect.objectContaining({
            terms: { origin: ['France', 'Italy'] },
          }),
        ])
      );
    });

    it('should calculate correct pagination offset', () => {
      const query = buildBrowseQuery({
        collectionId: 'rose',
        facets: {},
        page: 2,
        pageSize: 20,
        categories,
      });

      expect(query.from).toBe(40);
      expect(query.size).toBe(20);
    });

    it('should filter quantized categories by bucket range, not exact value', () => {
      const quantizedCategories = [
        { name: 'num_illustrations', label: 'Number of Illustrations', 'quantize-interval': 10 },
      ];

      const query = buildBrowseQuery({
        collectionId: 'rose',
        facets: { num_illustrations: [20] },
        page: 0,
        pageSize: 20,
        categories: quantizedCategories,
      });

      expect(query.query.bool.filter).toEqual(
        expect.arrayContaining([
          {
            bool: {
              should: [{ range: { num_illustrations: { gte: 20, lt: 30 } } }],
              minimum_should_match: 1,
            },
          },
        ])
      );
      // The old behaviour matched only documents with exactly 20 illustrations.
      expect(query.query.bool.filter).not.toEqual(
        expect.arrayContaining([expect.objectContaining({ terms: { num_illustrations: [20] } })])
      );
    });
  });

  describe('pageCount', () => {
    it('counts the pages of the results', () => {
      expect(pageCount(0, 20)).toBe(0);
      expect(pageCount(1, 20)).toBe(1);
      expect(pageCount(150, 20)).toBe(8);
    });

    it('stops at the last page inside the result window', () => {
      expect(pageCount(MAX_RESULT_WINDOW + 1, 20)).toBe(500);
      expect(pageCount(MAX_RESULT_WINDOW + 1, 30)).toBe(333);
    });
  });

  describe('buildSimpleSearchQuery', () => {
    it('should build a multi_match query for simple search', () => {
      const query = buildSimpleSearchQuery({
        query: 'rose',
        defaultFields,
        fieldDefinitions,
        collectionId: 'rose',
        facets: {},
        page: 0,
        pageSize: 20,
        categories,
      });

      expect(query.query.bool.must).toEqual(
        expect.arrayContaining([
          expect.objectContaining({
            multi_match: expect.objectContaining({ query: 'rose' }),
          }),
        ])
      );
    });

    it('should include highlighting', () => {
      const query = buildSimpleSearchQuery({
        query: 'rose',
        defaultFields,
        fieldDefinitions,
        collectionId: 'rose',
        facets: {},
        page: 0,
        pageSize: 20,
        categories,
      });

      expect(query).toHaveProperty('highlight');
    });

    it('lets any bare word match, as Opensearch does by default', () => {
      const query = buildSimpleSearchQuery({
        query: 'rose amant',
        defaultFields: ['label'],
        fieldDefinitions: [],
        collectionId: 'rose',
      });
      expect(query.query.bool.must).toEqual([
        { multi_match: { query: 'rose amant', fields: ['label'], type: 'best_fields' } },
      ]);
    });

    it('searches a quoted phrase beside the bare words', () => {
      const query = buildSimpleSearchQuery({
        query: 'rose "Guillaume de Lorris"',
        defaultFields: ['label'],
        fieldDefinitions: [],
        collectionId: 'rose',
      });
      expect(query.query.bool.must).toEqual([
        {
          bool: {
            should: [
              { multi_match: { query: 'rose', fields: ['label'], type: 'best_fields' } },
              { multi_match: { query: 'Guillaume de Lorris', fields: ['label'], type: 'phrase' } },
            ],
            minimum_should_match: 1,
          },
        },
      ]);
      expect(query.highlight.fields).toEqual({ label: {} });
    });
  });

  describe('buildTextClause', () => {
    const fields = ['title.*'];
    const words = (query, extra = {}) => ({
      multi_match: { query, fields, type: 'best_fields', ...extra },
    });
    const phrase = (query) => ({ multi_match: { query, fields, type: 'phrase' } });

    it('returns null when there is nothing to search for', () => {
      expect(buildTextClause('', fields)).toBeNull();
      expect(buildTextClause(' "" ', fields)).toBeNull();
    });

    it('keeps the plain multi_match shape for bare words', () => {
      expect(buildTextClause('a b', fields)).toEqual(words('a b'));
      expect(buildTextClause('a b', fields, { operator: 'AND' })).toEqual(
        words('a b', { operator: 'and' })
      );
    });

    it('returns a single phrase on its own', () => {
      expect(buildTextClause('"a b"', fields, { operator: 'AND' })).toEqual(phrase('a b'));
    });

    it('combines units with should for OR and must for AND', () => {
      expect(buildTextClause('c "a b"', fields)).toEqual({
        bool: { should: [words('c'), phrase('a b')], minimum_should_match: 1 },
      });
      expect(buildTextClause('c "a b"', fields, { operator: 'AND' })).toEqual({
        bool: { must: [words('c', { operator: 'and' }), phrase('a b')] },
      });
    });
  });

  describe('buildAdvancedSearchQuery', () => {
    it('should build query from multiple rows with AND operator', () => {
      const rows = [
        { field: 'label', value: 'rose', operator: 'AND' },
        { field: 'origin', value: 'France', operator: 'AND' },
      ];

      const query = buildAdvancedSearchQuery({
        rows,
        fieldDefinitions,
        collectionId: 'rose',
        facets: {},
        page: 0,
        pageSize: 20,
        categories,
      });

      // Both rows have values, should be in the query
      expect(query.query.bool).toBeDefined();
    });

    it('should skip empty rows', () => {
      const rows = [
        { field: 'label', value: 'rose', operator: 'AND' },
        { field: '', value: '', operator: 'AND' },
      ];

      const query = buildAdvancedSearchQuery({
        rows,
        fieldDefinitions,
        collectionId: 'rose',
        facets: {},
        page: 0,
        pageSize: 20,
        categories,
      });

      // Should still produce a valid query
      expect(query.query.bool).toBeDefined();
    });

    it('should skip rows for fields the collection does not have', () => {
      const query = buildAdvancedSearchQuery({
        rows: [
          { field: 'label', value: 'rose', operator: 'AND' },
          { field: 'no_such_field', value: 'x', operator: 'AND' },
        ],
        fieldDefinitions,
        collectionId: 'rose',
        categories,
      });

      expect(query.query.bool.must).toHaveLength(1);
      expect(query.query.bool.must[0].multi_match.fields).toEqual(['label']);
    });

    describe('extra text on enumerated fields', () => {
      const markFields = [
        { name: 'mark', label: 'Mark', has_subfields: true, values: [{ value: 'dash' }] },
      ];
      const mm = (value) => ({
        multi_match: { query: value, fields: ['mark.*'], type: 'best_fields', operator: 'and' },
      });
      const build = (row) =>
        buildAdvancedSearchQuery({
          rows: [{ field: 'mark', operator: 'AND', ...row }],
          fieldDefinitions: markFields,
          collectionId: 'aor',
        });

      const dash = { term: { 'mark.keyword': 'dash' } };

      it('requires both the value and the extra text', () => {
        const body = build({ value: 'dash', text: 'regnum' });
        expect(body.query.bool.must).toEqual([{ bool: { must: [dash, mm('regnum')] } }]);
        expect(body.highlight.fields).toEqual({ 'mark.*': {} });
      });

      it('searches the extra text alone when no value is chosen', () => {
        expect(build({ value: '', text: 'regnum' }).query.bool.must).toEqual([mm('regnum')]);
      });

      it('ignores blank extra text', () => {
        expect(build({ value: 'dash', text: '  ' }).query.bool.must).toEqual([dash]);
      });
    });

    describe('dropdown values', () => {
      const fields = [
        { name: 'mark', label: 'Mark', has_subfields: true, values: [{ value: 'X_sign' }] },
        { name: 'method', label: 'Method', values: [{ value: 'pen' }] },
        { name: 'marginalia', label: 'Marginalia', has_subfields: true },
        { name: 'people', label: 'People' },
      ];
      const clauseFor = (field, value) =>
        buildAdvancedSearchQuery({
          rows: [{ field, value, operator: 'AND' }],
          fieldDefinitions: fields,
          collectionId: 'aor',
        }).query.bool.must[0];

      it('uses a term query on the keyword sub-field when the field has sub-fields', () => {
        expect(clauseFor('mark', 'X_sign')).toEqual({ term: { 'mark.keyword': 'X_sign' } });
      });

      it('uses a term query on the field itself when it is a keyword field', () => {
        expect(clauseFor('method', 'pen')).toEqual({ term: { method: 'pen' } });
      });

      it('still uses multi_match for fields without a dropdown', () => {
        expect(clauseFor('marginalia', 'rex')).toEqual({
          multi_match: {
            query: 'rex',
            fields: ['marginalia.*'],
            type: 'best_fields',
            operator: 'and',
          },
        });
        expect(clauseFor('people', 'Livy')).toEqual({
          multi_match: { query: 'Livy', fields: ['people'], type: 'best_fields', operator: 'and' },
        });
      });
    });

    describe('free text', () => {
      const fields = [
        { name: 'marginalia', label: 'Marginalia', has_subfields: true },
        { name: 'people', label: 'People' },
        { name: 'mark', label: 'Mark', has_subfields: true, values: [{ value: 'dash' }] },
      ];
      const clauseFor = (row) =>
        buildAdvancedSearchQuery({
          rows: [{ operator: 'AND', ...row }],
          fieldDefinitions: fields,
          collectionId: 'aor',
        }).query.bool.must[0];
      const words = (query, f) => ({
        multi_match: { query, fields: f, type: 'best_fields', operator: 'and' },
      });
      const phrase = (query, f) => ({ multi_match: { query, fields: f, type: 'phrase' } });

      it('requires every bare word', () => {
        expect(clauseFor({ field: 'marginalia', value: ' ad  fontes ' })).toEqual(
          words('ad fontes', ['marginalia.*'])
        );
      });

      it('searches a quoted value as a phrase', () => {
        expect(clauseFor({ field: 'people', value: '"Julius Caesar"' })).toEqual(
          phrase('Julius Caesar', ['people'])
        );
      });

      it('requires the bare words and every phrase', () => {
        expect(clauseFor({ field: 'marginalia', value: 'rex "ad fontes" "vide infra"' })).toEqual({
          bool: {
            must: [
              words('rex', ['marginalia.*']),
              phrase('ad fontes', ['marginalia.*']),
              phrase('vide infra', ['marginalia.*']),
            ],
          },
        });
      });

      it('accepts a phrase in the extra text of an enumerated field', () => {
        expect(clauseFor({ field: 'mark', value: 'dash', text: '"regnum rex"' })).toEqual({
          bool: { must: [{ term: { 'mark.keyword': 'dash' } }, phrase('regnum rex', ['mark.*'])] },
        });
      });

      it('builds nothing for a row whose text is only quotes', () => {
        const body = buildAdvancedSearchQuery({
          rows: [{ field: 'people', value: '""', operator: 'AND' }],
          fieldDefinitions: fields,
          collectionId: 'aor',
        });
        expect(body).toBeNull();
      });

      it('builds nothing when every row names a field the collection lacks', () => {
        const body = buildAdvancedSearchQuery({
          rows: [{ field: 'nosuchfield', value: 'x', operator: 'AND' }],
          fieldDefinitions: fields,
          collectionId: 'aor',
        });
        expect(body).toBeNull();
      });
    });

    describe('rowHasQuery', () => {
      it('counts a value or extra text, not whitespace', () => {
        expect(rowHasQuery({ value: 'a' })).toBe(true);
        expect(rowHasQuery({ value: '', text: 'b' })).toBe(true);
        expect(rowHasQuery({ value: ' ', text: ' ' })).toBe(false);
        expect(rowHasQuery({})).toBe(false);
      });
    });

    describe('boolean operators', () => {
      const mm = (field, value) => ({
        multi_match: { query: value, fields: [field], type: 'best_fields', operator: 'and' },
      });
      const build = (rows) =>
        buildAdvancedSearchQuery({ rows, fieldDefinitions, collectionId: 'rose', categories }).query
          .bool;
      const filter = [{ terms: { collection_id: ['rose'] } }];

      it('requires every row for AND', () => {
        expect(
          build([
            { field: 'label', value: 'a', operator: 'AND' },
            { field: 'origin', value: 'b', operator: 'AND' },
          ])
        ).toEqual({ must: [mm('label', 'a'), mm('origin', 'b')], filter });
      });

      it('matches either row for OR', () => {
        expect(
          build([
            { field: 'label', value: 'a', operator: 'AND' },
            { field: 'origin', value: 'b', operator: 'OR' },
          ])
        ).toEqual({
          should: [mm('label', 'a'), mm('origin', 'b')],
          minimum_should_match: 1,
          filter,
        });
      });

      it('ignores the operator on the first row', () => {
        expect(build([{ field: 'label', value: 'a', operator: 'OR' }])).toEqual({
          must: [mm('label', 'a')],
          filter,
        });
      });

      it('binds AND tighter than OR', () => {
        // a AND b OR c AND d  ->  (a AND b) OR (c AND d)
        expect(
          build([
            { field: 'label', value: 'a', operator: 'AND' },
            { field: 'origin', value: 'b', operator: 'AND' },
            { field: 'label', value: 'c', operator: 'OR' },
            { field: 'origin', value: 'd', operator: 'AND' },
          ])
        ).toEqual({
          should: [
            { bool: { must: [mm('label', 'a'), mm('origin', 'b')] } },
            { bool: { must: [mm('label', 'c'), mm('origin', 'd')] } },
          ],
          minimum_should_match: 1,
          filter,
        });

        // a OR b AND c  ->  a OR (b AND c)
        expect(
          build([
            { field: 'label', value: 'a', operator: 'AND' },
            { field: 'origin', value: 'b', operator: 'OR' },
            { field: 'label', value: 'c', operator: 'AND' },
          ])
        ).toEqual({
          should: [mm('label', 'a'), { bool: { must: [mm('origin', 'b'), mm('label', 'c')] } }],
          minimum_should_match: 1,
          filter,
        });
      });
    });
  });

  describe('buildSortClause', () => {
    it('should return null for relevance so the caller leaves sort unset', () => {
      expect(buildSortClause('_score', 'desc')).toBeNull();
    });

    it('should build a plain clause for an ordinary field', () => {
      expect(buildSortClause('label.keyword', 'asc')).toEqual([{ 'label.keyword': 'asc' }]);
    });

    it('should set unmapped_type on page_num so manifest shards do not fail', () => {
      // page_num exists only in the canvas mapping, and simple and advanced
      // searches span manifest and canvas. Without unmapped_type every manifest
      // shard fails with "No mapping found for [page_num] in order to sort on",
      // and Opensearch answers 200 with the manifest hits silently absent.
      expect(buildSortClause('page_num', 'asc')).toEqual([
        { page_num: { order: 'asc', missing: '_last', unmapped_type: 'long' } },
      ]);
    });
  });

  describe('page_num sort through the query builders', () => {
    const pageNumSort = [{ page_num: { order: 'asc', missing: '_last', unmapped_type: 'long' } }];

    it('should carry unmapped_type in a simple search', () => {
      const query = buildSimpleSearchQuery({
        query: 'rome',
        defaultFields,
        fieldDefinitions,
        collectionId: 'aor',
        categories,
        sortField: 'page_num',
        sortOrder: 'asc',
      });

      expect(query.sort).toEqual(pageNumSort);
    });

    it('should carry unmapped_type in an advanced search', () => {
      const query = buildAdvancedSearchQuery({
        rows: [{ field: 'label', value: 'rome', operator: 'AND' }],
        fieldDefinitions,
        collectionId: 'aor',
        categories,
        sortField: 'page_num',
        sortOrder: 'asc',
      });

      expect(query.sort).toEqual(pageNumSort);
    });

    it('should leave sort unset when sorting by relevance', () => {
      const query = buildSimpleSearchQuery({
        query: 'rome',
        defaultFields,
        fieldDefinitions,
        collectionId: 'aor',
        categories,
        sortField: '_score',
        sortOrder: 'desc',
      });

      expect(query.sort).toBeUndefined();
    });
  });

  describe('buildFacetAggregations', () => {
    it('should use terms aggregation for regular categories', () => {
      const regularCategories = [
        { name: 'origin', label: 'Origin' },
        { name: 'date', label: 'Date' },
      ];

      const aggs = buildFacetAggregations(regularCategories);

      expect(aggs.origin).toEqual({
        terms: { field: 'origin', size: 100 },
      });
      expect(aggs.date).toEqual({
        terms: { field: 'date', size: 100 },
      });
    });

    it('should use histogram aggregation for categories with quantize-interval', () => {
      const categoriesWithInterval = [
        { name: 'origin', label: 'Origin' },
        { name: 'num_pages', label: 'Number of Pages', 'quantize-interval': 100 },
        { name: 'num_illustrations', label: 'Number of Illustrations', 'quantize-interval': 10 },
      ];

      const aggs = buildFacetAggregations(categoriesWithInterval);

      expect(aggs.origin).toEqual({
        terms: { field: 'origin', size: 100 },
      });
      expect(aggs.num_pages).toEqual({
        histogram: { field: 'num_pages', interval: 100, min_doc_count: 1 },
      });
      expect(aggs.num_illustrations).toEqual({
        histogram: { field: 'num_illustrations', interval: 10, min_doc_count: 1 },
      });
    });
  });

  describe('with the facet fields of every book loaded', () => {
    const books = [
      { id: 'rose.A', origin: 'France' },
      { id: 'rose.B', origin: 'Italy' },
      { id: 'rose.C', origin: 'Italy' },
    ];
    const allowA = {
      bool: {
        should: [{ terms: { id: ['rose.A'] } }, { terms: { manifest_id: ['rose.A'] } }],
        minimum_should_match: 1,
      },
    };

    it('filters a simple search to the allowed books and their pages', () => {
      const query = buildSimpleSearchQuery({
        query: 'red',
        defaultFields,
        fieldDefinitions,
        collectionId: 'rose',
        facets: { origin: ['France'] },
        categories,
        books,
      });

      expect(query.query.bool.filter).toEqual([{ terms: { collection_id: ['rose'] } }, allowA]);
      expect(query.aggs).toEqual(buildMatchedBookAggregations());
    });

    it('filters an advanced search, keeping the Name filter', () => {
      const query = buildAdvancedSearchQuery({
        rows: [{ field: 'label', value: 'rose', operator: 'AND' }],
        fieldDefinitions,
        collectionId: 'rose',
        facets: { id: ['rose.B'], origin: ['France'] },
        categories,
        books,
      });

      expect(query.query.bool.filter).toEqual([
        { terms: { collection_id: ['rose'] } },
        buildNameFilter(['rose.B']),
        allowA,
      ]);
      expect(query.aggs).toEqual(buildMatchedBookAggregations());
    });

    it('builds nothing for an advanced search without rows', () => {
      const query = buildAdvancedSearchQuery({
        rows: [],
        fieldDefinitions,
        collectionId: 'rose',
        facets: {},
        categories,
        books,
      });

      expect(query).toBeNull();
    });

    it('builds nothing for a simple search of only quotes', () => {
      const query = buildSimpleSearchQuery({
        query: ' "" ',
        defaultFields: ['description'],
        fieldDefinitions,
        collectionId: 'rose',
      });

      expect(query).toBeNull();
    });
  });

  describe('Name filter', () => {
    const nameFilter = (ids) => ({
      bool: {
        should: [{ terms: { id: ids } }, { terms: { manifest_id: ids } }],
        minimum_should_match: 1,
      },
    });
    const ids = ['rose.Douce195', 'rose.Douce332'];

    it('uses the id facet key', () => {
      expect(NAME_FACET).toBe('id');
    });

    it('matches manifests by id and canvases by manifest_id', () => {
      expect(buildNameFilter(ids)).toEqual(nameFilter(ids));
    });

    it('returns null when nothing is selected', () => {
      expect(buildNameFilter([])).toBeNull();
      expect(buildNameFilter(undefined)).toBeNull();
    });

    it('splits long id lists to stay within the proxy terms limit', () => {
      const many = Array.from({ length: MAX_TERMS_VALUES + 1 }, (_, i) => `rose.B${i}`);
      const filter = buildNameFilter(many);

      expect(filter.bool.should).toHaveLength(4);
      expect(filter.bool.should[0].terms.id).toHaveLength(MAX_TERMS_VALUES);
      expect(filter.bool.should[1].terms.id).toEqual([`rose.B${MAX_TERMS_VALUES}`]);
      expect(filter.bool.should[2].terms.manifest_id).toHaveLength(MAX_TERMS_VALUES);
      expect(filter.bool.should[3].terms.manifest_id).toEqual([`rose.B${MAX_TERMS_VALUES}`]);
    });

    it('is applied to browse queries', () => {
      const query = buildBrowseQuery({
        collectionId: 'rose',
        facets: { id: ids, origin: ['France'] },
        categories,
      });
      expect(query.query.bool.filter).toEqual([
        { terms: { collection_id: ['rose'] } },
        nameFilter(ids),
        { terms: { origin: ['France'] } },
      ]);
    });

    it('is applied to simple search queries', () => {
      const query = buildSimpleSearchQuery({
        query: 'rose',
        defaultFields,
        fieldDefinitions,
        collectionId: 'rose',
        facets: { id: ids },
        categories,
      });
      expect(query.query.bool.filter).toContainEqual(nameFilter(ids));
    });

    it('is applied to advanced search queries', () => {
      const query = buildAdvancedSearchQuery({
        rows: [{ field: 'label', value: 'rose', operator: 'AND' }],
        fieldDefinitions,
        collectionId: 'rose',
        facets: { id: ids },
        categories,
      });
      expect(query.query.bool.filter).toContainEqual(nameFilter(ids));
    });

    it('is not aggregated as a category', () => {
      const query = buildBrowseQuery({ collectionId: 'rose', facets: { id: ids }, categories });
      expect(query.aggs).not.toHaveProperty('id');
    });

    describe('buildNameSuggestQuery', () => {
      it('runs an AND bool_prefix match over the search_as_you_type fields', () => {
        const query = buildNameSuggestQuery({ text: 'douce 1', collectionId: 'rose' });
        expect(query.query.bool.must).toEqual([
          {
            multi_match: {
              query: 'douce 1',
              type: 'bool_prefix',
              operator: 'and',
              fields: NAME_SUGGEST_FIELDS,
            },
          },
        ]);
        expect(NAME_SUGGEST_FIELDS).toEqual([
          'label_suggest',
          'label_suggest._2gram',
          'label_suggest._3gram',
        ]);
      });

      it('returns at most 10 small hits by default', () => {
        const query = buildNameSuggestQuery({ text: 'do', collectionId: 'rose' });
        expect(query.size).toBe(10);
        expect(query._source).toEqual(['id', 'label', 'logo']);
        expect(query).not.toHaveProperty('aggs');
        expect(query).not.toHaveProperty('highlight');
      });

      it('filters by collection and other facets but not by the selected names', () => {
        const query = buildNameSuggestQuery({
          text: 'do',
          collectionId: 'dlmm',
          facets: { id: ids, origin: ['France'] },
          categories,
        });
        expect(query.query.bool.filter).toEqual([
          { terms: { collection_id: ['dlmm'] } },
          { terms: { origin: ['France'] } },
        ]);
      });
    });

    describe('buildNameLookupQuery', () => {
      it('fetches label and logo for the given ids', () => {
        expect(buildNameLookupQuery(ids)).toEqual({
          query: { bool: { filter: [{ terms: { id: ids } }] } },
          size: 2,
          _source: ['id', 'label', 'logo'],
        });
      });

      it('caps size and splits the ids within the proxy limits', () => {
        const many = Array.from({ length: MAX_PAGE_SIZE + 1 }, (_, i) => `rose.B${i}`);
        const query = buildNameLookupQuery(many);

        expect(query.size).toBe(MAX_PAGE_SIZE);
        const clauses = query.query.bool.filter[0].bool.should;
        expect(clauses).toHaveLength(2);
        expect(clauses[0].terms.id).toHaveLength(MAX_TERMS_VALUES);
        expect(clauses[1].terms.id).toHaveLength(MAX_PAGE_SIZE + 1 - MAX_TERMS_VALUES);
        expect(query.query.bool.filter[0].bool.minimum_should_match).toBe(1);
      });
    });
  });

  describe('pageWindow', () => {
    it('pages normally inside the result window', () => {
      expect(pageWindow(0, 20)).toEqual({ from: 0, size: 20 });
      expect(pageWindow(3, 30)).toEqual({ from: 90, size: 30 });
    });

    it('never asks past the result window', () => {
      for (const size of [20, 30, 40, 50]) {
        const { from } = pageWindow(100000, size);
        expect(from + size).toBeLessThanOrEqual(MAX_RESULT_WINDOW);
        expect(from % size).toBe(0);
      }
      expect(pageWindow(333, 30)).toEqual({ from: 9960, size: 30 });
      expect(pageWindow(334, 30)).toEqual({ from: 9960, size: 30 });
    });

    it('caps the page size at the proxy limit', () => {
      expect(pageWindow(0, 1000)).toEqual({ from: 0, size: MAX_PAGE_SIZE });
    });

    it('is used by the search queries', () => {
      const query = buildSimpleSearchQuery({
        query: 'rose',
        defaultFields: ['marginalia'],
        fieldDefinitions: mockJHSearchService.fields,
        collectionId: 'rose',
        facets: {},
        page: 400,
        pageSize: 30,
      });
      expect(query.from + query.size).toBeLessThanOrEqual(MAX_RESULT_WINDOW);
    });
  });
});
