/**
 * Book description plugin for Mirador 4.
 *
 * Adds a sidebar tab (a coffee mug) that opens the book description of the
 * window's manifest: the notesStmt sections of the TEI file linked from the
 * manifest's seeAlso (IIIF cookbook recipe 0053, written by rosa-tool
 * generate-iiif-pres). The tab is shown only for manifests that link a TEI
 * description, so the plugin does nothing for other books.
 *
 * Mirador renders every "add" plugin of WindowSideBarButtons as a tab, using the
 * component's static value as the companion window content key. To hide the
 * tab, a "wrap" plugin renders the buttons with a plugin map that leaves it out.
 */

import React, { useContext, useMemo } from 'react';
import CoffeeIcon from '@mui/icons-material/CoffeeSharp';
import { PluginContext, getManifest, setCanvas } from 'mirador';
import { BookDescriptionPanel } from './components/BookDescriptionPanel';
import { hasTeiDescription } from './utils/manifest';
import './description.css';

export { BookDescription, renderInline } from './components/BookDescription';
export { BookDescriptionPanel } from './components/BookDescriptionPanel';
export { useBookDescription, loadBookDescription } from './hooks/useBookDescription';
export { parseDescription, humanizeRend, TeiParseError } from './utils/tei';
export { parseFolioRef, buildFolioIndex, resolveLocus } from './utils/folio';
export { selectTeiDescriptionUrl, hasTeiDescription } from './utils/manifest';

/** Companion window content key, also the sidebar tab value. */
export const BOOK_DESCRIPTION_PANEL = 'bookDescription';

const BUTTON_PLUGIN_NAME = 'RosaBookDescriptionButton';

/** Sidebar tab icon. The tab's tooltip is openCompanionWindow_bookDescription. */
export function BookDescriptionIcon() {
  return <CoffeeIcon />;
}
BookDescriptionIcon.value = BOOK_DESCRIPTION_PANEL;

/**
 * The plugin map without the description tab.
 * @param {Object} pluginMap - Mirador's target -> mode -> plugins map
 * @returns {Object}
 */
export function withoutDescriptionButton(pluginMap) {
  const buttons = pluginMap?.WindowSideBarButtons;
  if (!buttons?.add?.some((p) => p.name === BUTTON_PLUGIN_NAME)) return pluginMap;
  return {
    ...pluginMap,
    WindowSideBarButtons: {
      ...buttons,
      add: buttons.add.filter((p) => p.name !== BUTTON_PLUGIN_NAME),
    },
  };
}

/**
 * Wraps WindowSideBarButtons and hides the description tab for manifests
 * without a TEI description.
 */
export function SideBarButtonsWrapper({ TargetComponent, targetProps, hasDescription }) {
  const pluginMap = useContext(PluginContext);
  const filtered = useMemo(() => withoutDescriptionButton(pluginMap), [pluginMap]);

  if (hasDescription) return <TargetComponent {...targetProps} />;
  return (
    <PluginContext.Provider value={filtered}>
      <TargetComponent {...targetProps} />
    </PluginContext.Provider>
  );
}

/** Whether the window's manifest links a TEI description. */
export function mapSideBarStateToProps(state, { windowId }) {
  return { hasDescription: hasTeiDescription(getManifest(state, { windowId })?.json) };
}

/** Manifest JSON for the panel. */
export function mapPanelStateToProps(state, { windowId }) {
  return { manifest: getManifest(state, { windowId })?.json };
}

const bookDescriptionButtonPlugin = {
  component: BookDescriptionIcon,
  mode: 'add',
  name: BUTTON_PLUGIN_NAME,
  target: 'WindowSideBarButtons',
  config: {
    translations: {
      en: { openCompanionWindow_bookDescription: 'Book description' },
    },
  },
};

const bookDescriptionButtonFilterPlugin = {
  component: SideBarButtonsWrapper,
  mapStateToProps: mapSideBarStateToProps,
  mode: 'wrap',
  name: 'RosaBookDescriptionButtonFilter',
  target: 'WindowSideBarButtons',
};

const bookDescriptionPanelPlugin = {
  companionWindowKey: BOOK_DESCRIPTION_PANEL,
  component: BookDescriptionPanel,
  mapDispatchToProps: { setCanvas },
  mapStateToProps: mapPanelStateToProps,
  name: 'RosaBookDescriptionPanel',
};

/**
 * Use with: Mirador.viewer(config, [...rosaDescriptionPlugin])
 */
export const rosaDescriptionPlugin = [
  bookDescriptionButtonPlugin,
  bookDescriptionButtonFilterPlugin,
  bookDescriptionPanelPlugin,
];

export default rosaDescriptionPlugin;
