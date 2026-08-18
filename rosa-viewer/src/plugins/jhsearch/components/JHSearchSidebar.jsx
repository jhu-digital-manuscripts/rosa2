/**
 * JHSearch sidebar component containing search controls and facets.
 */

import React from 'react';
import { useSelector, useDispatch } from 'react-redux';
import Box from '@mui/material/Box';
import Divider from '@mui/material/Divider';
import Button from '@mui/material/Button';
import Tabs from '@mui/material/Tabs';
import Tab from '@mui/material/Tab';
import ClearIcon from '@mui/icons-material/Clear';

import { CollectionSelector } from './CollectionSelector';
import { CategoryFacets } from './CategoryFacets';
import { SimpleSearch } from './SimpleSearch';
import { AdvancedSearch } from './AdvancedSearch';
import { SortOrder } from './SortOrder';

import * as selectors from '../state/selectors';
import { clearSearch, setSearchMode, setViewMode } from '../state/actions';

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
  const searchMode = useSelector(selectors.getSearchMode);
  const simpleQuery = useSelector(selectors.getSimpleQuery);
  const advancedRows = useSelector(selectors.getAdvancedRows);

  const handleClear = () => {
    dispatch(clearSearch());
  };

  const handleTabChange = (event, newValue) => {
    const newMode = newValue === 0 ? 'simple' : 'advanced';
    dispatch(setSearchMode(newMode));

    // Execute search if the new mode has a query
    if (newMode === 'simple' && simpleQuery.trim()) {
      dispatch(setViewMode('search'));
    } else if (newMode === 'advanced') {
      const hasAdvancedQuery = advancedRows.some((row) => row.value && row.value.trim());
      if (hasAdvancedQuery) {
        dispatch(setViewMode('search'));
      } else {
        dispatch(setViewMode('browse'));
      }
    } else {
      dispatch(setViewMode('browse'));
    }
  };

  const showClearButton = hasSearch || hasSelectedFacets;
  const tabValue = searchMode === 'simple' ? 0 : 1;

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
      <Box sx={{ borderBottom: 1, borderColor: 'divider' }}>
        {/* Collection Selector */}
        {showCollectionSelector && (
          <Box sx={{ p: 2, pb: 1 }}>
            <CollectionSelector />
          </Box>
        )}

        {/* Search Mode Tabs */}
        <Tabs
          value={tabValue}
          onChange={handleTabChange}
          aria-label="Search mode"
          variant="fullWidth"
        >
          <Tab label="Basic Search" id="search-tab-0" aria-controls="search-tabpanel-0" />
          <Tab label="Advanced Search" id="search-tab-1" aria-controls="search-tabpanel-1" />
        </Tabs>
      </Box>

      {/* Scrollable content */}
      <Box
        sx={{
          flex: 1,
          overflow: 'auto',
          p: 2,
        }}
      >
        {/* Basic Search Panel */}
        {searchMode === 'simple' && (
          <Box
            role="tabpanel"
            id="search-tabpanel-0"
            aria-labelledby="search-tab-0"
          >
            <SimpleSearch />
          </Box>
        )}

        {/* Advanced Search Panel */}
        {searchMode === 'advanced' && (
          <Box
            role="tabpanel"
            id="search-tabpanel-1"
            aria-labelledby="search-tab-1"
          >
            <AdvancedSearch />
          </Box>
        )}

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
