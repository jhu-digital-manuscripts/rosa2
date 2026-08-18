/**
 * SortOrder component - dropdown for selecting result sort order.
 */

import React, { useEffect } from 'react';
import { useSelector, useDispatch } from 'react-redux';
import Box from '@mui/material/Box';
import FormControl from '@mui/material/FormControl';
import Select from '@mui/material/Select';
import MenuItem from '@mui/material/MenuItem';
import Typography from '@mui/material/Typography';

import * as selectors from '../state/selectors';
import { setSort } from '../state/actions';

/**
 * Sort options available to the user.
 * - Browse mode: Title options only (manifests don't have page_num, relevance needs a query)
 * - Search mode: Relevance and Page Order only
 */
const SORT_OPTIONS = [
  { value: '_score:desc', label: 'Relevance', field: '_score', order: 'desc', modes: ['search'] },
  { value: 'label.keyword:asc', label: 'Title (A-Z)', field: 'label.keyword', order: 'asc', modes: ['browse'] },
  { value: 'label.keyword:desc', label: 'Title (Z-A)', field: 'label.keyword', order: 'desc', modes: ['browse'] },
  { value: 'page_num:asc', label: 'Page Order', field: 'page_num', order: 'asc', modes: ['search'] },
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
  const availableOptions = SORT_OPTIONS.filter((option) => option.modes.includes(viewMode));

  // If current sort is not in available options, switch to default for this mode
  // This effect is a fallback; the reducer should set the default when mode changes
  useEffect(() => {
    const isCurrentValueAvailable = availableOptions.some(
      (option) => option.value === currentValue
    );
    
    if (!isCurrentValueAvailable && availableOptions.length > 0) {
      const defaultOption = availableOptions[0];
      dispatch(setSort(defaultOption.field, defaultOption.order));
    }
  }, [viewMode, currentValue, availableOptions, dispatch]);

  const handleChange = (event) => {
    const [field, order] = event.target.value.split(':');
    dispatch(setSort(field, order));
  };

  // Only render if current value is in available options (prevents MUI warning)
  const isValueAvailable = availableOptions.some(
    (option) => option.value === currentValue
  );

  return (
    <Box>
      <Typography variant="body2" color="text.secondary" sx={{ mb: 1 }}>
        Sort By
      </Typography>
      <FormControl fullWidth size="small">
        <Select
          value={isValueAvailable ? currentValue : ''}
          onChange={handleChange}
          aria-label="Sort order"
          displayEmpty
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
