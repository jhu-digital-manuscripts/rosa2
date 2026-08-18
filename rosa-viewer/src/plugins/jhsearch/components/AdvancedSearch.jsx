/**
 * AdvancedSearch component - multi-field search with boolean operators.
 */

import React, { useState, useEffect } from 'react';
import { useSelector, useDispatch } from 'react-redux';
import Box from '@mui/material/Box';
import Button from '@mui/material/Button';
import IconButton from '@mui/material/IconButton';
import TextField from '@mui/material/TextField';
import Select from '@mui/material/Select';
import MenuItem from '@mui/material/MenuItem';
import FormControl from '@mui/material/FormControl';
import InputLabel from '@mui/material/InputLabel';
import Tooltip from '@mui/material/Tooltip';
import AddIcon from '@mui/icons-material/Add';
import RemoveIcon from '@mui/icons-material/Remove';
import HelpOutlineIcon from '@mui/icons-material/HelpOutline';
import SearchIcon from '@mui/icons-material/Search';

import * as selectors from '../state/selectors';
import {
  setAdvancedRow,
  addAdvancedRow,
  removeAdvancedRow,
  setViewMode,
} from '../state/actions';

/**
 * Single row in the advanced search form.
 */
function AdvancedSearchRow({ row, index, fields, onUpdate, onRemove, onSubmit, showOperator }) {
  const handleFieldChange = (event) => {
    onUpdate(index, { ...row, field: event.target.value });
  };

  const handleValueChange = (event) => {
    onUpdate(index, { ...row, value: event.target.value });
  };

  const handleOperatorChange = (event) => {
    onUpdate(index, { ...row, operator: event.target.value });
  };

  const handleKeyDown = (event) => {
    if (event.key === 'Enter') {
      event.preventDefault();
      onSubmit();
    }
  };

  const fieldDef = fields.find((f) => f.name === row.field);
  const hasValues = fieldDef?.values && fieldDef.values.length > 0;

  return (
    <Box sx={{ display: 'flex', gap: 1, mb: 1, alignItems: 'flex-start' }}>
      {/* Operator (for rows after the first) */}
      {showOperator && (
        <FormControl size="small" sx={{ minWidth: 70 }}>
          <Select
            value={row.operator || 'AND'}
            onChange={handleOperatorChange}
            aria-label="Boolean operator"
          >
            <MenuItem value="AND">AND</MenuItem>
            <MenuItem value="OR">OR</MenuItem>
          </Select>
        </FormControl>
      )}

      {/* Field selector */}
      <FormControl size="small" sx={{ minWidth: 120, flex: 1 }}>
        <InputLabel id={`field-label-${index}`}>Field</InputLabel>
        <Select
          labelId={`field-label-${index}`}
          value={row.field || ''}
          onChange={handleFieldChange}
          label="Field"
        >
          <MenuItem value="">
            <em>Any field</em>
          </MenuItem>
          {fields.map((field) => (
            <MenuItem key={field.name} value={field.name}>
              {field.label}
            </MenuItem>
          ))}
        </Select>
      </FormControl>

      {/* Value input or dropdown */}
      {hasValues ? (
        <FormControl size="small" sx={{ flex: 2 }}>
          <InputLabel id={`value-label-${index}`}>Value</InputLabel>
          <Select
            labelId={`value-label-${index}`}
            value={row.value || ''}
            onChange={handleValueChange}
            label="Value"
          >
            <MenuItem value="">
              <em>Select...</em>
            </MenuItem>
            {fieldDef.values.map((v) => (
              <MenuItem key={v.value} value={v.value}>
                {v.label}
              </MenuItem>
            ))}
          </Select>
        </FormControl>
      ) : (
        <TextField
          size="small"
          placeholder="Search term..."
          value={row.value || ''}
          onChange={handleValueChange}
          onKeyDown={handleKeyDown}
          sx={{ flex: 2 }}
        />
      )}

      {/* Field help */}
      {fieldDef?.description && (
        <Tooltip title={fieldDef.description} placement="top">
          <IconButton size="small" aria-label="Field help">
            <HelpOutlineIcon fontSize="small" />
          </IconButton>
        </Tooltip>
      )}

      {/* Remove row button */}
      <IconButton
        size="small"
        onClick={() => onRemove(index)}
        aria-label="Remove search row"
      >
        <RemoveIcon fontSize="small" />
      </IconButton>
    </Box>
  );
}

/**
 * AdvancedSearch provides field-specific search with boolean operators.
 * Uses local state for input values and only commits to Redux on submit.
 */
export function AdvancedSearch() {
  const dispatch = useDispatch();

  const reduxRows = useSelector(selectors.getAdvancedRows);
  const fields = useSelector(selectors.getFields);

  // Local state for form values - only synced to Redux on submit
  const [localRows, setLocalRows] = useState(reduxRows);

  // Sync local state when Redux state changes externally (e.g., clearSearch)
  useEffect(() => {
    setLocalRows(reduxRows);
  }, [reduxRows]);

  const handleUpdateRow = (index, row) => {
    const newRows = [...localRows];
    newRows[index] = row;
    setLocalRows(newRows);
  };

  const handleAddRow = () => {
    setLocalRows([...localRows, { field: '', value: '', operator: 'AND' }]);
  };

  const handleRemoveRow = (index) => {
    const newRows = localRows.filter((_, i) => i !== index);
    // Ensure at least one row remains
    if (newRows.length === 0) {
      newRows.push({ field: '', value: '', operator: 'AND' });
    }
    setLocalRows(newRows);
  };

  const handleSearch = () => {
    // Commit local state to Redux
    localRows.forEach((row, index) => {
      dispatch(setAdvancedRow(index, row));
    });

    // Handle removed rows - if local has fewer rows than redux
    while (reduxRows.length > localRows.length) {
      dispatch(removeAdvancedRow(localRows.length));
    }

    // Handle added rows - if local has more rows than redux
    for (let i = reduxRows.length; i < localRows.length; i++) {
      dispatch(addAdvancedRow(localRows[i]));
    }

    // Check if any row has a value and set view mode
    const hasValue = localRows.some((row) => row.value && row.value.trim());
    if (hasValue) {
      dispatch(setViewMode('search'));
    } else {
      dispatch(setViewMode('browse'));
    }
  };

  return (
    <Box>
      {/* Search rows */}
      {localRows.map((row, index) => (
        <AdvancedSearchRow
          key={index}
          row={row}
          index={index}
          fields={fields}
          onUpdate={handleUpdateRow}
          onRemove={handleRemoveRow}
          onSubmit={handleSearch}
          showOperator={index > 0}
        />
      ))}

      {/* Add row and Search buttons */}
      <Box sx={{ display: 'flex', gap: 1, mt: 1 }}>
        <Button
          size="small"
          startIcon={<AddIcon />}
          onClick={handleAddRow}
          sx={{ textTransform: 'none' }}
        >
          Add Field
        </Button>

        <Button
          size="small"
          variant="contained"
          startIcon={<SearchIcon />}
          onClick={handleSearch}
          sx={{ ml: 'auto' }}
        >
          Search
        </Button>
      </Box>
    </Box>
  );
}

export default AdvancedSearch;
