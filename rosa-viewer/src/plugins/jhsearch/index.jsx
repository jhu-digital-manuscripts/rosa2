/**
 * JHSearch plugin for Mirador 4.
 * Provides collection browsing and search capabilities via Opensearch.
 */

import React from 'react';
import SearchIcon from '@mui/icons-material/Search';
import { getWorkspace, MiradorMenuButton, setWorkspaceAddVisibility } from 'mirador';

import { ConnectedJHSearchView } from './components/JHSearchView';
import { jhsearchReducer } from './state/reducers';
import { workspaceAddButtonPlugin } from './workspace';

export { keepWorkspaceAddVisible } from './workspace';

/**
 * Look of the button while the JHSearch view is showing: a white icon on grey,
 * also on hover, with the default cursor because clicking it then does nothing.
 */
const ACTIVE_SX = {
  backgroundColor: 'grey.600',
  color: 'common.white',
  cursor: 'default',
  '&:hover': { backgroundColor: 'grey.600' },
};

/**
 * JHSearch button component for the workspace control panel.
 * Allows returning to the JHSearch view from anywhere, and is highlighted while
 * that view is showing.
 */
function JHSearchButton({ isWorkspaceAddVisible = false, setWorkspaceAddVisibility: showSearch }) {
  const handleClick = () => {
    showSearch(true);
  };

  return (
    <MiradorMenuButton
      aria-label="Search collections"
      onClick={handleClick}
      selected={isWorkspaceAddVisible}
      sx={isWorkspaceAddVisible ? ACTIVE_SX : {}}
    >
      <SearchIcon />
    </MiradorMenuButton>
  );
}

/** Whether the JHSearch view (Mirador's add resource view) is showing. */
function mapJHSearchButtonStateToProps(state) {
  return { isWorkspaceAddVisible: Boolean(getWorkspace(state).isWorkspaceAddVisible) };
}

/**
 * Plugin definition for the JHSearch button in the workspace control panel.
 */
const jhsearchButtonPlugin = {
  component: JHSearchButton,
  mode: 'add',
  name: 'JHSearchButton',
  target: 'WorkspaceControlPanelButtons',
  mapStateToProps: mapJHSearchButtonStateToProps,
  mapDispatchToProps: { setWorkspaceAddVisibility },
};

/**
 * Plugin definition for the main JHSearch view.
 * This wraps WorkspaceAdd to replace the default manifest addition view.
 */
const jhsearchViewPlugin = {
  component: ConnectedJHSearchView,
  mode: 'wrap',
  name: 'JHSearchView',
  target: 'WorkspaceAdd',
};

/**
 * Plugin definition for the JHSearch reducer.
 */
const jhsearchReducerPlugin = {
  name: 'JHSearchReducer',
  reducers: {
    jhsearch: jhsearchReducer,
  },
};

/**
 * Complete JHSearch plugin array for Mirador.
 * Use with: Mirador.viewer(config, [...jhsearchPlugin])
 */
export const jhsearchPlugin = [
  jhsearchViewPlugin,
  jhsearchButtonPlugin,
  jhsearchReducerPlugin,
  workspaceAddButtonPlugin,
];

export default jhsearchPlugin;
