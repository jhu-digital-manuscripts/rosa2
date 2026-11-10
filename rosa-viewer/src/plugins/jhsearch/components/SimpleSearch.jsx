/**
 * SimpleSearch component - single text input for searching across default fields.
 */

import React, { useState, useCallback, useEffect } from 'react';
import { useSelector, useDispatch } from 'react-redux';
import TextField from '@mui/material/TextField';
import InputAdornment from '@mui/material/InputAdornment';
import IconButton from '@mui/material/IconButton';
import SearchIcon from '@mui/icons-material/Search';
import ClearIcon from '@mui/icons-material/Clear';

import * as selectors from '../state/selectors';
import { setSimpleQuery, setViewMode } from '../state/actions';

/**
 * SimpleSearch provides a single search input for quick searching.
 */
export function SimpleSearch() {
  const dispatch = useDispatch();
  const currentQuery = useSelector(selectors.getSimpleQuery);

  // Local state for the input; the query is only committed on submit
  const [inputValue, setInputValue] = useState(currentQuery);

  // Sync the input when the query changes elsewhere (e.g. browser back/forward)
  useEffect(() => {
    setInputValue(currentQuery);
  }, [currentQuery]);

  const handleInputChange = (event) => {
    setInputValue(event.target.value);
  };

  const handleSubmit = useCallback(
    (event) => {
      if (event) {
        event.preventDefault();
      }

      const query = inputValue.trim();
      dispatch(setSimpleQuery(query));
      // Also sets the default sort for the mode
      dispatch(setViewMode(query ? 'search' : 'browse'));
    },
    [dispatch, inputValue]
  );

  const handleClear = () => {
    setInputValue('');
    dispatch(setSimpleQuery(''));
    dispatch(setViewMode('browse'));
  };

  return (
    <form onSubmit={handleSubmit}>
      <TextField
        fullWidth
        size="small"
        placeholder='Search collections ("quotes" for a phrase)...'
        value={inputValue}
        onChange={handleInputChange}
        aria-label="Search"
        slotProps={{
          input: {
            startAdornment: (
              <InputAdornment position="start">
                <IconButton
                  size="small"
                  onClick={handleSubmit}
                  aria-label="Submit search"
                  edge="start"
                >
                  <SearchIcon />
                </IconButton>
              </InputAdornment>
            ),
            endAdornment: inputValue ? (
              <InputAdornment position="end">
                <IconButton size="small" onClick={handleClear} aria-label="Clear search" edge="end">
                  <ClearIcon />
                </IconButton>
              </InputAdornment>
            ) : null,
          },
        }}
      />
    </form>
  );
}

export default SimpleSearch;
