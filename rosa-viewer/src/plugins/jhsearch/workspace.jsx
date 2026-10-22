/**
 * Keep the JHSearch view (Mirador's "add resource" view) on screen whenever no
 * window is open, so the user never lands on Mirador's empty "Welcome to
 * Mirador" workspace.
 *
 * - keepWorkspaceAddVisible() shows the view again when the last window closes.
 * - The WorkspaceAddButton wrap plugin hides the button that closes the view
 *   while there is no window to go back to.
 */

import React from 'react';
import { getWindowIds, getWorkspace, setWorkspaceAddVisibility } from 'mirador';

/** Whether the add resource view is hidden although no window is open. */
export function isWorkspaceEmpty(state) {
  return getWindowIds(state).length === 0 && !getWorkspace(state).isWorkspaceAddVisible;
}

/**
 * Show the add resource view whenever the workspace has no windows.
 * @param {Object} store - Mirador's redux store (viewer.store)
 * @returns {Function} Unsubscribe
 */
export function keepWorkspaceAddVisible(store) {
  const check = () => {
    if (isWorkspaceEmpty(store.getState())) {
      store.dispatch(setWorkspaceAddVisibility(true));
    }
  };
  const unsubscribe = store.subscribe(check);
  check();
  return unsubscribe;
}

/**
 * Wraps WorkspaceAddButton and leaves it out while it could only close the add
 * resource view onto an empty workspace.
 */
export function WorkspaceAddButtonWrapper({ TargetComponent, targetProps, hasWindows }) {
  if (!hasWindows && targetProps.isWorkspaceAddVisible) return null;
  return <TargetComponent {...targetProps} />;
}

export function mapWorkspaceAddButtonStateToProps(state) {
  return { hasWindows: getWindowIds(state).length > 0 };
}

export const workspaceAddButtonPlugin = {
  component: WorkspaceAddButtonWrapper,
  mapStateToProps: mapWorkspaceAddButtonStateToProps,
  mode: 'wrap',
  name: 'JHSearchWorkspaceAddButton',
  target: 'WorkspaceAddButton',
};
