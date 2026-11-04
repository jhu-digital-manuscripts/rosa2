import { describe, it, expect } from 'vitest';
import { siteRoute } from './siteLinks';

const jhsearch = { collectionId: 'aor', childCollectionIds: [] };
const page = (href) => new URL(href);
const plutarch = "aor/search?q=people:'Plutarch'&type=advanced";
const row = { field: 'people', value: 'Plutarch', operator: 'AND' };

describe('siteRoute', () => {
  it('follows fragment-only search links', () => {
    const route = siteRoute(`#${plutarch}`, jhsearch, page('https://x.org/aor/'));
    expect(route).toMatchObject({
      type: 'search',
      mode: 'advanced',
      collectionId: 'aor',
      rows: [row],
    });
  });

  it('follows absolute links to the page the viewer is on, ignoring the query string', () => {
    const href = `https://x.org/viewer/#${plutarch}`;
    expect(siteRoute(href, jhsearch, page('https://x.org/viewer/?page_id=7'))).toMatchObject({
      type: 'search',
      rows: [row],
    });
    // Trailing slash and index.html are the same page
    expect(siteRoute(href, jhsearch, page('https://x.org/viewer'))).not.toBeNull();
    expect(siteRoute(href, jhsearch, page('https://x.org/viewer/index.html'))).not.toBeNull();
  });

  it('decodes the value the way the link was written', () => {
    const href = `#aor/search?q=people:'O%5C'Neill+%26+Dionysius+of+Halicarnassus'&type=advanced`;
    const route = siteRoute(href, jhsearch, page('https://x.org/aor/'));
    expect(route.rows).toEqual([
      { field: 'people', value: "O'Neill & Dionysius of Halicarnassus", operator: 'AND' },
    ]);
  });

  it('keeps the double quotes rosa-tool wraps a name in, so it searches as a phrase', () => {
    const href = `#aor/search?q=people:'%22Julius+Caesar%22'&type=advanced`;
    const route = siteRoute(href, jhsearch, page('https://x.org/aor/'));
    expect(route.rows).toEqual([{ field: 'people', value: '"Julius Caesar"', operator: 'AND' }]);
  });

  it('ignores links to other origins and other pages', () => {
    const here = page('https://x.org/aor/');
    expect(siteRoute(`https://other.org/aor/#${plutarch}`, jhsearch, here)).toBeNull();
    expect(siteRoute(`https://x.org/dlmm/#${plutarch}`, jhsearch, here)).toBeNull();
    expect(siteRoute('https://example.org/', jhsearch, here)).toBeNull();
    expect(siteRoute('mailto:a@b.c', jhsearch, here)).toBeNull();
  });

  it('follows links to a page of a book', () => {
    const here = page('https://x.org/aor/');
    expect(siteRoute('#aor/RCPD1266/037v/image', jhsearch, here)).toEqual({
      type: 'book',
      collectionId: 'aor',
      bookId: 'RCPD1266',
      canvas: '037v',
      view: 'single',
    });
    expect(
      siteRoute(
        'https://x.org/aor/#aor/RCPD1266/037v/image',
        jhsearch,
        page('https://x.org/aor/?page_id=7')
      )
    ).toMatchObject({
      type: 'book',
      bookId: 'RCPD1266',
      canvas: '037v',
    });
    expect(siteRoute('#aor/RCPD1266/thumb', jhsearch, here)).toMatchObject({
      type: 'book',
      view: 'gallery',
      canvas: null,
    });
    expect(siteRoute('https://other.org/aor/#aor/RCPD1266/037v/image', jhsearch, here)).toBeNull();
  });

  it('ignores links that are neither a search nor a book', () => {
    const here = page('https://x.org/aor/');
    expect(siteRoute('#aor', jhsearch, here)).toBeNull();
    expect(siteRoute('#aor?f.id=aor.RCPD1266', jhsearch, here)).toBeNull();
    expect(siteRoute('#rose/Douce195/001r/image', jhsearch, here)).toBeNull();
    expect(siteRoute("#rose/search?q=people:'x'&type=advanced", jhsearch, here)).toBeNull();
    expect(siteRoute('#', jhsearch, here)).toBeNull();
    expect(siteRoute('', jhsearch, here)).toBeNull();
    expect(siteRoute(`#${plutarch}`, null, here)).toBeNull();
  });
});
