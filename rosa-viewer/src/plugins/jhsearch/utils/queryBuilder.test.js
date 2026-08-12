/**
 * Tests for queryBuilder utility.
 */

import { describe, it, expect } from 'vitest';
import {
  buildBrowseQuery,
  buildSimpleSearchQuery,
  buildAdvancedSearchQuery,
  buildFacetAggregations,
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