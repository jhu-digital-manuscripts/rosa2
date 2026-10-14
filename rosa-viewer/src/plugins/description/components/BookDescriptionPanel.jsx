/**
 * Companion window showing the book description of the window's manifest.
 * Folio references navigate the window to their canvas.
 */

import React, { useCallback, useMemo } from 'react';
import { ConnectedCompanionWindow } from 'mirador';
import { BookDescription } from './BookDescription';
import { useBookDescription } from '../hooks/useBookDescription';
import { selectTeiDescriptionUrl } from '../utils/manifest';
import { buildFolioIndex, resolveLocus } from '../utils/folio';

export const PANEL_TITLE = 'Book description';

const MESSAGES = {
  loading: 'Loading the book description…',
  empty: 'No description is available for this book.',
  error: 'The book description could not be loaded.',
};

function Status({ status }) {
  return (
    <div className="rosa-annos rosa-desc">
      <p className="rosa-desc-status" role="status">
        {MESSAGES[status]}
      </p>
    </div>
  );
}

/**
 * @param {Object} props
 * @param {string} props.id - Companion window id
 * @param {string} props.windowId
 * @param {Object} [props.manifest] - Manifest JSON
 * @param {Function} [props.setCanvas] - (windowId, canvasId)
 */
export function BookDescriptionPanel({ id, windowId, manifest, setCanvas }) {
  const url = useMemo(() => selectTeiDescriptionUrl(manifest), [manifest]);
  const { status, description } = useBookDescription(url);
  const index = useMemo(() => buildFolioIndex(manifest?.items), [manifest]);
  const resolve = useCallback((locus) => resolveLocus(locus, index), [index]);
  const onNavigate = useCallback(
    (canvasId) => setCanvas?.(windowId, canvasId),
    [setCanvas, windowId]
  );

  let content;
  if (status === 'ready') {
    content = (
      <BookDescription
        description={description}
        resolveLocus={resolve}
        onNavigate={setCanvas ? onNavigate : undefined}
      />
    );
  } else {
    content = <Status status={status === 'idle' ? 'empty' : status} />;
  }

  return (
    <ConnectedCompanionWindow
      title={PANEL_TITLE}
      paperClassName="rosa-desc-panel"
      windowId={windowId}
      id={id}
    >
      {content}
    </ConnectedCompanionWindow>
  );
}

export default BookDescriptionPanel;
