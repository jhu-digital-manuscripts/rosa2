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

import { getAnnotationResourcesByMotivationForCanvas } from 'mirador';
import { RosaCanvasAnnotations } from './components/RosaCanvasAnnotations';
import './annotations.css';

export { RosaCanvasAnnotations } from './components/RosaCanvasAnnotations';
export { AnnotationBody, htmlToReact, NoteMarker } from './components/AnnotationBody';
export { PlaceIcon } from './components/PlaceIcon';
export { sanitizeIiifHtml } from './utils/sanitize';
export { parseAnnotation } from './utils/parse';
export { groupAnnotations, PLACES, DLMM_KINDS } from './utils/group';
export { addStylesheet } from './utils/stylesheet';

const NO_IDS = [];

/**
 * Plugin state: the raw annotation resources for the canvas (so the styleClass is
 * available) and the hovered annotation ids of the window.
 */
export function mapStateToProps(state, { canvasId, windowId }) {
  return {
    annotationResources: getAnnotationResourcesByMotivationForCanvas(state, { canvasId, windowId }),
    hoveredAnnotationIds: state.windows?.[windowId]?.hoveredAnnotationIds || NO_IDS,
  };
}

/**
 * Wraps CanvasAnnotations, which already receives the canvas label, the selected
 * annotation and the select, deselect and hover actions.
 */
function RosaCanvasAnnotationsWrapper({ TargetComponent: _target, targetProps: _props, ...props }) {
  return <RosaCanvasAnnotations {...props} />;
}

const rosaCanvasAnnotationsPlugin = {
  component: RosaCanvasAnnotationsWrapper,
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
