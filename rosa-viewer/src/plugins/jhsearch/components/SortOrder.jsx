/**
 * SortOrder component - dropdown for selecting result sort order.
 */

import React from 'react';
import { useSelector, useDispatch } from 'react-redux';
import Box from '@mui/material/Box';
import FormControl from '@mui/material/FormControl';
import InputLabel from '@mui/material/InputLabel';
import Select from '@mui/material/Select';
import MenuItem from '@mui/material/MenuItem';
import Typography from '@mui/material/Typography';

import * as selectors from '../state/selectors';
import { setSort } from '../state/actions';

/**
 * Sort options available to the user.
 */
const SORT_OPTIONS = [
  { value: '_score:desc', label: 'Relevance', field: '_score', order: 'desc' },
  { value: 'label.keyword:asc', label: 'Title (A-Z)', field: 'label.keyword', order: 'asc' },
  { value: 'label.keyword:desc', label: 'Title (Z-A)', field: 'label.keyword', order: 'desc' },
  { value: 'page_num:asc', label: 'Page Order', field: 'page_num', order: 'asc' },
];

/**
 * SortOrder allows users to select how results are sorted.
 */
export function SortOrder() {
  const dispatch = useDispatch();

  const sortField = useSelector(selectors.getSortField);
  const sortOrder = useSelector(selectors.getSortOrder);
  const viewMode = useSelector(selectors.getViewMode);

  // Combine field and order into a single value for the select
  const currentValue = `${sortField}:${sortOrder}`;

  // Filter options based on view mode
  // Relevance only makes sense when searching
  // Page Order only makes sense for canvas results in search mode
  const availableOptions = SORT_OPTIONS.filter((option) => {
    if (option.field === '_score' && viewMode !== 'search') {
      return false;
    }
    return true;
  });

  const handleChange = (event) => {
    const [field, order] = event.target.value.split(':');
    dispatch(setSort(field, order));
  };

  return (
    <Box>
      <Typography variant="body2" color="text.secondary" sx={{ mb: 1 }}>
        Sort By
      </Typography>
      <FormControl fullWidth size="small">
        <Select
          value={currentValue}
          onChange={handleChange}
          aria-label="Sort order"
        >
          {availableOptions.map((option) => (
            <MenuItem key={option.value} value={option.value}>
              {option.label}
            </MenuItem>
          ))}
        </Select>
      </FormControl>
    </Box>
  );
}

export default SortOrder;
