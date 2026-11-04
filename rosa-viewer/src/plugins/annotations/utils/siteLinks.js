/**
 * Links in annotation bodies that point into this site.
 *
 * rosa-tool writes the people, books, locations and cross-references named in AoR
 * annotations as links to the site's advanced search, e.g.
 * `https://example.org/aor/#aor/search?q=people:'Plutarch'&type=advanced`, and the
 * targets of internal references as links to the page they point to, e.g.
 * `https://example.org/aor/#aor/RCPD1266/037v/image` (see docs/iiif-generation.md).
 * In another IIIF viewer they are ordinary links; here they are followed in place,
 * without reloading the page.
 */

import { createRouteContext, parseLocation } from '../../../site/history/route';

/** A path with a trailing index.html or slash removed, so /aor/, /aor and /aor/index.html match. */
function normalizePath(pathname) {
  return pathname.replace(/\/index\.html$/, '/').replace(/\/+$/, '') || '/';
}

/**
 * The route a link leads to, when it is a search or a page of a book in this site.
 *
 * A link is into this site when it is only a fragment, or when its origin and path
 * are those of the current page (the query string is ignored: WordPress pages
 * carry parameters such as `?page_id=7`). A link to another site, to a collection's
 * browse view or to a fragment the site does not understand gives null.
 *
 * @param {string} href - Link target as written in the annotation
 * @param {Object} jhsearch - config.jhsearch of the site (collection ids, page size)
 * @param {Location} [location=globalThis.location] - The current page's location
 * @returns {Object|null} Route from parseLocation with type 'search' or 'book', or null
 */
export function siteRoute(href, jhsearch, location = globalThis.location) {
  if (!href || !jhsearch?.collectionId) return null;

  let url;
  try {
    url = new URL(href, location?.href || 'http://localhost/');
  } catch {
    return null;
  }
  if (!url.hash || url.hash === '#') return null;

  if (!href.startsWith('#')) {
    if (
      !location ||
      url.origin !== location.origin ||
      normalizePath(url.pathname) !== normalizePath(location.pathname)
    ) {
      return null;
    }
  }

  const route = parseLocation({ hash: url.hash }, createRouteContext(jhsearch));
  return route.type === 'search' || route.type === 'book' ? route : null;
}
