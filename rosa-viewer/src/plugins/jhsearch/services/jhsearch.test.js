/**
 * Tests for jhsearch service.
 */

import { describe, it, expect, vi, beforeEach } from 'vitest';
import {
  extractJHSearchService,
  loadJHSearchConfig,
  loadCollectionMetadata,
} from '../services/jhsearch';
import { mockCollection, mockJHSearchService, createFetchResponse } from '../../../test/mocks';

describe('jhsearch service', () => {
  beforeEach(() => {
    vi.resetAllMocks();
  });

  describe('extractJHSearchService', () => {
    it('should extract JHSearchService2 from collection', () => {
      const service = extractJHSearchService(mockCollection);

      expect(service).toMatchObject({
        id: 'http://localhost:3000/iiif/rose/service/jhsearch.json',
        type: 'JHSearchService2',
      });
    });

    it('should return null if no service found', () => {
      const collectionWithoutService = { ...mockCollection, service: [] };
      const service = extractJHSearchService(collectionWithoutService);
      expect(service).toBeNull();
    });

    it('should handle service as single object', () => {
      const collectionWithSingleService = {
        ...mockCollection,
        service: mockCollection.service[0],
      };
      const service = extractJHSearchService(collectionWithSingleService);
      expect(service).not.toBeNull();
      expect(service.type).toBe('JHSearchService2');
    });
  });

  describe('loadJHSearchConfig', () => {
    it('should fetch and parse JHSearch configuration', async () => {
      const mockResponse = {
        opensearch: 'http://localhost:9200/_search',
        fields: mockJHSearchService.fields,
        categories: mockJHSearchService.categories,
        'default-fields': mockJHSearchService.default_fields,
      };
      global.fetch = vi.fn().mockResolvedValue(createFetchResponse(mockResponse));

      const serviceUrl = 'http://localhost:3000/iiif/rose/service/jhsearch.json';
      const result = await loadJHSearchConfig(serviceUrl);

      expect(global.fetch).toHaveBeenCalledWith(serviceUrl, expect.any(Object));
      expect(result).toMatchObject({
        opensearchUrl: 'http://localhost:9200/_search',
        fields: expect.any(Array),
        categories: expect.any(Array),
      });
    });
  });

  describe('loadCollectionMetadata', () => {
    it('should fetch and parse collection metadata', async () => {
      global.fetch = vi.fn().mockResolvedValue(createFetchResponse(mockCollection));

      const collectionUrl = 'http://localhost:3000/iiif/rose/collection.json';
      const result = await loadCollectionMetadata(collectionUrl);

      expect(global.fetch).toHaveBeenCalledWith(collectionUrl, expect.any(Object));
      expect(result).toMatchObject({
        id: mockCollection.id,
        label: 'Roman de la Rose Digital Library',
      });
      expect(result.service).not.toBeNull();
    });
  });
});
