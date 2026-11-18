/**
 * JHSearch plugin for Mirador 4.
 * Provides collection browsing and search capabilities via Opensearch.
 */

import React from 'react';
import SearchIcon from '@mui/icons-material/Search';
import { getWindowIds, getWorkspace, MiradorMenuButton, setWorkspaceAddVisibility } from 'mirador';

import { ConnectedJHSearchView } from './components/JHSearchView';
import { jhsearchReducer } from './state/reducers';
import { workspaceAddButtonPlugin } from './workspace';

export { keepWorkspaceAddVisible } from './workspace';

/**
 * Look of the button while the JHSearch view is showing: a white icon on grey.
 * Clicking it then closes the view, so it darkens on hover.
 */
const ACTIVE_SX = {
  backgroundColor: 'grey.600',
  color: 'common.white',
  '&:hover': { backgroundColor: 'grey.700' },
};

/**
 * The same while no window is open: closing the view would leave an empty
 * workspace (see keepWorkspaceAddVisible), so the click does nothing and the
 * button shows the default cursor and no hover change.
 */
const ACTIVE_LOCKED_SX = {
  ...ACTIVE_SX,
  cursor: 'default',
  '&:hover': { backgroundColor: 'grey.600' },
};

/**
 * JHSearch button component for the workspace control panel.
 * Toggles the JHSearch view, and is highlighted while that view is showing.
 * With no window open the view stays showing, so the button only opens it.
 */
function JHSearchButton({
  hasWindows = false,
  isWorkspaceAddVisible = false,
  setWorkspaceAddVisibility: showSearch,
}) {
  const locked = isWorkspaceAddVisible && !hasWindows;

  const handleClick = () => {
    if (locked) return;
    showSearch(!isWorkspaceAddVisible);
  };

  let sx = {};
  if (locked) sx = ACTIVE_LOCKED_SX;
  else if (isWorkspaceAddVisible) sx = ACTIVE_SX;

  return (
    <MiradorMenuButton
      aria-label="Search collections"
      onClick={handleClick}
      selected={isWorkspaceAddVisible}
      sx={sx}
    >
      <SearchIcon />
    </MiradorMenuButton>
  );
}

/**
 * Whether the JHSearch view (Mirador's add resource view) is showing, and
 * whether there is a window to go back to when it closes.
 */
function mapJHSearchButtonStateToProps(state) {
  return {
    hasWindows: getWindowIds(state).length > 0,
    isWorkspaceAddVisible: Boolean(getWorkspace(state).isWorkspaceAddVisible),
  };
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
