/**
 * JHSearch sidebar component containing search controls and facets.
 */

import React from 'react';
import { useSelector, useDispatch } from 'react-redux';
import Box from '@mui/material/Box';
import Divider from '@mui/material/Divider';
import Button from '@mui/material/Button';
import ClearIcon from '@mui/icons-material/Clear';

import { CollectionSelector } from './CollectionSelector';
import { CategoryFacets } from './CategoryFacets';
import { SimpleSearch } from './SimpleSearch';
import { AdvancedSearch } from './AdvancedSearch';
import { SortOrder } from './SortOrder';

import * as selectors from '../state/selectors';
import { clearSearch } from '../state/actions';

/**
 * JHSearchSidebar provides all search and browse controls.
 */
export function JHSearchSidebar() {
  const dispatch = useDispatch();

  const enableFacets = useSelector(selectors.areFacetsEnabled);
  const hasSearch = useSelector(selectors.hasActiveSearch);
  const hasSelectedFacets = useSelector(selectors.hasSelectedFacets);
  const collectionItems = useSelector(selectors.getCollectionItems);
  const showCollectionSelector = collectionItems.length > 1;

  const handleClear = () => {
    dispatch(clearSearch());
  };

  const showClearButton = hasSearch || hasSelectedFacets;

  return (
    <Box
      component="aside"
      sx={{
        width: 300,
        flexShrink: 0,
        borderRight: 1,
        borderColor: 'divider',
        display: 'flex',
        flexDirection: 'column',
        height: '100%',
        overflow: 'hidden',
      }}
      role="complementary"
      aria-label="Search controls"
    >
      {/* Fixed header section */}
      <Box sx={{ p: 2, borderBottom: 1, borderColor: 'divider' }}>
        {/* Collection Selector */}
        {showCollectionSelector && (
          <Box sx={{ mb: 2 }}>
            <CollectionSelector />
          </Box>
        )}

        {/* Simple Search */}
        <SimpleSearch />
      </Box>

      {/* Scrollable content */}
      <Box
        sx={{
          flex: 1,
          overflow: 'auto',
          p: 2,
        }}
      >
        {/* Advanced Search */}
        <AdvancedSearch />

        <Divider sx={{ my: 2 }} />

        {/* Sort Order */}
        <SortOrder />

        {/* Facets */}
        {enableFacets && (
          <>
            <Divider sx={{ my: 2 }} />
            <CategoryFacets />
          </>
        )}
      </Box>

      {/* Clear button - fixed at bottom */}
      {showClearButton && (
        <Box sx={{ p: 2, borderTop: 1, borderColor: 'divider' }}>
          <Button
            variant="outlined"
            fullWidth
            startIcon={<ClearIcon />}
            onClick={handleClear}
            aria-label="Clear search and filters"
          >
            Clear All
          </Button>
        </Box>
      )}
    </Box>
  );
}

export default JHSearchSidebar;
