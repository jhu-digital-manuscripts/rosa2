import { describe, it, expect, vi } from 'vitest';

vi.mock('mirador', () => ({
  addOrUpdateCompanionWindow: (windowId, payload) => ({ type: 'UPDATE_CW', windowId, payload }),
  getManifest: (state, { manifestId }) => state.manifests?.[manifestId],
  getWindows: (state) => state.windows,
  updateWindow: (id, payload) => ({ type: 'UPDATE_WINDOW', id, payload }),
}));

import { manifestHasAnnotations, openAnnotationsByDefault } from './defaultPanel';

const annotated = {
  items: [{ id: 'c0' }, { id: 'c1', annotations: [{ id: 'c1/annotations.json' }] }],
};
const plain = { items: [{ id: 'c0' }, { id: 'c1', annotations: [] }] };

/** Minimal redux-like store; dispatching UPDATE_CW sets the window's panel. */
function createStore(state) {
  const listeners = [];
  const store = {
    state,
    getState: () => store.state,
    subscribe: (fn) => {
      listeners.push(fn);
      return () => listeners.splice(listeners.indexOf(fn), 1);
    },
    dispatch: vi.fn((action) => {
      if (action.type === 'UPDATE_WINDOW') {
        const w = store.state.windows[action.id];
        store.setState({
          windows: { ...store.state.windows, [w.id]: { ...w, ...action.payload } },
        });
      }
      if (action.type === 'UPDATE_CW') {
        const win = store.state.windows[action.windowId];
        store.setState({
          windows: {
            ...store.state.windows,
            [win.id]: { ...win, sideBarPanel: action.payload.content },
          },
        });
      }
    }),
    setState: (patch) => {
      store.state = { ...store.state, ...patch };
      listeners.slice().forEach((fn) => fn());
    },
  };
  return store;
}

const win = (id, manifestId, sideBarPanel = 'info', sideBarOpen = false) => ({
  id,
  manifestId,
  sideBarPanel,
  sideBarOpen,
});

describe('manifestHasAnnotations', () => {
  it('is true when any canvas references an annotation page', () => {
    expect(manifestHasAnnotations(annotated)).toBe(true);
    expect(manifestHasAnnotations(plain)).toBe(false);
    expect(manifestHasAnnotations({})).toBe(false);
    expect(manifestHasAnnotations(undefined)).toBe(false);
  });
});

describe('openAnnotationsByDefault', () => {
  it("opens a new window's sidebar on the annotations panel once its manifest loads", () => {
    const store = createStore({ windows: { w1: win('w1', 'm1') }, manifests: {} });
    openAnnotationsByDefault(store);
    expect(store.dispatch).not.toHaveBeenCalled();

    store.setState({ manifests: { m1: { isFetching: true } } });
    expect(store.dispatch).not.toHaveBeenCalled();

    store.setState({ manifests: { m1: { isFetching: false, json: annotated } } });
    expect(store.dispatch).toHaveBeenCalledWith({
      type: 'UPDATE_CW',
      windowId: 'w1',
      payload: { content: 'annotations', position: 'left' },
    });
    expect(store.dispatch).toHaveBeenCalledWith({
      type: 'UPDATE_WINDOW',
      id: 'w1',
      payload: { sideBarOpen: true },
    });
    expect(store.state.windows.w1).toMatchObject({
      sideBarPanel: 'annotations',
      sideBarOpen: true,
    });
  });

  it('only switches the panel when the sidebar is already open', () => {
    const store = createStore({
      windows: { w1: win('w1', 'm1', 'info', true) },
      manifests: { m1: { json: annotated } },
    });
    openAnnotationsByDefault(store);
    expect(store.dispatch).toHaveBeenCalledTimes(1);
    expect(store.state.windows.w1).toMatchObject({
      sideBarPanel: 'annotations',
      sideBarOpen: true,
    });
  });

  it('leaves the sidebar of books without annotations closed', () => {
    const store = createStore({
      windows: { w1: win('w1', 'm1') },
      manifests: { m1: { json: plain } },
    });
    openAnnotationsByDefault(store);
    expect(store.dispatch).not.toHaveBeenCalled();
  });

  it('decides each window once, so the user can close the sidebar or change panel', () => {
    const store = createStore({
      windows: { w1: win('w1', 'm1') },
      manifests: { m1: { json: annotated } },
    });
    openAnnotationsByDefault(store);
    expect(store.dispatch).toHaveBeenCalledTimes(2);
    store.setState({ windows: { w1: win('w1', 'm1', 'info', false) } });
    expect(store.dispatch).toHaveBeenCalledTimes(2);
    expect(store.state.windows.w1).toMatchObject({ sideBarPanel: 'info', sideBarOpen: false });

    // A new window of the same, already loaded book is opened too
    store.setState({ windows: { ...store.state.windows, w2: win('w2', 'm1') } });
    expect(store.dispatch).toHaveBeenCalledTimes(4);
    expect(store.state.windows.w2).toMatchObject({
      sideBarPanel: 'annotations',
      sideBarOpen: true,
    });
  });

  it('does not override a panel chosen before the manifest loaded', () => {
    const store = createStore({ windows: { w1: win('w1', 'm1', 'canvas') }, manifests: {} });
    openAnnotationsByDefault(store);
    store.setState({ manifests: { m1: { json: annotated } } });
    expect(store.dispatch).not.toHaveBeenCalled();
  });

  it('gives up on a manifest that failed to load', () => {
    const store = createStore({
      windows: { w1: win('w1', 'm1') },
      manifests: { m1: { error: 'boom' } },
    });
    openAnnotationsByDefault(store);
    store.setState({ manifests: { m1: { json: annotated } } });
    expect(store.dispatch).not.toHaveBeenCalled();
  });
});
