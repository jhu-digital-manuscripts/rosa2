/**
 * The sort orders of the browse and search views.
 *
 * Browsing lists books, which have a title but no relevance or page order.
 * Searching matches books and pages, ordered by relevance or by page.
 */

/**
 * Sort key -> sort. The key is also the `s` parameter of viewer URLs (see
 * site/history/route.js). The sorts of a mode are offered in this order, and the
 * first one is its default.
 */
export const SORTS = {
  relevance: { field: '_score', order: 'desc', label: 'Relevance', modes: ['search'] },
  page: { field: 'page_num', order: 'asc', label: 'Page Order', modes: ['search'] },
  title: { field: 'label.keyword', order: 'asc', label: 'Title (A-Z)', modes: ['browse'] },
  title_desc: { field: 'label.keyword', order: 'desc', label: 'Title (Z-A)', modes: ['browse'] },
};

/** View mode -> key of its default sort. */
export const DEFAULT_SORT = { browse: 'title', search: 'relevance' };

/**
 * The sorts offered in a view mode, in display order.
 * @param {string} mode - 'browse' | 'search'
 * @returns {Array<{key: string, field: string, order: string, label: string}>}
 */
export function sortsForMode(mode) {
  return Object.entries(SORTS)
    .filter(([, sort]) => sort.modes.includes(mode))
    .map(([key, { field, order, label }]) => ({ key, field, order, label }));
}

/**
 * The sort key for a sort field and order, or null when it has none.
 * @param {string} field
 * @param {string} order
 * @returns {string|null}
 */
export function sortToken(field, order) {
  const entry = Object.entries(SORTS).find(([, s]) => s.field === field && s.order === order);
  return entry ? entry[0] : null;
}

/**
 * The sort to use in a mode given a sort key, falling back to the mode's default
 * when the key is unknown or not offered in that mode.
 * @param {string} mode - 'browse' | 'search'
 * @param {string} [token] - Sort key
 * @returns {{field: string, order: string}}
 */
export function resolveSort(mode, token) {
  const sort = SORTS[token]?.modes.includes(mode) ? SORTS[token] : SORTS[DEFAULT_SORT[mode]];
  return { field: sort.field, order: sort.order };
}
