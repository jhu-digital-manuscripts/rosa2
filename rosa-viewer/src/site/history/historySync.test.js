import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest';
import {
  addWindow,
  createPluggableStore,
  focusWindow,
  removeWindow,
  setCanvas,
  setWindowViewType,
  setWorkspaceAddVisibility,
} from 'mirador';
import { mergeConfig } from '../../config';
import { jhsearchPlugin } from '../../plugins/jhsearch';
import { setSimpleQuery, setViewMode, toggleFacet } from '../../plugins/jhsearch/state/actions';
import { connectHistory, parseManifestUrl, routeFromState, shouldReplace } from './historySync';

const BASE = 'https://example.org/iiif';

function manifest(collection, book, images) {
  const id = `${BASE}/${collection}/${book}`;
  return {
    id: `${id}/manifest.json`,
    type: 'Manifest',
    label: { en: [`Manuscript ${book}`] },
    items: images.map((image, index) => ({
      id: `${id}/canvas/${index}`,
      type: 'Canvas',
      label: { en: [image] },
      width: 100,
      height: 100,
      items: [
        {
          id: `${id}/canvas/${index}/page`,
          type: 'AnnotationPage',
          items: [
            {
              id: `${id}/canvas/${index}/page/annotation`,
              type: 'Annotation',
              motivation: 'painting',
              target: `${id}/canvas/${index}`,
              body: {
                id: `https://images.example.org/x/full/full/0/default.jpg`,
                type: 'Image',
                service: [
                  {
                    id: `https://images.example.org/${encodeURIComponent(`${collection}/${book}/${book}.${image}`)}`,
                    type: 'ImageService2',
                  },
                ],
              },
            },
          ],
        },
      ],
    })),
  };
}

const MANIFESTS = Object.fromEntries(
  [
    manifest('rose', 'Add12042', ['binding.frontcover', '001r', '001v', '002r']),
    manifest('rose', 'Arras845', ['001r', '001v', '002r']),
  ].map((m) => [m.id, m])
);

const ADD12042 = `${BASE}/rose/Add12042/manifest.json`;
const ARRAS845 = `${BASE}/rose/Arras845/manifest.json`;
const canvasOf = (manifestId, index) => manifestId.replace('manifest.json', `canvas/${index}`);

function createStore() {
  return createPluggableStore(
    mergeConfig({
      id: 'rosa-viewer',
      jhsearch: { iiifBaseUrl: BASE, collectionId: 'dlmm', childCollectionIds: ['rose', 'pizan'] },
    }),
    jhsearchPlugin
  );
}

const hash = () => window.location.hash;
const settle = () => new Promise((resolve) => setTimeout(resolve, 0));

async function waitForHash(expected) {
  await vi.waitFor(() => expect(hash()).toBe(expected));
}

function go(url) {
  window.history.replaceState(null, '', url);
  window.dispatchEvent(new PopStateEvent('popstate'));
}

describe('connectHistory', () => {
  let store;
  let disconnect;
  let pushState;
  let replaceState;

  async function connect(url) {
    window.history.replaceState(null, '', url);
    store = createStore();
    pushState = vi.spyOn(window.history, 'pushState');
    replaceState = vi.spyOn(window.history, 'replaceState');
    disconnect = connectHistory(store, { siteTitle: 'DLMM' });
    await disconnect.ready;
    await settle();
  }

  const state = () => store.getState();
  const windows = () => Object.values(state().windows);

  beforeEach(() => {
    global.fetch.mockImplementation(async (url) =>
      MANIFESTS[url]
        ? new Response(JSON.stringify(MANIFESTS[url]), { status: 200 })
        : new Response('{}', { status: 404 })
    );
  });

  afterEach(() => {
    disconnect?.();
    pushState?.mockRestore();
    replaceState?.mockRestore();
  });

  it('writes the site collection into an empty fragment', async () => {
    await connect('/viewer/');

    expect(hash()).toBe('#dlmm');
    expect(pushState).not.toHaveBeenCalled();
    expect(state().workspace.isWorkspaceAddVisible).toBe(true);
    expect(document.title).toBe('DLMM');
  });

  it('opens a bookmarked page of a book', async () => {
    await connect('/viewer/#rose/Add12042/001v/image');
    await vi.waitFor(() => expect(windows()).toHaveLength(1));

    expect(windows()[0]).toMatchObject({ manifestId: ADD12042, canvasId: canvasOf(ADD12042, 2) });
    expect(state().workspace.isWorkspaceAddVisible).toBe(false);
    await vi.waitFor(() => expect(document.title).toBe('Manuscript Add12042, 001v — DLMM'));
    expect(hash()).toBe('#rose/Add12042/001v/image');
    expect(pushState).not.toHaveBeenCalled();
  });

  it('opens a bookmarked reading view', async () => {
    await connect('/viewer/#rose/Arras845/002r/opening');
    await vi.waitFor(() => expect(windows()).toHaveLength(1));

    expect(windows()[0]).toMatchObject({ view: 'book', canvasId: canvasOf(ARRAS845, 2) });
    expect(hash()).toBe('#rose/Arras845/002r/opening');
  });

  it('rewrites an old search bookmark into the fragment', async () => {
    await connect('/viewer/?q=green&m=30&s=relevance&type=basic&service=rose#rose/search');

    expect(state().jhsearch.ui).toMatchObject({
      selectedCollectionId: 'rose',
      viewMode: 'search',
      searchMode: 'simple',
    });
    expect(state().jhsearch.simpleSearch.query).toBe('green');
    expect(state().jhsearch.pagination.pageSize).toBe(30);
    expect(window.location.search).toBe('');
    expect(hash()).toBe('#rose/search?q=green&m=30');
    expect(pushState).not.toHaveBeenCalled();
    expect(document.title).toBe('Search: green — DLMM');
  });

  it('restores an advanced search from an old bookmark', async () => {
    await connect(
      '/viewer/?q=description%3A%27sword%27&m=30&s=relevance&type=advanced&service=rose#rose/search'
    );

    expect(state().jhsearch.ui.searchMode).toBe('advanced');
    expect(state().jhsearch.advancedSearch.rows).toEqual([
      { field: 'description', value: 'sword', operator: 'AND' },
    ]);
    expect(hash()).toBe("#rose/search?q=description:'sword'&type=advanced&m=30");
  });

  it('adds entries for searches and categories, and restores them on back', async () => {
    await connect('/viewer/#rose');

    store.dispatch(toggleFacet('date', 1350));
    await waitForHash('#rose?f.date=1350');

    store.dispatch(setViewMode('search'));
    store.dispatch(setSimpleQuery('green'));
    await waitForHash('#rose/search?q=green&f.date=1350');
    expect(pushState).toHaveBeenCalledTimes(2);

    go('/viewer/#rose?f.date=1350');
    await settle();
    expect(state().jhsearch.ui.viewMode).toBe('browse');
    expect(state().jhsearch.simpleSearch.query).toBe('');
    expect(state().jhsearch.facets).toEqual({ date: [1350] });

    go('/viewer/#rose');
    await settle();
    expect(state().jhsearch.facets).toEqual({});
    expect(pushState).toHaveBeenCalledTimes(2);
  });

  it('adds an entry for opening a book and a new view, but not for turning pages', async () => {
    await connect('/viewer/#rose/search?q=green');

    store.dispatch(addWindow({ manifestId: ADD12042 }));
    store.dispatch(setWorkspaceAddVisibility(false));
    await waitForHash('#rose/Add12042/binding.frontcover/image');
    expect(pushState).toHaveBeenCalledTimes(1);

    const windowId = windows()[0].id;
    store.dispatch(setCanvas(windowId, canvasOf(ADD12042, 2)));
    await waitForHash('#rose/Add12042/001v/image');
    expect(pushState).toHaveBeenCalledTimes(1);

    store.dispatch(setWindowViewType(windowId, 'gallery'));
    await waitForHash('#rose/Add12042/thumb');
    expect(pushState).toHaveBeenCalledTimes(2);

    // Showing the search view again is a new entry
    store.dispatch(setWorkspaceAddVisibility(true));
    await waitForHash('#rose/search?q=green');
    expect(pushState).toHaveBeenCalledTimes(3);
  });

  it('goes back and forward between search results and an open book', async () => {
    await connect('/viewer/#rose/search?q=green');
    store.dispatch(addWindow({ manifestId: ADD12042, canvasId: canvasOf(ADD12042, 1) }));
    store.dispatch(setWorkspaceAddVisibility(false));
    await waitForHash('#rose/Add12042/001r/image');

    go('/viewer/#rose/search?q=green');
    await settle();
    expect(state().workspace.isWorkspaceAddVisible).toBe(true);
    expect(windows()).toHaveLength(1);

    go('/viewer/#rose/Add12042/002r/opening');
    await vi.waitFor(() => expect(state().workspace.isWorkspaceAddVisible).toBe(false));
    expect(windows()).toHaveLength(1);
    expect(windows()[0]).toMatchObject({ view: 'book', canvasId: canvasOf(ADD12042, 3) });
    expect(hash()).toBe('#rose/Add12042/002r/opening');
  });

  it('reopens a book whose window was closed', async () => {
    await connect('/viewer/#rose/Add12042/001v/image');
    await vi.waitFor(() => expect(windows()).toHaveLength(1));
    store.dispatch(removeWindow(windows()[0].id));
    await settle();

    go('/viewer/#rose/Add12042/001v/image');
    await vi.waitFor(() => expect(windows()).toHaveLength(1));
    expect(windows()[0]).toMatchObject({ manifestId: ADD12042, canvasId: canvasOf(ADD12042, 2) });
  });

  it('updates the URL in place when switching windows or closing one', async () => {
    await connect('/viewer/#rose/Add12042/001v/image');
    await vi.waitFor(() => expect(windows()).toHaveLength(1));
    const first = windows()[0].id;

    store.dispatch(addWindow({ manifestId: ARRAS845 }));
    await waitForHash('#rose/Arras845/001r/image');
    expect(pushState).toHaveBeenCalledTimes(1);
    const second = windows().find((w) => w.id !== first).id;

    store.dispatch(focusWindow(first));
    await waitForHash('#rose/Add12042/001v/image');

    store.dispatch(focusWindow(second));
    await waitForHash('#rose/Arras845/001r/image');

    store.dispatch(removeWindow(second));
    await waitForHash('#rose/Add12042/001v/image');
    expect(pushState).toHaveBeenCalledTimes(1);
  });

  it('stops following the store once disconnected', async () => {
    await connect('/viewer/#rose');
    disconnect();
    store.dispatch(toggleFacet('date', 1350));
    await settle();
    expect(hash()).toBe('#rose');
  });
});

describe('helpers', () => {
  it('parses manifest URLs under the IIIF base URL', () => {
    expect(parseManifestUrl(ADD12042, BASE)).toEqual({ collectionId: 'rose', bookId: 'Add12042' });
    expect(parseManifestUrl('https://other.org/iiif/rose/X/manifest.json', BASE)).toBeNull();
    expect(parseManifestUrl(`${BASE}/rose/manifest.json`, BASE)).toBeNull();
  });

  it('ignores a focused window showing something from elsewhere', () => {
    const state = {
      workspace: { windowIds: ['w'], focusedWindowId: 'w' },
      windows: { w: { id: 'w', manifestId: 'https://other.org/manifest.json' } },
    };
    const ctx = { collectionId: 'dlmm', collectionIds: ['dlmm'], iiifBaseUrl: BASE, pageSize: 20 };
    expect(routeFromState(state, ctx, () => 'single')).toBeNull();
  });

  it('replaces the entry for a page turn and adds one for another book', () => {
    const a = { type: 'book', windowId: 'w1', manifestId: 'a', view: 'single' };
    expect(shouldReplace(null, a)).toBe(true);
    expect(shouldReplace(a, { ...a, canvas: 'x' }, ['w1'], ['w1'])).toBe(true);
    expect(shouldReplace(a, { ...a, view: 'book' }, ['w1'], ['w1'])).toBe(false);
    expect(shouldReplace(a, { ...a, windowId: 'w2', manifestId: 'b' }, ['w1'], ['w1', 'w2'])).toBe(
      false
    );
    expect(shouldReplace(a, { type: 'browse' }, ['w1'], ['w1'])).toBe(false);
  });
});
