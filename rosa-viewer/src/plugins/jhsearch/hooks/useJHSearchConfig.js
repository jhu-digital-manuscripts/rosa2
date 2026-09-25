/**
 * useJHSearchConfig hook - access JHSearch configuration from Redux state.
 */

import { useSelector } from 'react-redux';
import * as selectors from '../state/selectors';

/**
 * Hook to access the JHSearch configuration.
 *
 * @returns {Object} Configuration object with various properties
 */
export function useJHSearchConfig() {
  const config = useSelector(selectors.getConfig);
  const service = useSelector(selectors.getService);
  const isInitialized = useSelector(selectors.isInitialized);
  const error = useSelector(selectors.getInitializationError);

  return {
    // Configuration state
    isInitialized,
    isLoading: config.loading || service.loading,
    error,

    // Plugin config
    iiifBaseUrl: config.iiifBaseUrl,
    collectionId: config.collectionId,
    childCollectionIds: config.childCollectionIds,
    imageBaseUrl: config.imageBaseUrl,
    thumbnailTemplate: config.thumbnailTemplate,
    thumbnailWidth: config.thumbnailWidth,
    logoBaseUrl: config.logoBaseUrl,
    enableFacets: config.enableFacets,
    pageSize: config.pageSize,
    pageSizeOptions: config.pageSizeOptions,

    // Service config
    fields: service.fields,
    categories: service.categories,
    defaultFields: service.defaultFields,
    opensearchUrl: service.opensearchUrl,

    // Thumbnail config object for buildThumbnailUrl
    thumbnailConfig: {
      imageBaseUrl: config.imageBaseUrl,
      thumbnailTemplate: config.thumbnailTemplate,
      thumbnailWidth: config.thumbnailWidth,
      logoBaseUrl: config.logoBaseUrl,
    },
  };
}

export default useJHSearchConfig;
