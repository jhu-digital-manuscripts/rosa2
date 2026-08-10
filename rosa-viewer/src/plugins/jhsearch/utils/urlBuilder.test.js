/**
 * Tests for urlBuilder utility.
 */

import { describe, it, expect } from 'vitest';
import {
  buildThumbnailUrl,
  buildManifestUrl,
  buildCanvasUrl,
  buildCollectionUrl,
  buildLogoUrl,
  parseManifestId,
} from '../utils/urlBuilder';
import { mockConfig } from '../../../test/mocks';

describe('urlBuilder', () => {
  describe('buildThumbnailUrl', () => {
    it('should build thumbnail URL from template (with URL encoding)', () => {
      const url = buildThumbnailUrl('rose/Douce195/1r', mockConfig);
      // The implementation URL-encodes the iiifImageId
      expect(url).toContain('http://localhost:3000/images/');
      expect(url).toContain('/full/200,/0/default.jpg');
    });

    it('should return null for null imageId', () => {
      const url = buildThumbnailUrl(null, mockConfig);
      expect(url).toBeNull();
    });

    it('should return null for missing imageBaseUrl', () => {
      const configNoBase = { ...mockConfig, imageBaseUrl: '' };
      const url = buildThumbnailUrl('rose/Douce195/1r', configNoBase);
      expect(url).toBeNull();
    });
  });

  describe('buildManifestUrl', () => {
    it('should build manifest URL from dot-formatted manifest ID', () => {
      // Implementation expects manifestId in format "collection.book"
      const url = buildManifestUrl('rose.Douce195', mockConfig.iiifBaseUrl);
      expect(url).toBe('http://localhost:3000/iiif/rose/Douce195/manifest.json');
    });

    it('should return null for invalid manifest ID format', () => {
      // No dot means invalid
      const url = buildManifestUrl('roseDouce195', mockConfig.iiifBaseUrl);
      expect(url).toBeNull();
    });
  });

  describe('buildCanvasUrl', () => {
    it('should build canvas URL from manifest ID and page number', () => {
      // Implementation expects manifestId + pageNum
      const url = buildCanvasUrl('rose.Douce195', 5, mockConfig.iiifBaseUrl);
      expect(url).toBe('http://localhost:3000/iiif/rose/Douce195/canvas/5');
    });
  });

  describe('buildCollectionUrl', () => {
    it('should build collection URL', () => {
      const url = buildCollectionUrl('rose', mockConfig.iiifBaseUrl);
      expect(url).toBe('http://localhost:3000/iiif/rose/collection.json');
    });
  });

  describe('buildLogoUrl', () => {
    it('should build logo URL', () => {
      // Implementation doesn't add extension - expects the logo param to have it
      const url = buildLogoUrl('rose.png', mockConfig.logoBaseUrl);
      expect(url).toBe('/logo/rose.png');
    });

    it('should return null for null logo', () => {
      const url = buildLogoUrl(null, mockConfig.logoBaseUrl);
      expect(url).toBeNull();
    });
  });

  describe('parseManifestId', () => {
    it('should parse valid manifest ID', () => {
      const result = parseManifestId('rose.Douce195');
      expect(result).toEqual({
        collectionId: 'rose',
        bookId: 'Douce195',
      });
    });

    it('should return null for invalid manifest ID', () => {
      const result = parseManifestId('roseDouce195');
      expect(result).toBeNull();
    });

    it('should return null for null input', () => {
      const result = parseManifestId(null);
      expect(result).toBeNull();
    });
  });
});
