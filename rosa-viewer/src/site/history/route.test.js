import { describe, it, expect } from 'vitest';
import {
  createRouteContext,
  formatRoute,
  parseLocation,
  parseParams,
  resolveSort,
  stripLegacyQuery,
} from './route';

const ctx = createRouteContext({
  collectionId: 'dlmm',
  childCollectionIds: ['rose', 'pizan'],
  pageSize: 20,
  pageSizeOptions: [20, 30, 40, 50],
});

const parse = (hash, search = '') => parseLocation({ hash, search }, ctx);
const titleSort = { field: 'label.keyword', order: 'asc' };
const relevance = { field: '_score', order: 'desc' };

describe('parseLocation', () => {
  it('reads an empty fragment as browsing the site collection', () => {
    expect(parse('')).toEqual({
      type: 'browse',
      collectionId: 'dlmm',
      facets: {},
      sort: titleSort,
      page: 0,
      pageSize: 20,
    });
  });

  it('reads a collection', () => {
    expect(parse('#rose')).toMatchObject({ type: 'browse', collectionId: 'rose' });
    expect(parse('#rose/')).toMatchObject({ type: 'browse', collectionId: 'rose' });
  });

  it('falls back to the site collection for a collection the site does not show', () => {
    expect(parse('#aor/FolgersHa2/thumb')).toMatchObject({ type: 'browse', collectionId: 'dlmm' });
  });

  it('reads categories, sort and paging when browsing', () => {
    expect(parse('#rose?f.date=1350&f.date=1400&f.type=Prose+text&s=title_desc&o=40&m=20')).toEqual(
      {
        type: 'browse',
        collectionId: 'rose',
        facets: { date: [1350, 1400], type: ['Prose text'] },
        sort: { field: 'label.keyword', order: 'desc' },
        page: 2,
        pageSize: 20,
      }
    );
  });

  it('keeps non-numeric category values as strings', () => {
    expect(parse('#rose?f.current_location=Paris%2C+BnF&f.date=007').facets).toEqual({
      current_location: ['Paris, BnF'],
      date: ['007'],
    });
    expect(parse('#rose?f.has_transcription=true').facets).toEqual({ has_transcription: [true] });
  });

  it('reads Name filter selections as manifest id strings', () => {
    expect(parse('#rose?f.id=rose.Douce195&f.id=rose.Arras845').facets).toEqual({
      id: ['rose.Douce195', 'rose.Arras845'],
    });
  });

  it('reads a simple search', () => {
    expect(parse('#rose/search?q=green&s=page&m=30&o=60')).toEqual({
      type: 'search',
      collectionId: 'rose',
      mode: 'simple',
      query: 'green',
      facets: {},
      sort: { field: 'page_num', order: 'asc' },
      page: 2,
      pageSize: 30,
    });
  });

  it('reads an advanced search', () => {
    expect(parse("#rose/search?q=(description:'sword'+%26+title:'x')&type=advanced")).toMatchObject(
      {
        type: 'search',
        mode: 'advanced',
        rows: [
          { field: 'description', value: 'sword', operator: 'AND' },
          { field: 'title', value: 'x', operator: 'AND' },
        ],
        sort: relevance,
      }
    );
  });

  it('treats an advanced query it cannot parse as a simple search', () => {
    expect(parse('#rose/search?q=green&type=advanced')).toMatchObject({
      mode: 'simple',
      query: 'green',
    });
  });

  it('treats a search without a query as browsing', () => {
    expect(parse('#rose/search')).toMatchObject({ type: 'browse', collectionId: 'rose' });
  });

  it('ignores a sort not offered in the mode and a page size not offered', () => {
    expect(parse('#rose/search?q=x&s=title&m=25')).toMatchObject({ sort: relevance, pageSize: 20 });
    expect(parse('#rose?s=relevance')).toMatchObject({ sort: titleSort });
  });

  describe('old bookmarks', () => {
    it('reads a simple search with the parameters before the fragment', () => {
      expect(
        parse('#rose/search', '?q=green&m=30&s=relevance&type=basic&service=rose')
      ).toMatchObject({
        type: 'search',
        collectionId: 'rose',
        mode: 'simple',
        query: 'green',
        pageSize: 30,
      });
    });

    it('reads an advanced search with the parameters before the fragment', () => {
      expect(
        parse(
          '#rose/search',
          '?q=description%3A%27sword%27&m=30&s=relevance&type=advanced&service=rose'
        )
      ).toMatchObject({
        type: 'search',
        mode: 'advanced',
        rows: [{ field: 'description', value: 'sword', operator: 'AND' }],
      });
    });

    it('ignores other query parameters, such as ones WordPress uses', () => {
      expect(parse('#rose', '?page_id=7')).toMatchObject({ type: 'browse', collectionId: 'rose' });
    });

    it('runs a search within a book as a search of the collection', () => {
      expect(
        parse('#rose/Arras845/015r/image/search', '?q=green&type=basic&service=rose/Arras845')
      ).toMatchObject({ type: 'search', collectionId: 'rose', query: 'green' });
    });

    it('reads the book views', () => {
      expect(parse('#rose/Add12042/001v/image')).toEqual({
        type: 'book',
        collectionId: 'rose',
        bookId: 'Add12042',
        view: 'single',
        canvas: '001v',
      });
      expect(parse('#rose/Arras845/015r/opening')).toMatchObject({ view: 'book', canvas: '015r' });
      expect(parse('#rose/Arras845/thumb')).toMatchObject({ view: 'gallery', canvas: null });
      expect(parse('#rose/Arras845/scroll')).toMatchObject({ view: 'scroll', canvas: null });
    });
  });

  it('reads a book without a view or page', () => {
    expect(parse('#rose/Arras845')).toMatchObject({ type: 'book', view: null, canvas: null });
    expect(parse('#rose/Arras845/015r')).toMatchObject({ view: null, canvas: '015r' });
    expect(parse('#rose/Arras845/image')).toMatchObject({ view: 'single', canvas: null });
  });
});

describe('formatRoute', () => {
  const browse = {
    type: 'browse',
    collectionId: 'rose',
    facets: {},
    sort: titleSort,
    page: 0,
    pageSize: 20,
  };

  it('leaves out default parameters', () => {
    expect(formatRoute(browse, ctx)).toBe('rose');
  });

  it('formats categories, sort and paging', () => {
    expect(
      formatRoute(
        {
          ...browse,
          facets: { current_location: ['Paris, BnF'], date: [1350, 1400] },
          sort: { field: 'label.keyword', order: 'desc' },
          page: 2,
          pageSize: 30,
        },
        ctx
      )
    ).toBe('rose?f.current_location=Paris,+BnF&f.date=1350&f.date=1400&s=title_desc&o=60&m=30');
  });

  it('formats a simple search', () => {
    expect(
      formatRoute(
        { ...browse, type: 'search', mode: 'simple', query: ' green man ', sort: relevance },
        ctx
      )
    ).toBe('rose/search?q=green+man');
  });

  it('formats an advanced search', () => {
    expect(
      formatRoute(
        {
          ...browse,
          type: 'search',
          mode: 'advanced',
          rows: [
            { field: 'description', value: 'sword', operator: 'AND' },
            { field: 'title', value: 'x', operator: 'OR' },
          ],
          sort: { field: 'page_num', order: 'asc' },
        },
        ctx
      )
    ).toBe("rose/search?q=(description:'sword'+%7C+title:'x')&type=advanced&s=page");
  });

  it('round-trips extra text on an advanced search row', () => {
    const rows = [{ field: 'mark', value: 'dash', text: 'regnum rex', operator: 'AND' }];
    const url = formatRoute({ ...browse, type: 'search', mode: 'advanced', rows }, ctx);
    expect(url).toBe("rose/search?q=mark:'dash'%2B'regnum+rex'&type=advanced");
    expect(parse(`#${url}`)).toMatchObject({ mode: 'advanced', rows });
  });

  it('formats a search without a query as browsing', () => {
    expect(formatRoute({ ...browse, type: 'search', mode: 'simple', query: '' }, ctx)).toBe('rose');
  });

  it('formats book views', () => {
    const book = { type: 'book', collectionId: 'rose', bookId: 'Arras845' };
    expect(formatRoute({ ...book, view: 'single', canvas: '001v' }, ctx)).toBe(
      'rose/Arras845/001v/image'
    );
    expect(formatRoute({ ...book, view: 'book', canvas: '015r' }, ctx)).toBe(
      'rose/Arras845/015r/opening'
    );
    expect(formatRoute({ ...book, view: 'gallery', canvas: '015r' }, ctx)).toBe(
      'rose/Arras845/thumb'
    );
    expect(formatRoute({ ...book, view: 'scroll' }, ctx)).toBe('rose/Arras845/scroll');
  });

  it('round-trips through parseLocation', () => {
    const hashes = [
      'rose',
      'rose?f.date=1350&f.type=Prose+text&s=title_desc&o=40',
      'rose?f.id=rose.Douce195&f.id=rose.Arras845',
      'pizan/search?q=green+man&f.date=1400&s=page&m=40',
      "rose/search?q=(description:'sword'+%26+title:'l%5C'amant')&type=advanced",
      'rose/Add12042/binding.frontcover/image',
      'rose/Arras845/015r/opening',
      'rose/Arras845/thumb',
    ];
    for (const hash of hashes) {
      expect(formatRoute(parse(`#${hash}`), ctx)).toBe(hash);
    }
  });
});

describe('helpers', () => {
  it('parses parameters', () => {
    expect(parseParams('a=1&b=x+y&b=%26&c')).toEqual([
      ['a', '1'],
      ['b', 'x y'],
      ['b', '&'],
      ['c', ''],
    ]);
  });

  it('resolves sorts per mode', () => {
    expect(resolveSort('browse', 'title_desc')).toEqual({ field: 'label.keyword', order: 'desc' });
    expect(resolveSort('browse', 'page')).toEqual(titleSort);
    expect(resolveSort('search', undefined)).toEqual(relevance);
  });

  it('strips the old search parameters from the query string', () => {
    expect(stripLegacyQuery('?q=green&m=30&s=relevance&type=basic&service=rose')).toBe('');
    expect(stripLegacyQuery('?page_id=7&q=green')).toBe('?page_id=7');
    expect(stripLegacyQuery('')).toBe('');
  });
});
