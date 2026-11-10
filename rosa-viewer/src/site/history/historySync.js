/**
 * Keeps the browser URL and history in step with the viewer.
 *
 * The URL describes what the user is looking at: the browse/search view when it
 * is shown, and otherwise the book in the focused Mirador window (see route.js).
 * Only that one resource is in the URL, however many windows are open.
 *
 * History entries are added when the user moves somewhere new: a search, a
 * different page of results, a category, a book or a different view of a book.
 * Turning pages, switching between open windows and closing a window update the
 * URL in place.
 *
 * Going back or forward (or opening a bookmark) restores the state in the URL:
 * - browse/search: the search view is shown with the collection, query,
 *   categories, sort and page restored; open windows are left alone.
 * - book: a window already showing the book is focused and moved to the page and
 *   view; if there is none, a window is opened. The search view is hidden.
 */

import {
  addWindow,
  focusWindow,
  setCanvas,
  setWindowViewType,
  setWorkspaceAddVisibility,
  getWindowViewType,
} from 'mirador';
import { applySearchState } from '../../plugins/jhsearch/state/actions';
import { buildManifestUrl } from '../../plugins/jhsearch/utils/urlBuilder';
import {
  createRouteContext,
  formatRoute,
  parseLocation,
  searchStateForRoute,
  stripLegacyQuery,
  VIEWS_WITH_CANVAS,
} from './route';
import { iiifLabel } from '../../utils/iiif';
import { canvasIdForName, nameForCanvasId } from './canvasNames';

/**
 * Collection and book of a manifest URL under the IIIF base URL.
 * @param {string} manifestId - e.g. 'https://x/iiif/rose/Douce195/manifest.json'
 * @param {string} iiifBaseUrl
 * @returns {{collectionId: string, bookId: string}|null}
 */
export function parseManifestUrl(manifestId, iiifBaseUrl) {
  const prefix = `${iiifBaseUrl}/`;
  const suffix = '/manifest.json';
  if (!manifestId || !manifestId.startsWith(prefix) || !manifestId.endsWith(suffix)) {
    return null;
  }
  const parts = manifestId.slice(prefix.length, -suffix.length).split('/');
  return parts.length === 2 && parts[0] && parts[1]
    ? { collectionId: parts[0], bookId: parts[1] }
    : null;
}

/**
 * Route for the browse/search view's state.
 */
function searchViewRoute(jhsearch, ctx) {
  const ui = jhsearch?.ui || {};
  const listing = {
    collectionId: ui.selectedCollectionId || ctx.collectionId,
    facets: jhsearch?.facets || {},
    sort: jhsearch?.sort,
    page: jhsearch?.pagination?.page || 0,
    pageSize: jhsearch?.pagination?.pageSize || ctx.pageSize,
  };

  if (ui.viewMode === 'search') {
    if (ui.searchMode === 'advanced') {
      return { type: 'search', mode: 'advanced', rows: jhsearch.advancedSearch?.rows, ...listing };
    }
    return { type: 'search', mode: 'simple', query: jhsearch.simpleSearch?.query, ...listing };
  }
  return { type: 'browse', ...listing };
}

/**
 * The route for the current viewer state.
 *
 * @param {Object} state - Redux state
 * @param {Object} ctx - Route context, plus iiifBaseUrl
 * @returns {Object|null} Route; {pending: true} while the page of the focused
 *   window cannot be named yet (its manifest is loading); null when the focused
 *   window shows something that is not from this site
 */
export function routeFromState(state, ctx, selectViewType = getWindowViewType) {
  const workspace = state.workspace || {};
  const windows = state.windows || {};
  const windowIds = workspace.windowIds || [];

  if (workspace.isWorkspaceAddVisible || windowIds.length === 0) {
    return searchViewRoute(state.jhsearch, ctx);
  }

  const windowId = windows[workspace.focusedWindowId]
    ? workspace.focusedWindowId
    : windowIds[windowIds.length - 1];
  const window = windows[windowId];
  const book = parseManifestUrl(window?.manifestId, ctx.iiifBaseUrl);
  if (!book || !ctx.collectionIds.includes(book.collectionId)) {
    return null;
  }

  const view = selectViewType(state, { windowId });
  const route = {
    type: 'book',
    ...book,
    view,
    canvas: null,
    windowId,
    manifestId: window.manifestId,
  };

  if (VIEWS_WITH_CANVAS.has(view)) {
    const manifest = state.manifests?.[window.manifestId]?.json;
    const canvas =
      manifest && window.canvasId && nameForCanvasId(manifest, window.canvasId, book.bookId);
    if (!canvas) {
      return { pending: true };
    }
    route.canvas = canvas;
  }
  return route;
}

/**
 * Whether going from one route to the next should replace the current history
 * entry instead of adding one.
 */
export function shouldReplace(prev, route, prevWindowIds = [], windowIds = []) {
  if (!prev) return true;

  // A window was closed
  if (prevWindowIds.some((id) => !windowIds.includes(id))) return true;

  if (prev.type === 'book' && route.type === 'book') {
    // Page turn
    if (
      route.windowId === prev.windowId &&
      route.manifestId === prev.manifestId &&
      route.view === prev.view
    ) {
      return true;
    }
    // Switched to a window that was already open
    if (route.windowId !== prev.windowId && prevWindowIds.includes(route.windowId)) {
      return true;
    }
  }
  return false;
}

/**
 * Document title for a route.
 */
export function routeTitle(route, state, siteTitle) {
  const suffix = siteTitle ? ` — ${siteTitle}` : '';

  if (route.type === 'book') {
    const manifest = state.manifests?.[route.manifestId]?.json;
    const bookLabel = iiifLabel(manifest?.label) || route.bookId;
    const windowState = state.windows?.[route.windowId];
    const canvas = route.canvas && manifest?.items?.find((c) => c.id === windowState?.canvasId);
    const canvasLabel = canvas ? iiifLabel(canvas.label) : '';
    return `${bookLabel}${canvasLabel ? `, ${canvasLabel}` : ''}${suffix}`;
  }

  if (route.type === 'search') {
    const q =
      route.mode === 'advanced'
        ? (route.rows || [])
            .flatMap((r) => [r.value, r.text])
            .filter((t) => t && t.trim())
            .map((t) => t.trim())
            .join(', ')
        : route.query;
    if (q && q.trim()) {
      return `Search: ${q.trim()}${suffix}`;
    }
  }

  return siteTitle || '';
}

async function defaultFetchManifest(manifestId) {
  const response = await fetch(manifestId);
  return response.ok ? response.json() : null;
}

/**
 * The route context for a site's jhsearch config, plus the IIIF base URL.
 * @param {Object} [jhsearch] - config.jhsearch
 * @returns {Object}
 */
export function routeContextFor(jhsearch = {}) {
  return { ...createRouteContext(jhsearch), iiifBaseUrl: jhsearch.iiifBaseUrl };
}

/**
 * Show a book route: focus the window that shows the book, moving it to the page
 * and view, or open a window at that page; then hide the search view. Used when a
 * URL is restored and when a link to a page is followed in place.
 *
 * Moving a window that is already open, or switching to one, replaces the URL (see
 * shouldReplace). A followed link should be a place the reader can go back from, so
 * when `win` is given and that is what will happen, the current URL is pushed first
 * and the change then replaces that copy. Nothing is pushed when the history adds an
 * entry itself (a new window, a new view, leaving the search view) or when the link
 * leads to what is already showing.
 *
 * @param {Object} store - Redux store of the Mirador viewer
 * @param {Object} route - A book route from parseLocation
 * @param {Object} ctx - routeContextFor(config.jhsearch)
 * @param {Object} [options]
 * @param {Function} [options.fetchManifest] - (manifestId) => Promise<json|null>
 * @param {Function} [options.selectViewType] - Mirador's getWindowViewType; for tests
 * @param {Function} [options.isCurrent] - () => false to stop after the manifest fetch
 * @param {Window} [options.win] - When given, a change that would replace the URL gets
 *   a history entry of its own
 */
export async function openBookRoute(
  store,
  route,
  ctx,
  {
    fetchManifest = defaultFetchManifest,
    selectViewType = getWindowViewType,
    isCurrent = () => true,
    win = null,
  } = {}
) {
  const manifestId = buildManifestUrl(`${route.collectionId}.${route.bookId}`, ctx.iiifBaseUrl);
  let state = store.getState();
  const windows = state.windows || {};
  const windowIds = state.workspace?.windowIds || [];
  const focused = state.workspace?.focusedWindowId;
  const existingId =
    windows[focused]?.manifestId === manifestId
      ? focused
      : windowIds.find((id) => windows[id]?.manifestId === manifestId);

  const needCanvas = route.canvas && VIEWS_WITH_CANVAS.has(route.view || 'single');
  let manifest = state.manifests?.[manifestId]?.json;
  const fetched = !manifest && (needCanvas || !existingId);
  if (fetched) {
    // Fetched here rather than by Mirador so the page can be found before the
    // window opens; Mirador would otherwise move it to the first page.
    manifest = await fetchManifest(manifestId).catch(() => null);
    if (!isCurrent()) return;
    state = store.getState();
  }

  const canvasId =
    needCanvas && manifest ? canvasIdForName(manifest, route.canvas, route.bookId) : null;

  if (existingId && state.windows?.[existingId]) {
    const workspace = state.workspace || {};
    const viewChanges =
      Boolean(route.view) && selectViewType(state, { windowId: existingId }) !== route.view;
    const canvasChanges = Boolean(canvasId) && canvasId !== state.windows[existingId].canvasId;
    const switches = workspace.focusedWindowId !== existingId;
    const replacesUrl =
      !workspace.isWorkspaceAddVisible && (switches || (canvasChanges && !viewChanges));
    if (win && replacesUrl) {
      win.history.pushState(null, '', win.location.href);
    }
    store.dispatch(focusWindow(existingId));
    if (viewChanges) {
      store.dispatch(setWindowViewType(existingId, route.view));
    }
    if (canvasChanges) {
      store.dispatch(setCanvas(existingId, canvasId));
    }
  } else {
    store.dispatch(
      addWindow({
        manifestId,
        ...(canvasId ? { canvasId } : {}),
        ...(route.view ? { view: route.view } : {}),
        ...(fetched && manifest ? { manifest } : {}),
      })
    );
  }
  store.dispatch(setWorkspaceAddVisibility(false));
}

/**
 * Connect a Mirador store to the browser history.
 *
 * Restores the state in the current URL straight away, then follows the store.
 *
 * @param {Object} store - Redux store of the Mirador viewer
 * @param {Object} [options]
 * @param {string} [options.siteTitle] - Appended to document titles
 * @param {Window} [options.window=window]
 * @param {Function} [options.fetchManifest] - (manifestId) => Promise<json|null>
 * @param {Function} [options.selectViewType] - Mirador's getWindowViewType; for tests
 * @returns {Function} disconnect
 */
export function connectHistory(store, options = {}) {
  const {
    siteTitle,
    window: win = globalThis.window,
    fetchManifest = defaultFetchManifest,
    selectViewType = getWindowViewType,
  } = options;

  const ctx = routeContextFor(store.getState().config?.jhsearch);

  let prev = null;
  let prevWindowIds = [];
  // The parts of the state the route and title are made from, as of the last flush
  let seen = [];
  let applying = false;
  let applyCount = 0;
  let replaceNext = false;
  let scheduled = false;
  let connected = true;

  const currentFragment = () => win.location.hash.replace(/^#/, '');

  function write(fragment, replace) {
    const { pathname, search } = win.location;
    const url = `${pathname}${stripLegacyQuery(search)}#${fragment}`;
    if (replace) {
      win.history.replaceState(null, '', url);
    } else {
      win.history.pushState(null, '', url);
    }
  }

  function flush() {
    scheduled = false;
    if (!connected || applying) return;

    const state = store.getState();
    // Most dispatches (viewport changes, hover, loading flags) touch none of the
    // state the URL describes. After restoring a URL (prev is null) always look.
    const parts = [state.workspace, state.windows, state.manifests, state.jhsearch];
    if (prev && !replaceNext && parts.every((part, i) => part === seen[i])) return;

    const route = routeFromState(state, ctx, selectViewType);
    if (!route || route.pending) return;
    seen = parts;

    const windowIds = state.workspace?.windowIds || [];
    const fragment = formatRoute(route, ctx);
    const replace = replaceNext || shouldReplace(prev, route, prevWindowIds, windowIds);
    const changed =
      fragment !== currentFragment() ||
      stripLegacyQuery(win.location.search) !== win.location.search;

    prev = route;
    prevWindowIds = windowIds;
    replaceNext = false;

    const title = routeTitle(route, state, siteTitle);
    if (title) win.document.title = title;
    if (changed) write(fragment, replace);
  }

  function scheduleFlush() {
    if (scheduled) return;
    scheduled = true;
    queueMicrotask(flush);
  }

  async function apply(route) {
    const count = ++applyCount;
    const isCurrent = () => connected && count === applyCount;
    applying = true;
    try {
      if (route.type === 'book') {
        await openBookRoute(store, route, ctx, { fetchManifest, selectViewType, isCurrent });
      } else {
        store.dispatch(applySearchState(searchStateForRoute(route)));
        store.dispatch(setWorkspaceAddVisibility(true));
      }
    } catch (error) {
      console.error('rosa-viewer: could not restore the state in the URL', error);
    } finally {
      if (isCurrent()) {
        applying = false;
        // Normalizing the URL (e.g. rewriting an old bookmark) is not a new entry
        replaceNext = true;
        prev = null;
        scheduleFlush();
      }
    }
  }

  const onPopState = () => apply(parseLocation(win.location, ctx));

  const unsubscribe = store.subscribe(scheduleFlush);
  win.addEventListener('popstate', onPopState);
  const ready = apply(parseLocation(win.location, ctx));

  function disconnect() {
    connected = false;
    unsubscribe();
    win.removeEventListener('popstate', onPopState);
  }
  disconnect.ready = ready;
  return disconnect;
}
