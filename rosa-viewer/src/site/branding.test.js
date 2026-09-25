import { describe, it, expect, beforeEach } from 'vitest';
import { applySiteBranding } from './branding';

describe('applySiteBranding', () => {
  beforeEach(() => {
    document.title = '';
    document.documentElement.style.removeProperty('--rosa-banner-image');
    document.documentElement.classList.remove('rosa-has-banner');
  });

  it('sets the document title and banner image variable', () => {
    applySiteBranding(
      {
        title: 'The Archaeology of Reading',
        banner: { imageId: 'aor%2FX%2FX.001r', region: '0,0,10,10', width: 100 },
      },
      { jhsearch: { imageBaseUrl: 'https://image.example.org/iiif' } }
    );

    expect(document.title).toBe('The Archaeology of Reading');
    expect(document.documentElement.style.getPropertyValue('--rosa-banner-image')).toBe(
      'url("https://image.example.org/iiif/aor%2FX%2FX.001r/0,0,10,10/100,/0/default.jpg")'
    );
    expect(document.documentElement.classList.contains('rosa-has-banner')).toBe(true);
  });

  it('clears the banner when the site has none', () => {
    document.documentElement.style.setProperty('--rosa-banner-image', 'url("old")');
    document.documentElement.classList.add('rosa-has-banner');

    applySiteBranding({ title: 'DLMM' }, { jhsearch: {} });

    expect(document.documentElement.style.getPropertyValue('--rosa-banner-image')).toBe('');
    expect(document.documentElement.classList.contains('rosa-has-banner')).toBe(false);
  });
});
