/**
 * JHSearch sidebar component containing search controls and facets.
 */

import React, { useState, useCallback, useRef, useEffect } from 'react';
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
import { rowHasQuery } from '../utils/queryBuilder';

const MIN_WIDTH = 300;
const DEFAULT_WIDTH = 450;

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

  // Resize state
  const [width, setWidth] = useState(DEFAULT_WIDTH);
  const [isResizing, setIsResizing] = useState(false);
  const sidebarRef = useRef(null);

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
      const hasAdvancedQuery = advancedRows.some(rowHasQuery);
      if (hasAdvancedQuery) {
        dispatch(setViewMode('search'));
      } else {
        dispatch(setViewMode('browse'));
      }
    } else {
      dispatch(setViewMode('browse'));
    }
  };

  // Handle mouse down on resize handle
  const handleMouseDown = useCallback((e) => {
    e.preventDefault();
    setIsResizing(true);
  }, []);

  // Handle mouse move during resize
  useEffect(() => {
    const handleMouseMove = (e) => {
      if (!isResizing || !sidebarRef.current) return;

      const sidebarRect = sidebarRef.current.getBoundingClientRect();
      const newWidth = e.clientX - sidebarRect.left;

      if (newWidth >= MIN_WIDTH) {
        setWidth(newWidth);
      }
    };

    const handleMouseUp = () => {
      setIsResizing(false);
    };

    if (isResizing) {
      document.addEventListener('mousemove', handleMouseMove);
      document.addEventListener('mouseup', handleMouseUp);
      // Prevent text selection during resize
      document.body.style.userSelect = 'none';
      document.body.style.cursor = 'col-resize';
    }

    return () => {
      document.removeEventListener('mousemove', handleMouseMove);
      document.removeEventListener('mouseup', handleMouseUp);
      document.body.style.userSelect = '';
      document.body.style.cursor = '';
    };
  }, [isResizing]);

  const showClearButton = hasSearch || hasSelectedFacets;
  const tabValue = searchMode === 'simple' ? 0 : 1;

  // From md up a resizable column beside the results. Below md the full width
  // above them at its content height, with no resize handle.
  return (
    <Box
      ref={sidebarRef}
      component="aside"
      sx={{
        width: { xs: '100%', md: width },
        minWidth: { xs: 0, md: MIN_WIDTH },
        flexShrink: 0,
        display: 'flex',
        flexDirection: 'column',
        height: { xs: 'auto', md: '100%' },
        overflow: { xs: 'visible', md: 'hidden' },
        position: 'relative',
      }}
      role="complementary"
      aria-label="Search controls"
    >
      {/* Main content area */}
      <Box
        sx={{
          flex: 1,
          display: 'flex',
          flexDirection: 'column',
          overflow: { xs: 'visible', md: 'hidden' },
          borderRight: { xs: 0, md: 1 },
          borderBottom: { xs: 1, md: 0 },
          borderColor: 'divider',
        }}
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
            overflow: { xs: 'visible', md: 'auto' },
            p: 2,
          }}
        >
          {/* Basic Search Panel */}
          {searchMode === 'simple' && (
            <Box role="tabpanel" id="search-tabpanel-0" aria-labelledby="search-tab-0">
              <SimpleSearch />
            </Box>
          )}

          {/* Advanced Search Panel */}
          {searchMode === 'advanced' && (
            <Box role="tabpanel" id="search-tabpanel-1" aria-labelledby="search-tab-1">
              <AdvancedSearch />
            </Box>
          )}

          <Divider sx={{ my: 2 }} />

          {/* Facets */}
          {enableFacets && (
            <>
              <CategoryFacets />
              <Divider sx={{ my: 2 }} />
            </>
          )}

          {/* Sort Order, below the filters */}
          <SortOrder />
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

      {/* Resize handle */}
      <Box
        onMouseDown={handleMouseDown}
        sx={{
          display: { xs: 'none', md: 'block' },
          position: 'absolute',
          top: 0,
          right: 0,
          width: 4,
          height: '100%',
          cursor: 'col-resize',
          backgroundColor: 'transparent',
          transition: 'background-color 0.2s',
          '&:hover': {
            backgroundColor: 'action.hover',
          },
          ...(isResizing && {
            backgroundColor: 'primary.main',
          }),
        }}
        role="separator"
        aria-orientation="vertical"
        aria-label="Resize sidebar"
      />
    </Box>
  );
}

export default JHSearchSidebar;
