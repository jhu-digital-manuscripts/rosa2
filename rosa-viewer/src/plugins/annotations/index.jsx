/**
 * RosaAnnotations plugin for Mirador 4.
 *
 * Replaces the list of annotations shown for each canvas in the annotations
 * side panel. Annotations are grouped (AoR by place on the page, DLMM by kind)
 * under collapsible headings and rendered with the IIIF HTML tag set, keeping
 * small, sub and sup which Mirador's own sanitizer removes. The annotation
 * styleClass values (IIIF cookbook recipe 0045) become CSS classes, styled by
 * the per-collection stylesheets in public/styles/.
 */

import React, { useCallback } from 'react';
import { getAnnotationResourcesByMotivationForCanvas, setWorkspaceAddVisibility } from 'mirador';
import { applySearchState } from '../jhsearch/state/actions';
import { searchStateForRoute } from '../../site/history/route';
import { RosaCanvasAnnotations } from './components/RosaCanvasAnnotations';
import { siteSearchRoute } from './utils/siteLinks';
import './annotations.css';

export { RosaCanvasAnnotations } from './components/RosaCanvasAnnotations';
export { AnnotationBody, htmlToReact, NoteMarker } from './components/AnnotationBody';
export { PlaceIcon } from './components/PlaceIcon';
export { sanitizeIiifHtml } from './utils/sanitize';
export { parseAnnotation } from './utils/parse';
export { groupAnnotations, PLACES, DLMM_KINDS } from './utils/group';
export { addStylesheet } from './utils/stylesheet';
export { openAnnotationsByDefault, manifestHasAnnotations } from './defaultPanel';

const NO_IDS = [];

/**
 * Plugin state: the raw annotation resources for the canvas (so the styleClass is
 * available), the hovered annotation ids of the window, and the site's jhsearch
 * config (to recognise links into the site's search).
 */
export function mapStateToProps(state, { canvasId, windowId }) {
  return {
    annotationResources: getAnnotationResourcesByMotivationForCanvas(state, { canvasId, windowId }),
    hoveredAnnotationIds: state.windows?.[windowId]?.hoveredAnnotationIds || NO_IDS,
    jhsearch: state.config?.jhsearch,
  };
}

/**
 * Actions for following a search link: show the search in the add-resource view.
 */
export const mapDispatchToProps = { applySearchState, setWorkspaceAddVisibility };

/**
 * Wraps CanvasAnnotations, which already receives the canvas label, the selected
 * annotation and the select, deselect and hover actions. A link in an annotation
 * body that is a search in this site (the people, books and places rosa-tool links
 * for AoR) opens the add-resource view with that search instead of a new tab.
 */
export function RosaCanvasAnnotationsWrapper({
  TargetComponent: _target,
  targetProps: _props,
  jhsearch,
  applySearchState: applySearch,
  setWorkspaceAddVisibility: showWorkspaceAdd,
  ...props
}) {
  const onFollowLink = useCallback(
    (href) => {
      const route = siteSearchRoute(href, jhsearch);
      if (!route) return false;
      applySearch?.(searchStateForRoute(route));
      showWorkspaceAdd?.(true);
      return true;
    },
    [jhsearch, applySearch, showWorkspaceAdd]
  );
  return <RosaCanvasAnnotations {...props} onFollowLink={onFollowLink} />;
}

const rosaCanvasAnnotationsPlugin = {
  component: RosaCanvasAnnotationsWrapper,
  mapDispatchToProps,
  mapStateToProps,
  mode: 'wrap',
  name: 'RosaCanvasAnnotations',
  target: 'CanvasAnnotations',
};

/**
 * Use with: Mirador.viewer(config, [...rosaAnnotationsPlugin])
 */
export const rosaAnnotationsPlugin = [rosaCanvasAnnotationsPlugin];

export default rosaAnnotationsPlugin;
