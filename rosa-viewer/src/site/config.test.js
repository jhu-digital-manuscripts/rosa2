import { describe, it, expect } from 'vitest';
import {
  buildBannerUrl,
  defaultLogoBaseUrl,
  resolveSiteConfig,
  stripTrailingSlash,
  validateSiteEnv,
} from './config';

const site = {
  id: 'dlmm',
  title: 'Digital Library of Medieval Manuscripts',
  jhsearch: { collectionId: 'dlmm', childCollectionIds: ['rose', 'pizan'] },
  theme: { palette: { primary: { main: '#9b2c3a' } } },
};

describe('stripTrailingSlash', () => {
  it('removes trailing slashes only', () => {
    expect(stripTrailingSlash('https://x.org/iiif/')).toBe('https://x.org/iiif');
    expect(stripTrailingSlash('https://x.org/iiif//')).toBe('https://x.org/iiif');
    expect(stripTrailingSlash('https://x.org/iiif')).toBe('https://x.org/iiif');
    expect(stripTrailingSlash(undefined)).toBeUndefined();
  });
});

describe('defaultLogoBaseUrl', () => {
  it('appends logo to the Vite base URL', () => {
    expect(defaultLogoBaseUrl('/')).toBe('/logo');
    expect(defaultLogoBaseUrl('./')).toBe('./logo');
    expect(defaultLogoBaseUrl('/rosa2/aor')).toBe('/rosa2/aor/logo');
    expect(defaultLogoBaseUrl('')).toBe('/logo');
  });
});

describe('validateSiteEnv', () => {
  it('requires iiifBaseUrl', () => {
    expect(validateSiteEnv({})).toEqual(['iiifBaseUrl is not set']);
    expect(validateSiteEnv({ iiifBaseUrl: 'http://localhost:3000/iiif' })).toEqual([]);
  });

  it('treats an unreplaced Vite placeholder as unset', () => {
    expect(validateSiteEnv({ iiifBaseUrl: '%VITE_ROSA_IIIF_BASE_URL%' })).toEqual([
      'iiifBaseUrl is not set',
    ]);
  });
});

describe('resolveSiteConfig', () => {
  const env = {
    iiifBaseUrl: 'https://example.org/viewer/iiif/',
    imageBaseUrl: 'https://image.example.org/iiif/',
  };

  it('merges the environment URLs into the jhsearch config', () => {
    const config = resolveSiteConfig(site, env);
    expect(config.jhsearch).toEqual({
      iiifBaseUrl: 'https://example.org/viewer/iiif',
      imageBaseUrl: 'https://image.example.org/iiif',
      logoBaseUrl: '/logo',
      collectionId: 'dlmm',
      childCollectionIds: ['rose', 'pizan'],
    });
  });

  it('does not let the environment override the site identity', () => {
    const config = resolveSiteConfig(
      { id: 'aor', jhsearch: { collectionId: 'aor', childCollectionIds: [], enableFacets: false } },
      { ...env, collectionId: 'dlmm', enableFacets: true }
    );
    expect(config.jhsearch.collectionId).toBe('aor');
    expect(config.jhsearch.childCollectionIds).toEqual([]);
    expect(config.jhsearch.enableFacets).toBe(false);
  });

  it('passes theme and extra Mirador settings through', () => {
    const config = resolveSiteConfig({ ...site, mirador: { workspace: { type: 'elastic' } } }, env);
    expect(config.id).toBe('rosa-viewer');
    expect(config.selectedTheme).toBe('light');
    expect(config.theme).toEqual(site.theme);
    expect(config.workspace).toEqual({ type: 'elastic' });
  });

  it('uses an explicit logoBaseUrl and omits imageBaseUrl when not given', () => {
    const config = resolveSiteConfig(site, {
      iiifBaseUrl: 'http://localhost:3000/iiif',
      logoBaseUrl: 'https://cdn.example.org/logos/',
    });
    expect(config.jhsearch.logoBaseUrl).toBe('https://cdn.example.org/logos');
    expect(config.jhsearch).not.toHaveProperty('imageBaseUrl');
  });

  it('rejects incomplete site definitions', () => {
    expect(() => resolveSiteConfig({ id: 'x' }, env)).toThrow(/site definition/);
    expect(() => resolveSiteConfig(null, env)).toThrow(/site definition/);
  });
});

describe('buildBannerUrl', () => {
  it('builds an IIIF Image API URL for the banner region', () => {
    expect(
      buildBannerUrl(
        { imageId: 'aor%2FFolgersHa2%2FFolgersHa2.012r', region: '300,90,2650,260', width: 1800 },
        'https://image.example.org/iiif/'
      )
    ).toBe(
      'https://image.example.org/iiif/aor%2FFolgersHa2%2FFolgersHa2.012r/300,90,2650,260/1800,/0/default.jpg'
    );
  });

  it('defaults region and width', () => {
    expect(buildBannerUrl({ imageId: 'x' }, 'https://i.org')).toBe(
      'https://i.org/x/full/1600,/0/default.jpg'
    );
  });

  it('returns null without a banner or image base URL', () => {
    expect(buildBannerUrl(undefined, 'https://i.org')).toBeNull();
    expect(buildBannerUrl({ imageId: 'x' }, undefined)).toBeNull();
    expect(buildBannerUrl({}, 'https://i.org')).toBeNull();
  });
});
