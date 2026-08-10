/**
 * useCollections hook - access and manage collection selection.
 */

import { useCallback } from 'react';
import { useSelector, useDispatch } from 'react-redux';
import * as selectors from '../state/selectors';
import { selectCollection } from '../state/actions';

/**
 * Hook for managing collection selection.
 *
 * @returns {Object} Collection state and methods
 */
export function useCollections() {
  const dispatch = useDispatch();

  const collections = useSelector(selectors.getCollectionItems);
  const selectedCollectionId = useSelector(selectors.getSelectedCollectionId);
  const collectionsState = useSelector(selectors.getCollections);
  const config = useSelector(selectors.getConfig);

  // Find the currently selected collection
  const selectedCollection =
    collections.find((c) => c.id === selectedCollectionId) || null;

  /**
   * Select a collection by ID.
   */
  const select = useCallback(
    (collectionId) => {
      dispatch(selectCollection(collectionId));
    },
    [dispatch]
  );

  /**
   * Check if a collection has children.
   */
  const hasChildren = useCallback(
    (collectionId) => {
      const collection = collections.find((c) => c.id === collectionId);
      return collection?.childIds?.length > 0;
    },
    [collections]
  );

  /**
   * Get child collections for a given collection.
   */
  const getChildren = useCallback(
    (collectionId) => {
      const collection = collections.find((c) => c.id === collectionId);
      if (!collection?.childIds) {
        return [];
      }
      return collection.childIds
        .map((id) => collections.find((c) => c.id === id))
        .filter(Boolean);
    },
    [collections]
  );

  return {
    // State
    collections,
    selectedCollectionId,
    selectedCollection,
    isLoading: collectionsState.loading,
    error: collectionsState.error,
    hasMultipleCollections: collections.length > 1,

    // Methods
    select,
    hasChildren,
    getChildren,
  };
}

export default useCollections;
