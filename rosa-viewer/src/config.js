/**
 * Default configuration for the JHSearch plugin.
 * These values are used when not explicitly provided in Mirador config.
 */
export const defaultJHSearchConfig = {
  // Default thumbnail URL template
  // Placeholders: {imageBaseUrl}, {iiifImageId}, {width}
  thumbnailTemplate: '{imageBaseUrl}/{iiifImageId}/full/{width},/0/default.jpg',

  // Default thumbnail width in pixels
  thumbnailWidth: 80,

  // Default base URL for collection logos
  logoBaseUrl: '/logo',

  // Enable faceted browsing by default
  enableFacets: true,

  // Default page size for results
  pageSize: 20,

  // Available page size options
  pageSizeOptions: [20, 30, 40, 50],
};

/**
 * Default Mirador configuration for rosa-viewer.
 */
export const defaultMiradorConfig = {
  id: 'rosa-viewer',
  window: {
    allowClose: true,
    allowFullscreen: true,
    allowMaximize: true,
    defaultView: 'single',
    panels: {
      info: true,
      annotations: true,
      canvas: true,
    },
  },
  workspace: {
    showZoomControls: true,
    type: 'mosaic',
  },
  workspaceControlPanel: {
    enabled: true,
  },
};

/**
 * Merge user config with defaults.
 * @param {Object} userConfig - User-provided configuration
 * @returns {Object} Merged configuration
 */
export function mergeConfig(userConfig = {}) {
  const jhsearch = {
    ...defaultJHSearchConfig,
    ...userConfig.jhsearch,
  };

  return {
    ...defaultMiradorConfig,
    ...userConfig,
    jhsearch,
  };
}
