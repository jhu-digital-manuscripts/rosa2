/**
 * JHSearch plugin for Mirador 4.
 * Provides collection browsing and search capabilities via Opensearch.
 */

import React from 'react';
import SearchIcon from '@mui/icons-material/Search';
import { MiradorMenuButton } from 'mirador';

import { ConnectedJHSearchView } from './components/JHSearchView';
import { jhsearchReducer } from './state/reducers';
import { workspaceAddButtonPlugin } from './workspace';

export { keepWorkspaceAddVisible } from './workspace';

/**
 * JHSearch button component for the workspace control panel.
 * Allows returning to the JHSearch view from anywhere.
 */
function JHSearchButton({ setWorkspaceAddVisibility }) {
  const handleClick = () => {
    setWorkspaceAddVisibility(true);
  };

  return (
    <MiradorMenuButton aria-label="Search collections" onClick={handleClick}>
      <SearchIcon />
    </MiradorMenuButton>
  );
}

/**
 * Plugin definition for the JHSearch button in the workspace control panel.
 */
const jhsearchButtonPlugin = {
  component: JHSearchButton,
  mode: 'add',
  name: 'JHSearchButton',
  target: 'WorkspaceControlPanelButtons',
  mapDispatchToProps: (dispatch) => ({
    setWorkspaceAddVisibility: (visible) =>
      dispatch({ type: 'mirador/SET_WORKSPACE_ADD_VISIBILITY', isWorkspaceAddVisible: visible }),
  }),
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
