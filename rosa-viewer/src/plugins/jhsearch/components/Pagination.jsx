/**
 * Pagination component - controls for navigating search results pages.
 */

import React from 'react';
import { useSelector, useDispatch } from 'react-redux';
import Box from '@mui/material/Box';
import Button from '@mui/material/Button';
import IconButton from '@mui/material/IconButton';
import Typography from '@mui/material/Typography';
import Select from '@mui/material/Select';
import MenuItem from '@mui/material/MenuItem';
import FormControl from '@mui/material/FormControl';
import FirstPageIcon from '@mui/icons-material/FirstPage';
import LastPageIcon from '@mui/icons-material/LastPage';
import ChevronLeftIcon from '@mui/icons-material/ChevronLeft';
import ChevronRightIcon from '@mui/icons-material/ChevronRight';

import * as selectors from '../state/selectors';
import { setPage, setPageSize } from '../state/actions';

/**
 * Pagination provides navigation controls for results.
 */
export function Pagination() {
  const dispatch = useDispatch();

  const currentPage = useSelector(selectors.getCurrentPage);
  const pageSize = useSelector(selectors.getPageSize);
  const totalResults = useSelector(selectors.getTotalResults);
  const totalPages = useSelector(selectors.getTotalPages);
  const pageSizeOptions = useSelector(selectors.getPageSizeOptions);

  const canGoPrev = currentPage > 0;
  const canGoNext = currentPage < totalPages - 1;

  const startResult = currentPage * pageSize + 1;
  const endResult = Math.min((currentPage + 1) * pageSize, totalResults);

  const handleFirstPage = () => {
    dispatch(setPage(0));
    scrollToTop();
  };

  const handlePrevPage = () => {
    dispatch(setPage(currentPage - 1));
    scrollToTop();
  };

  const handleNextPage = () => {
    dispatch(setPage(currentPage + 1));
    scrollToTop();
  };

  const handleLastPage = () => {
    dispatch(setPage(totalPages - 1));
    scrollToTop();
  };

  const handlePageSizeChange = (event) => {
    dispatch(setPageSize(event.target.value));
    scrollToTop();
  };

  const scrollToTop = () => {
    // Find the results container and scroll to top
    const resultsContainer = document.querySelector('[data-results-container]');
    if (resultsContainer) {
      resultsContainer.scrollTop = 0;
    }
  };

  if (totalResults === 0) {
    return null;
  }

  return (
    <Box
      sx={{
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'space-between',
        flexWrap: 'wrap',
        gap: 2,
        py: 1,
        px: 2,
        borderTop: 1,
        borderColor: 'divider',
        bgcolor: 'background.paper',
      }}
    >
      {/* Results count and page size selector */}
      <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
        <Typography variant="body2" color="text.secondary">
          {startResult}–{endResult} of {totalResults.toLocaleString()}
        </Typography>
        
        <Typography variant="body2" color="text.secondary" sx={{ mx: 1 }}>
          |
        </Typography>
        
        <Typography variant="body2" color="text.secondary">
          Show:
        </Typography>
        <FormControl size="small" variant="standard">
          <Select
            value={pageSize}
            onChange={handlePageSizeChange}
            aria-label="Results per page"
            sx={{ minWidth: 60 }}
          >
            {pageSizeOptions.map((size) => (
              <MenuItem key={size} value={size}>
                {size}
              </MenuItem>
            ))}
          </Select>
        </FormControl>
      </Box>

      {/* Page navigation */}
      <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.5 }}>
        <IconButton
          size="small"
          onClick={handleFirstPage}
          disabled={!canGoPrev}
          aria-label="First page"
        >
          <FirstPageIcon />
        </IconButton>
        
        <IconButton
          size="small"
          onClick={handlePrevPage}
          disabled={!canGoPrev}
          aria-label="Previous page"
        >
          <ChevronLeftIcon />
        </IconButton>

        <Typography variant="body2" sx={{ px: 1 }}>
          Page {currentPage + 1} of {totalPages}
        </Typography>

        <IconButton
          size="small"
          onClick={handleNextPage}
          disabled={!canGoNext}
          aria-label="Next page"
        >
          <ChevronRightIcon />
        </IconButton>
        
        <IconButton
          size="small"
          onClick={handleLastPage}
          disabled={!canGoNext}
          aria-label="Last page"
        >
          <LastPageIcon />
        </IconButton>
      </Box>
    </Box>
  );
}

export default Pagination;
