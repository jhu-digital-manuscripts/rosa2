/**
 * NameFilter component - pick books by name with a type-ahead over manifest labels.
 *
 * Selections are manifest ids kept in facets.id, so they share the facets' URL
 * encoding (f.id=...) and are cleared with them. Labels and logos for the chips come
 * from the chosen suggestion or, for selections restored from a URL, from one lookup.
 */

import React, { useState, useMemo } from 'react';
import { useSelector, useDispatch } from 'react-redux';
import Autocomplete from '@mui/material/Autocomplete';
import TextField from '@mui/material/TextField';
import Chip from '@mui/material/Chip';
import Box from '@mui/material/Box';
import CircularProgress from '@mui/material/CircularProgress';

import * as selectors from '../state/selectors';
import { setNameLabels, toggleFacet } from '../state/actions';
import {
  useNameSuggestions,
  useNameLabelRestore,
  MIN_SUGGEST_LENGTH,
} from '../hooks/useNameSuggestions';
import { NAME_FACET } from '../utils/queryBuilder';
import { buildLogoUrl } from '../utils/urlBuilder';

const LOGO_SIZE = 20;

/** Collection logo, decorative since the label says which book it is. */
function NameLogo({ logo, logoBaseUrl }) {
  if (!logo) {
    return null;
  }
  return (
    <Box
      component="img"
      src={buildLogoUrl(logo, logoBaseUrl)}
      alt=""
      sx={{ width: LOGO_SIZE, height: LOGO_SIZE, objectFit: 'contain', flexShrink: 0 }}
    />
  );
}

/**
 * NameFilter lets users pick books by typing part of their name.
 */
export function NameFilter() {
  const dispatch = useDispatch();
  const selected = useSelector(selectors.getSelectedNames);
  const logoBaseUrl = useSelector(selectors.getLogoBaseUrl);

  const [inputValue, setInputValue] = useState('');
  const { options: suggestions, loading, error } = useNameSuggestions(inputValue);
  useNameLabelRestore();

  // The selected books are always options too, so MUI can match the value against
  // them; filterSelectedOptions keeps them out of the list.
  const options = useMemo(() => {
    const seen = new Set(selected.map((s) => s.id));
    return [...selected, ...suggestions.filter((s) => !seen.has(s.id))];
  }, [selected, suggestions]);

  const handleChange = (event, newValue) => {
    const before = new Set(selected.map((s) => s.id));
    const after = new Set(newValue.map((s) => s.id));

    const added = newValue.filter((s) => !before.has(s.id));
    if (added.length > 0) {
      dispatch(
        setNameLabels(
          Object.fromEntries(added.map((s) => [s.id, { label: s.label, logo: s.logo }]))
        )
      );
    }
    for (const s of added) {
      dispatch(toggleFacet(NAME_FACET, s.id));
    }
    for (const s of selected) {
      if (!after.has(s.id)) {
        dispatch(toggleFacet(NAME_FACET, s.id));
      }
    }
  };

  const tooShort = inputValue.trim().length < MIN_SUGGEST_LENGTH;
  const noOptionsText = tooShort
    ? `Type at least ${MIN_SUGGEST_LENGTH} characters`
    : error
      ? 'Suggestions are unavailable'
      : 'No matching books';

  return (
    <Autocomplete
      multiple
      size="small"
      id="jhsearch-name-filter"
      options={options}
      value={selected}
      onChange={handleChange}
      inputValue={inputValue}
      onInputChange={(event, value) => setInputValue(value)}
      // Opensearch does the matching
      filterOptions={(x) => x}
      filterSelectedOptions
      getOptionLabel={(option) => option.label}
      isOptionEqualToValue={(option, value) => option.id === value.id}
      loading={loading}
      loadingText="Searching…"
      noOptionsText={noOptionsText}
      renderOption={(props, option) => {
        const { key, ...optionProps } = props;
        return (
          <Box
            component="li"
            key={key}
            {...optionProps}
            sx={{ display: 'flex', alignItems: 'center', gap: 1 }}
          >
            <NameLogo logo={option.logo} logoBaseUrl={logoBaseUrl} />
            {option.label}
          </Box>
        );
      }}
      renderValue={(value, getItemProps) =>
        value.map((option, index) => {
          const { key, ...itemProps } = getItemProps({ index });
          return (
            <Chip
              key={key}
              {...itemProps}
              size="small"
              color="primary"
              variant="outlined"
              label={option.label}
              icon={
                option.logo ? <NameLogo logo={option.logo} logoBaseUrl={logoBaseUrl} /> : undefined
              }
            />
          );
        })
      }
      renderInput={(params) => (
        <TextField
          {...params}
          label="Name"
          placeholder={selected.length === 0 ? 'Type a book name' : undefined}
          InputProps={{
            ...params.InputProps,
            endAdornment: (
              <>
                {loading ? <CircularProgress color="inherit" size={16} /> : null}
                {params.InputProps.endAdornment}
              </>
            ),
          }}
        />
      )}
      sx={{ mb: 1 }}
    />
  );
}

export default NameFilter;
