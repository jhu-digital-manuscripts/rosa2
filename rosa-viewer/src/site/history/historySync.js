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
import { createRouteContext, formatRoute, parseLocation, stripLegacyQuery } from './route';
import { canvasIdForName, nameForCanvasId } from './canvasNames';

const VIEWS_WITH_CANVAS = new Set(['single', 'book']);

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

function firstLabel(label) {
  if (!label) return null;
  if (typeof label === 'string') return label;
  const values = Object.values(label)[0];
  return Array.isArray(values) ? values[0] : values || null;
}

/**
 * Document title for a route.
 */
export function routeTitle(route, state, siteTitle) {
  const suffix = siteTitle ? ` — ${siteTitle}` : '';

  if (route.type === 'book') {
    const manifest = state.manifests?.[route.manifestId]?.json;
    const bookLabel = firstLabel(manifest?.label) || route.bookId;
    const windowState = state.windows?.[route.windowId];
    const canvas = route.canvas && manifest?.items?.find((c) => c.id === windowState?.canvasId);
    const canvasLabel = canvas ? firstLabel(canvas.label) : null;
    return `${bookLabel}${canvasLabel ? `, ${canvasLabel}` : ''}${suffix}`;
  }

  if (route.type === 'search') {
    const q =
      route.mode === 'advanced'
        ? (route.rows || [])
            .filter((r) => r.value && r.value.trim())
            .map((r) => r.value.trim())
            .join(', ')
        : route.query;
    if (q && q.trim()) {
      return `Search: ${q.trim()}${suffix}`;
    }
  }

  return siteTitle || '';
}

function searchStateForRoute(route) {
  const state = {
    collectionId: route.collectionId,
    viewMode: route.type === 'search' ? 'search' : 'browse',
    facets: route.facets,
    sort: route.sort,
    page: route.page,
    pageSize: route.pageSize,
  };
  if (route.type === 'search') {
    state.searchMode = route.mode;
    if (route.mode === 'advanced') {
      state.advancedRows = route.rows;
    } else {
      state.simpleQuery = route.query;
    }
  }
  return state;
}

async function defaultFetchManifest(manifestId) {
  const response = await fetch(manifestId);
  return response.ok ? response.json() : null;
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

  const jhsearch = store.getState().config?.jhsearch || {};
  const ctx = { ...createRouteContext(jhsearch), iiifBaseUrl: jhsearch.iiifBaseUrl };

  let prev = null;
  let prevWindowIds = [];
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
    const route = routeFromState(state, ctx, selectViewType);
    if (!route || route.pending) return;

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

  async function applyBook(route, isCurrent) {
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
      store.dispatch(focusWindow(existingId));
      if (route.view && selectViewType(state, { windowId: existingId }) !== route.view) {
        store.dispatch(setWindowViewType(existingId, route.view));
      }
      if (canvasId && canvasId !== state.windows[existingId].canvasId) {
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

  async function apply(route) {
    const count = ++applyCount;
    const isCurrent = () => connected && count === applyCount;
    applying = true;
    try {
      if (route.type === 'book') {
        await applyBook(route, isCurrent);
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
