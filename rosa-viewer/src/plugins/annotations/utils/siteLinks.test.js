import { describe, it, expect } from 'vitest';
import { siteSearchRoute } from './siteLinks';

const jhsearch = { collectionId: 'aor', childCollectionIds: [] };
const page = (href) => new URL(href);
const plutarch = "aor/search?q=people:'Plutarch'&type=advanced";
const row = { field: 'people', value: 'Plutarch', operator: 'AND' };

describe('siteSearchRoute', () => {
  it('follows fragment-only search links', () => {
    const route = siteSearchRoute(`#${plutarch}`, jhsearch, page('https://x.org/aor/'));
    expect(route).toMatchObject({
      type: 'search',
      mode: 'advanced',
      collectionId: 'aor',
      rows: [row],
    });
  });

  it('follows absolute links to the page the viewer is on, ignoring the query string', () => {
    const href = `https://x.org/viewer/#${plutarch}`;
    expect(siteSearchRoute(href, jhsearch, page('https://x.org/viewer/?page_id=7'))).toMatchObject({
      type: 'search',
      rows: [row],
    });
    // Trailing slash and index.html are the same page
    expect(siteSearchRoute(href, jhsearch, page('https://x.org/viewer'))).not.toBeNull();
    expect(siteSearchRoute(href, jhsearch, page('https://x.org/viewer/index.html'))).not.toBeNull();
  });

  it('decodes the value the way the link was written', () => {
    const href = `#aor/search?q=people:'O%5C'Neill+%26+Dionysius+of+Halicarnassus'&type=advanced`;
    const route = siteSearchRoute(href, jhsearch, page('https://x.org/aor/'));
    expect(route.rows).toEqual([
      { field: 'people', value: "O'Neill & Dionysius of Halicarnassus", operator: 'AND' },
    ]);
  });

  it('ignores links to other origins and other pages', () => {
    const here = page('https://x.org/aor/');
    expect(siteSearchRoute(`https://other.org/aor/#${plutarch}`, jhsearch, here)).toBeNull();
    expect(siteSearchRoute(`https://x.org/dlmm/#${plutarch}`, jhsearch, here)).toBeNull();
    expect(siteSearchRoute('https://example.org/', jhsearch, here)).toBeNull();
    expect(siteSearchRoute('mailto:a@b.c', jhsearch, here)).toBeNull();
  });

  it('ignores links that are not a search', () => {
    const here = page('https://x.org/aor/');
    expect(siteSearchRoute('#aor/RCPD1266/150r/image', jhsearch, here)).toBeNull();
    expect(siteSearchRoute('#aor', jhsearch, here)).toBeNull();
    expect(siteSearchRoute("#rose/search?q=people:'x'&type=advanced", jhsearch, here)).toBeNull();
    expect(siteSearchRoute('#', jhsearch, here)).toBeNull();
    expect(siteSearchRoute('', jhsearch, here)).toBeNull();
    expect(siteSearchRoute(`#${plutarch}`, null, here)).toBeNull();
  });
});
