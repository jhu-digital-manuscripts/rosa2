/**
 * Utilities for the list of collections shown in the collection selector.
 */

/**
 * Build the collection items for the selector from the plugin config.
 *
 * Without `collectionLabels`, each collection is shown by its id with no logo.
 * A collection with a configured label is shown with that label and the logo
 * `{id}.jpg` from `logoBaseUrl`.
 *
 * @param {Object} config - JHSearch plugin config
 * @param {string} config.collectionId - Top-level collection id
 * @param {string[]} [config.childCollectionIds] - Child collection ids
 * @param {Object<string, string>} [config.collectionLabels] - Display labels by collection id
 * @returns {Array<{id: string, label: string, logo?: string}>}
 */
export function buildCollectionItems(config) {
  const labels = config.collectionLabels || {};
  const ids = [config.collectionId, ...(config.childCollectionIds || [])];

  return ids.map((id) => {
    const label = labels[id];
    return label ? { id, label, logo: `${id}.jpg` } : { id, label: id };
  });
}
