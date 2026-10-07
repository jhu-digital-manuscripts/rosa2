/**
 * Viewer URLs.
 *
 * The state of the viewer is kept in the URL fragment so it can be bookmarked and
 * so the browser's back and forward buttons work. Everything, including search
 * parameters, is inside the fragment: the sites are served from WordPress, which
 * would treat a query string such as `?s=relevance&m=30` as its own search and
 * archive parameters.
 *
 *   #rose                                      browse a collection
 *   #rose?f.date=1350&f.type=Prose             browse by category
 *   #rose/search?q=green                       simple search
 *   #rose/search?type=advanced&q=(description:'sword' & title:'x')
 *   #rose/Arras845/thumb                       thumbnail gallery of a book
 *   #rose/Arras845/scroll                      scroll view of a book
 *   #rose/Add12042/001v/image                  single image
 *   #rose/Arras845/015r/opening                reading (two-page) view
 *
 * Browse and search URLs also take `s` (sort), `o` (offset of the first result)
 * and `m` (results per page). Parameters with default values are left out.
 *
 * The paths are the ones the previous AoR and DLMM viewers used. Their search
 * URLs put the parameters before the fragment
 * (`?q=green&m=30&s=relevance&type=basic&service=rose#rose/search`); those are
 * still understood, as are their search-within-a-book URLs
 * (`#rose/Arras845/015r/image/search?...`), which now run the query across the
 * collection.
 */

import { parseAdvancedQuery, formatAdvancedQuery } from './advancedQuery';

/** Mirador view type -> URL token */
export const VIEW_TOKENS = {
  single: 'image',
  book: 'opening',
  gallery: 'thumb',
  scroll: 'scroll',
};

const TOKEN_VIEWS = Object.fromEntries(Object.entries(VIEW_TOKENS).map(([v, t]) => [t, v]));

/** Views whose URL names the current page. */
const VIEWS_WITH_CANVAS = new Set(['single', 'book']);

/** URL sort token -> sort, and the modes the sort is offered in (see SortOrder.jsx). */
export const SORTS = {
  relevance: { field: '_score', order: 'desc', modes: ['search'] },
  page: { field: 'page_num', order: 'asc', modes: ['search'] },
  title: { field: 'label.keyword', order: 'asc', modes: ['browse'] },
  title_desc: { field: 'label.keyword', order: 'desc', modes: ['browse'] },
};

const DEFAULT_SORT = { browse: 'title', search: 'relevance' };

/** Parameters the previous viewers put in the query string. */
export const LEGACY_QUERY_PARAMS = ['q', 'o', 'm', 's', 'type', 'service'];

const FACET_PREFIX = 'f.';

/**
 * @typedef {Object} RouteContext
 * @property {string} collectionId - Site's top-level collection
 * @property {string[]} collectionIds - Collections the site shows
 * @property {number} pageSize - Default results per page
 * @property {number[]} pageSizeOptions - Allowed results per page
 */

/**
 * Build the route context from the jhsearch config.
 * @param {Object} jhsearch - config.jhsearch
 * @returns {RouteContext}
 */
export function createRouteContext(jhsearch = {}) {
  return {
    collectionId: jhsearch.collectionId,
    collectionIds: [jhsearch.collectionId, ...(jhsearch.childCollectionIds || [])],
    pageSize: jhsearch.pageSize || 20,
    pageSizeOptions: jhsearch.pageSizeOptions || [20, 30, 40, 50],
  };
}

/**
 * The sort token for a sort field and order, or null when it has none.
 */
export function sortToken(field, order) {
  const entry = Object.entries(SORTS).find(([, s]) => s.field === field && s.order === order);
  return entry ? entry[0] : null;
}

/**
 * The sort to use for a mode given a URL sort token, falling back to the mode's default.
 * @param {string} mode - 'browse' | 'search'
 * @param {string} [token]
 * @returns {{field: string, order: string}}
 */
export function resolveSort(mode, token) {
  const sort = SORTS[token]?.modes.includes(mode) ? SORTS[token] : SORTS[DEFAULT_SORT[mode]];
  return { field: sort.field, order: sort.order };
}

// ----------------------------------------------------------------------------
// Parameter encoding
// ----------------------------------------------------------------------------

/**
 * Encode a parameter name or value. Characters that are legal in a fragment and
 * common in queries are left readable.
 */
function encodeParam(value) {
  return encodeURIComponent(String(value))
    .replace(/%20/g, '+')
    .replace(/%3A/gi, ':')
    .replace(/%2C/gi, ',')
    .replace(/%2F/gi, '/');
}

function decodeParam(value) {
  try {
    return decodeURIComponent(value.replace(/\+/g, ' '));
  } catch {
    return value;
  }
}

/**
 * Parse `a=1&b=2&b=3` into [[name, value], ...].
 */
export function parseParams(text) {
  if (!text) return [];
  return text
    .split('&')
    .filter(Boolean)
    .map((pair) => {
      const eq = pair.indexOf('=');
      return eq === -1
        ? [decodeParam(pair), '']
        : [decodeParam(pair.slice(0, eq)), decodeParam(pair.slice(eq + 1))];
    });
}

function formatParams(pairs) {
  return pairs.map(([name, value]) => `${encodeParam(name)}=${encodeParam(value)}`).join('&');
}

function getParam(pairs, name) {
  const pair = pairs.find(([n]) => n === name);
  return pair ? pair[1] : undefined;
}

/**
 * Facet values in URLs are strings. Bucket keys for numeric and boolean fields
 * are numbers and booleans, so those are restored with their types.
 */
function parseFacetValue(text) {
  if (text === 'true') return true;
  if (text === 'false') return false;
  const n = Number(text);
  return text.trim() !== '' && Number.isFinite(n) && String(n) === text ? n : text;
}

function parseFacets(pairs) {
  const facets = {};
  for (const [name, value] of pairs) {
    if (name.startsWith(FACET_PREFIX) && name.length > FACET_PREFIX.length && value !== '') {
      const category = name.slice(FACET_PREFIX.length);
      const parsed = parseFacetValue(value);
      const values = facets[category] || (facets[category] = []);
      if (!values.some((v) => String(v) === String(parsed))) {
        values.push(parsed);
      }
    }
  }
  return facets;
}

function parseInteger(text) {
  const n = Number(text);
  return Number.isInteger(n) && n >= 0 ? n : null;
}

// ----------------------------------------------------------------------------
// Parsing
// ----------------------------------------------------------------------------

function decodeSegment(segment) {
  try {
    return decodeURIComponent(segment);
  } catch {
    return segment;
  }
}

/**
 * Browse/search settings shared by both route types.
 */
function parseListing(pairs, mode, ctx) {
  const m = parseInteger(getParam(pairs, 'm'));
  const pageSize = m && ctx.pageSizeOptions.includes(m) ? m : ctx.pageSize;
  const offset = parseInteger(getParam(pairs, 'o')) || 0;

  return {
    facets: parseFacets(pairs),
    sort: resolveSort(mode, getParam(pairs, 's')),
    page: Math.floor(offset / pageSize),
    pageSize,
  };
}

function browseRoute(collectionId, pairs, ctx) {
  return { type: 'browse', collectionId, ...parseListing(pairs, 'browse', ctx) };
}

function searchRoute(collectionId, pairs, ctx) {
  const q = (getParam(pairs, 'q') || '').trim();
  if (!q) {
    return browseRoute(collectionId, pairs, ctx);
  }

  const listing = parseListing(pairs, 'search', ctx);
  if (getParam(pairs, 'type') === 'advanced') {
    const rows = parseAdvancedQuery(q);
    if (rows) {
      return { type: 'search', collectionId, mode: 'advanced', rows, ...listing };
    }
  }
  return { type: 'search', collectionId, mode: 'simple', query: q, ...listing };
}

function bookRoute(collectionId, bookId, rest) {
  const last = rest[rest.length - 1];
  let view = null;
  let canvas = null;

  if (last && TOKEN_VIEWS[last]) {
    view = TOKEN_VIEWS[last];
    canvas = rest.length > 1 ? rest[0] : null;
  } else if (rest.length > 0) {
    canvas = rest[0];
  }

  return {
    type: 'book',
    collectionId,
    bookId,
    view,
    canvas: view && !VIEWS_WITH_CANVAS.has(view) ? null : canvas,
  };
}

/**
 * Parse a location into a route.
 *
 * @param {{hash: string, search?: string}} location - window.location or similar
 * @param {RouteContext} ctx
 * @returns {Object} Route: {type: 'browse'|'search'|'book', collectionId, ...}
 */
export function parseLocation(location, ctx) {
  const fragment = (location.hash || '').replace(/^#/, '');
  const qmark = fragment.indexOf('?');
  const path = qmark === -1 ? fragment : fragment.slice(0, qmark);

  let pairs = parseParams(qmark === -1 ? '' : fragment.slice(qmark + 1));
  if (pairs.length === 0 && location.search) {
    pairs = parseParams(location.search.replace(/^\?/, '')).filter(([name]) =>
      LEGACY_QUERY_PARAMS.includes(name)
    );
  }

  const parts = path.split('/').filter(Boolean).map(decodeSegment);
  const collectionId = parts[0];

  if (!collectionId || !ctx.collectionIds.includes(collectionId)) {
    return browseRoute(ctx.collectionId, parts.length > 0 ? [] : pairs, ctx);
  }

  if (parts[parts.length - 1] === 'search') {
    return searchRoute(collectionId, pairs, ctx);
  }

  if (parts.length === 1) {
    return browseRoute(collectionId, pairs, ctx);
  }

  return bookRoute(collectionId, parts[1], parts.slice(2));
}

// ----------------------------------------------------------------------------
// Formatting
// ----------------------------------------------------------------------------

function listingParams(route, mode, ctx) {
  const pairs = [];

  for (const [category, values] of Object.entries(route.facets || {})) {
    for (const value of values) {
      pairs.push([`${FACET_PREFIX}${category}`, value]);
    }
  }

  const token = sortToken(route.sort?.field, route.sort?.order);
  if (token && token !== DEFAULT_SORT[mode] && SORTS[token].modes.includes(mode)) {
    pairs.push(['s', token]);
  }

  const pageSize = route.pageSize || ctx.pageSize;
  if (route.page > 0) {
    pairs.push(['o', route.page * pageSize]);
  }
  if (pageSize !== ctx.pageSize) {
    pairs.push(['m', pageSize]);
  }

  return pairs;
}

function withParams(path, pairs) {
  return pairs.length > 0 ? `${path}?${formatParams(pairs)}` : path;
}

/**
 * Format a route as a URL fragment (without the leading '#').
 *
 * @param {Object} route - See parseLocation
 * @param {RouteContext} ctx
 * @returns {string}
 */
export function formatRoute(route, ctx) {
  const collection = encodeURIComponent(route.collectionId);

  if (route.type === 'book') {
    const parts = [collection, encodeURIComponent(route.bookId)];
    const view = route.view || 'single';
    if (VIEWS_WITH_CANVAS.has(view) && route.canvas) {
      parts.push(encodeURIComponent(route.canvas));
    }
    parts.push(VIEW_TOKENS[view] || VIEW_TOKENS.single);
    return parts.join('/');
  }

  if (route.type === 'search') {
    const q = route.mode === 'advanced' ? formatAdvancedQuery(route.rows) : route.query;
    if (q && q.trim()) {
      const pairs = [['q', q.trim()]];
      if (route.mode === 'advanced') {
        pairs.push(['type', 'advanced']);
      }
      pairs.push(...listingParams(route, 'search', ctx));
      return withParams(`${collection}/search`, pairs);
    }
  }

  return withParams(collection, listingParams(route, 'browse', ctx));
}

/**
 * The query string with the previous viewers' search parameters removed, so a
 * page URL such as `/viewer/?page_id=7` keeps any parameters WordPress needs.
 *
 * @param {string} search - location.search
 * @returns {string} '' or '?...'
 */
export function stripLegacyQuery(search) {
  if (!search || search === '?') return '';
  const kept = search
    .replace(/^\?/, '')
    .split('&')
    .filter((pair) => pair && !LEGACY_QUERY_PARAMS.includes(decodeParam(pair.split('=')[0])));
  return kept.length > 0 ? `?${kept.join('&')}` : '';
}
