/**
 * Open the sidebar on the annotations panel for books that have annotations.
 *
 * Mirador opens every new window with the same sidebar settings (closed, on the
 * 'info' panel, by default). This watches the store and, once a new window's
 * manifest has loaded, opens its sidebar on the annotations panel if any canvas
 * references an annotation page. rosa-tool only writes canvas.annotations when a
 * page has annotations, so the manifest alone tells whether there are any. Other
 * windows keep their sidebar closed.
 *
 * Each window is decided once. A window whose panel the user has already changed
 * is left alone.
 */

import { addOrUpdateCompanionWindow, getManifest, getWindows, updateWindow } from 'mirador';

export const ANNOTATIONS_PANEL = 'annotations';

/**
 * Whether a IIIF Presentation 3 manifest references annotations on any canvas.
 * @param {Object} manifestJson
 * @returns {boolean}
 */
export function manifestHasAnnotations(manifestJson) {
  const canvases = Array.isArray(manifestJson?.items) ? manifestJson.items : [];
  return canvases.some((canvas) => Array.isArray(canvas?.annotations) && canvas.annotations.length);
}

/**
 * @param {Object} store - Mirador's redux store (viewer.store)
 * @param {Object} [options]
 * @param {string} [options.defaultPanel='info'] - Panel windows open on; only
 *   windows still showing it are switched
 * @returns {Function} Unsubscribe
 */
export function openAnnotationsByDefault(store, { defaultPanel = 'info' } = {}) {
  const decided = new Set();

  const check = () => {
    const state = store.getState();
    const windows = getWindows(state) || {};

    for (const id of decided) {
      if (!windows[id]) decided.delete(id);
    }

    for (const win of Object.values(windows)) {
      if (decided.has(win.id) || !win.manifestId) continue;
      if (win.sideBarPanel !== defaultPanel) {
        decided.add(win.id);
        continue;
      }
      const manifest = getManifest(state, { manifestId: win.manifestId });
      if (!manifest || manifest.isFetching || (!manifest.json && !manifest.error)) continue;

      decided.add(win.id);
      if (manifestHasAnnotations(manifest.json)) {
        store.dispatch(
          addOrUpdateCompanionWindow(win.id, { content: ANNOTATIONS_PANEL, position: 'left' })
        );
        if (!win.sideBarOpen) {
          store.dispatch(updateWindow(win.id, { sideBarOpen: true }));
        }
      }
    }
  };

  const unsubscribe = store.subscribe(check);
  check();
  return unsubscribe;
}
