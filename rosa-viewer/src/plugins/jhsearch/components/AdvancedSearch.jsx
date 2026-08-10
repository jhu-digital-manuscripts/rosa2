/**
 * AdvancedSearch component - multi-field search with boolean operators.
 */

import React, { useState } from 'react';
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
import Collapse from '@mui/material/Collapse';
import Typography from '@mui/material/Typography';
import AddIcon from '@mui/icons-material/Add';
import RemoveIcon from '@mui/icons-material/Remove';
import ExpandMoreIcon from '@mui/icons-material/ExpandMore';
import ExpandLessIcon from '@mui/icons-material/ExpandLess';
import HelpOutlineIcon from '@mui/icons-material/HelpOutline';
import SearchIcon from '@mui/icons-material/Search';

import * as selectors from '../state/selectors';
import {
  setAdvancedRow,
  addAdvancedRow,
  removeAdvancedRow,
  setSearchMode,
  setViewMode,
} from '../state/actions';

/**
 * Single row in the advanced search form.
 */
function AdvancedSearchRow({ row, index, fields, onUpdate, onRemove, showOperator }) {
  const handleFieldChange = (event) => {
    onUpdate(index, { ...row, field: event.target.value });
  };

  const handleValueChange = (event) => {
    onUpdate(index, { ...row, value: event.target.value });
  };

  const handleOperatorChange = (event) => {
    onUpdate(index, { ...row, operator: event.target.value });
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
 */
export function AdvancedSearch() {
  const dispatch = useDispatch();
  const [expanded, setExpanded] = useState(false);

  const rows = useSelector(selectors.getAdvancedRows);
  const fields = useSelector(selectors.getFields);
  const searchMode = useSelector(selectors.getSearchMode);

  const handleToggle = () => {
    setExpanded(!expanded);
  };

  const handleUpdateRow = (index, row) => {
    dispatch(setAdvancedRow(index, row));
  };

  const handleAddRow = () => {
    dispatch(addAdvancedRow());
  };

  const handleRemoveRow = (index) => {
    dispatch(removeAdvancedRow(index));
  };

  const handleSearch = () => {
    // Switch to advanced search mode
    if (searchMode !== 'advanced') {
      dispatch(setSearchMode('advanced'));
    }

    // Check if any row has a value
    const hasValue = rows.some((row) => row.value && row.value.trim());
    if (hasValue) {
      dispatch(setViewMode('search'));
    } else {
      dispatch(setViewMode('browse'));
    }
  };

  return (
    <Box>
      {/* Toggle button */}
      <Button
        fullWidth
        variant="text"
        onClick={handleToggle}
        endIcon={expanded ? <ExpandLessIcon /> : <ExpandMoreIcon />}
        sx={{ justifyContent: 'space-between', textTransform: 'none' }}
      >
        <Typography variant="body2">Advanced Search</Typography>
      </Button>

      {/* Collapsible content */}
      <Collapse in={expanded}>
        <Box sx={{ pt: 1 }}>
          {/* Search rows */}
          {rows.map((row, index) => (
            <AdvancedSearchRow
              key={index}
              row={row}
              index={index}
              fields={fields}
              onUpdate={handleUpdateRow}
              onRemove={handleRemoveRow}
              showOperator={index > 0}
            />
          ))}

          {/* Add row button */}
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
      </Collapse>
    </Box>
  );
}

export default AdvancedSearch;
