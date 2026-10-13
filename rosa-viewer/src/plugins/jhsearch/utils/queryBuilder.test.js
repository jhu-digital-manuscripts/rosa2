/**
 * Tests for queryBuilder utility.
 */

import { describe, it, expect } from 'vitest';
import {
  buildBrowseQuery,
  buildSimpleSearchQuery,
  buildAdvancedSearchQuery,
  buildFacetAggregations,
  buildSortClause,
  rowHasQuery,
} from '../utils/queryBuilder';
import { mockJHSearchService } from '../../../test/mocks';

describe('queryBuilder', () => {
  const fieldDefinitions = mockJHSearchService.fields;
  const categories = mockJHSearchService.categories;
  const defaultFields = mockJHSearchService.default_fields;

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
        multi_match: { query: value, fields: ['mark.*'], type: 'best_fields' },
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
          multi_match: { query: 'rex', fields: ['marginalia.*'], type: 'best_fields' },
        });
        expect(clauseFor('people', 'Livy')).toEqual({
          multi_match: { query: 'Livy', fields: ['people'], type: 'best_fields' },
        });
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
        multi_match: { query: value, fields: [field], type: 'best_fields' },
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
});
