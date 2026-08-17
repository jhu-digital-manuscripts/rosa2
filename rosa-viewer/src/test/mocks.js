/**
 * Mock data and utilities for tests.
 */

// Sample IIIF collection response
export const mockCollection = {
  '@context': 'http://iiif.io/api/presentation/3/context.json',
  id: 'http://localhost:3000/iiif/rose/collection.json',
  type: 'Collection',
  label: { en: ['Roman de la Rose Digital Library'] },
  items: [
    {
      id: 'http://localhost:3000/iiif/rose/Douce195/manifest.json',
      type: 'Manifest',
      label: { en: ['MS. Douce 195'] },
    },
    {
      id: 'http://localhost:3000/iiif/rose/LudwigXV7/manifest.json',
      type: 'Manifest',
      label: { en: ['MS. Ludwig XV 7'] },
    },
  ],
  service: [
    {
      '@context': 'http://library.jhu.edu/api/jhsearch/2/context.json',
      id: 'http://localhost:3000/iiif/rose/service/jhsearch.json',
      type: 'JHSearchService2',
      profile: 'http://library.jhu.edu/api/jhsearch/2',
    },
  ],
};

// Sample JHSearch service configuration
export const mockJHSearchService = {
  '@context': 'http://library.jhu.edu/api/jhsearch/2/context.json',
  id: 'http://localhost:3000/iiif/rose/service/jhsearch.json',
  type: 'JHSearchService2',
  search_url: 'http://localhost:9200/_search',
  default_fields: ['label', 'description'],
  fields: [
    {
      name: 'label',
      label: 'Title',
      description: 'The title or name of the item',
    },
    {
      name: 'description',
      label: 'Description',
      description: 'A description of the item',
    },
    {
      name: 'text',
      label: 'Text',
      description: 'Full text content',
      has_subfields: true,
    },
    {
      name: 'origin',
      label: 'Origin',
      description: 'Place of origin',
    },
  ],
  categories: [
    {
      name: 'origin',
      label: 'Origin',
      description: 'Place of origin',
      type: 'checkbox',
    },
    {
      name: 'date_range',
      label: 'Date Range',
      description: 'Approximate date range',
      type: 'checkbox',
    },
  ],
};

// Sample Opensearch search response
export const mockSearchResponse = {
  took: 15,
  timed_out: false,
  hits: {
    total: { value: 2, relation: 'eq' },
    max_score: 1.5,
    hits: [
      {
        _index: 'manifest',
        _id: 'rose/Douce195',
        _score: 1.5,
        _source: {
          manifest_id: 'Douce195',
          collection_id: 'rose',
          label: 'MS. Douce 195',
          description: 'A beautiful 15th century manuscript of the Roman de la Rose',
          origin: 'France',
          date_range: '1400-1450',
          thumbnail_id: 'rose/Douce195/1r',
        },
        highlight: {
          description: ['A beautiful 15th century <em>manuscript</em> of the Roman de la Rose'],
        },
      },
      {
        _index: 'manifest',
        _id: 'rose/LudwigXV7',
        _score: 1.2,
        _source: {
          manifest_id: 'LudwigXV7',
          collection_id: 'rose',
          label: 'MS. Ludwig XV 7',
          description: 'Another fine manuscript of the Roman de la Rose',
          origin: 'France',
          date_range: '1350-1400',
          thumbnail_id: 'rose/LudwigXV7/1r',
        },
        highlight: {
          description: ['Another fine <em>manuscript</em> of the Roman de la Rose'],
        },
      },
    ],
  },
  aggregations: {
    origin: {
      buckets: [
        { key: 'France', doc_count: 15 },
        { key: 'Italy', doc_count: 5 },
      ],
    },
    date_range: {
      buckets: [
        { key: '1300-1350', doc_count: 3 },
        { key: '1350-1400', doc_count: 7 },
        { key: '1400-1450', doc_count: 10 },
      ],
    },
  },
};

// Sample canvas search result
export const mockCanvasSearchResponse = {
  took: 8,
  timed_out: false,
  hits: {
    total: { value: 1, relation: 'eq' },
    max_score: 2.1,
    hits: [
      {
        _index: 'canvas',
        _id: 'rose/Douce195/5r',
        _score: 2.1,
        _source: {
          canvas_id: '5r',
          manifest_id: 'Douce195',
          collection_id: 'rose',
          label: 'Folio 5 recto',
          manifest_label: 'MS. Douce 195',
          text: 'Li maus damors ma si grevé que jeo en tremble et fremir.',
          thumbnail_id: 'rose/Douce195/5r',
        },
        highlight: {
          text: ['Li maus <em>damors</em> ma si grevé que jeo en tremble et fremir.'],
        },
      },
    ],
  },
};

// Default plugin config
export const mockConfig = {
  iiifBaseUrl: 'http://localhost:3000/iiif',
  collectionId: 'rose',
  childCollectionIds: [],
  imageBaseUrl: 'http://localhost:3000/images',
  thumbnailTemplate: '{imageBaseUrl}/{iiifImageId}/full/{width},/0/default.jpg',
  thumbnailWidth: 200,
  logoBaseUrl: '/logo',
  enableFacets: true,
  pageSize: 20,
  pageSizeOptions: [10, 20, 50, 100],
};

// Redux state mock
export const mockReduxState = {
  jhsearch: {
    config: {
      ...mockConfig,
      loading: false,
      error: null,
    },
    service: {
      fields: mockJHSearchService.fields,
      categories: mockJHSearchService.categories,
      defaultFields: mockJHSearchService.default_fields,
      opensearchUrl: mockJHSearchService.search_url,
      loading: false,
      error: null,
    },
    collections: {
      items: [
        {
          id: 'rose',
          label: 'Roman de la Rose Digital Library',
          logo: '/logo/rose.png',
          childIds: [],
        },
      ],
      loading: false,
      error: null,
    },
    ui: {
      viewMode: 'browse',
      searchMode: 'simple',
      selectedCollectionId: 'rose',
    },
    facets: {
      selected: {},
      expanded: {},
    },
    simpleSearch: {
      query: '',
    },
    advancedSearch: {
      rows: [{ id: 1, field: '', value: '', operator: 'AND' }],
    },
    sort: {
      field: '_score',
      order: 'desc',
    },
    pagination: {
      page: 1,
      pageSize: 20,
    },
    results: {
      items: [],
      total: 0,
      aggregations: {},
      loading: false,
      error: null,
    },
  },
};

// Helper to create fetch mock response
export function createFetchResponse(data, ok = true) {
  return Promise.resolve({
    ok,
    status: ok ? 200 : 500,
    json: () => Promise.resolve(data),
    text: () => Promise.resolve(JSON.stringify(data)),
  });
}

// Helper to mock Redux store
export function createMockStore(state = mockReduxState) {
  return {
    getState: () => state,
    dispatch: () => {},
    subscribe: () => () => {},
  };
}
