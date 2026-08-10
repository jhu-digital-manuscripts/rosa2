/**
 * SimpleSearch component - single text input for searching across default fields.
 */

import React, { useState, useCallback } from 'react';
import { useSelector, useDispatch } from 'react-redux';
import TextField from '@mui/material/TextField';
import InputAdornment from '@mui/material/InputAdornment';
import IconButton from '@mui/material/IconButton';
import SearchIcon from '@mui/icons-material/Search';
import ClearIcon from '@mui/icons-material/Clear';

import * as selectors from '../state/selectors';
import { setSimpleQuery, setSearchMode, setViewMode } from '../state/actions';

/**
 * SimpleSearch provides a single search input for quick searching.
 */
export function SimpleSearch() {
  const dispatch = useDispatch();
  const currentQuery = useSelector(selectors.getSimpleQuery);
  const searchMode = useSelector(selectors.getSearchMode);

  // Local state for input value (debounced submit)
  const [inputValue, setInputValue] = useState(currentQuery);

  const handleInputChange = (event) => {
    setInputValue(event.target.value);
  };

  const handleSubmit = useCallback(
    (event) => {
      if (event) {
        event.preventDefault();
      }
      
      // Switch to simple search mode if needed
      if (searchMode !== 'simple') {
        dispatch(setSearchMode('simple'));
      }
      
      if (inputValue.trim()) {
        dispatch(setViewMode('search'));
      } else {
        dispatch(setViewMode('browse'));
      }
      
      dispatch(setSimpleQuery(inputValue.trim()));
    },
    [dispatch, inputValue, searchMode]
  );

  const handleClear = () => {
    setInputValue('');
    dispatch(setSimpleQuery(''));
    dispatch(setViewMode('browse'));
  };

  const handleKeyDown = (event) => {
    if (event.key === 'Enter') {
      handleSubmit(event);
    }
  };

  return (
    <form onSubmit={handleSubmit}>
      <TextField
        fullWidth
        size="small"
        placeholder="Search collections..."
        value={inputValue}
        onChange={handleInputChange}
        onKeyDown={handleKeyDown}
        aria-label="Search"
        InputProps={{
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
              <IconButton
                size="small"
                onClick={handleClear}
                aria-label="Clear search"
                edge="end"
              >
                <ClearIcon />
              </IconButton>
            </InputAdornment>
          ) : null,
        }}
      />
    </form>
  );
}

export default SimpleSearch;
