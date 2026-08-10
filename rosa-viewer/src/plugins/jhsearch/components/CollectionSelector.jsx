/**
 * CollectionSelector component - dropdown for selecting which collection to browse/search.
 */

import React from 'react';
import { useSelector, useDispatch } from 'react-redux';
import Box from '@mui/material/Box';
import FormControl from '@mui/material/FormControl';
import InputLabel from '@mui/material/InputLabel';
import Select from '@mui/material/Select';
import MenuItem from '@mui/material/MenuItem';
import ListItemIcon from '@mui/material/ListItemIcon';
import ListItemText from '@mui/material/ListItemText';
import Avatar from '@mui/material/Avatar';

import * as selectors from '../state/selectors';
import { selectCollection } from '../state/actions';
import { buildLogoUrl } from '../utils/urlBuilder';

/**
 * CollectionSelector allows users to choose which collection to browse.
 */
export function CollectionSelector() {
  const dispatch = useDispatch();

  const collections = useSelector(selectors.getCollectionItems);
  const selectedCollectionId = useSelector(selectors.getSelectedCollectionId);
  const config = useSelector(selectors.getConfig);
  const logoBaseUrl = config.logoBaseUrl || '/logo';

  const handleChange = (event) => {
    dispatch(selectCollection(event.target.value));
  };

  // Don't render if there's only one or no collections
  if (!collections || collections.length <= 1) {
    return null;
  }

  return (
    <FormControl fullWidth size="small">
      <InputLabel id="collection-selector-label">Collection</InputLabel>
      <Select
        labelId="collection-selector-label"
        id="collection-selector"
        value={selectedCollectionId || ''}
        onChange={handleChange}
        label="Collection"
        renderValue={(value) => {
          const selected = collections.find((c) => c.id === value);
          return selected?.label || value;
        }}
      >
        {collections.map((collection) => (
          <MenuItem key={collection.id} value={collection.id}>
            {collection.logo && (
              <ListItemIcon>
                <Avatar
                  src={buildLogoUrl(collection.logo, logoBaseUrl)}
                  alt=""
                  sx={{ width: 24, height: 24 }}
                />
              </ListItemIcon>
            )}
            <ListItemText primary={collection.label || collection.id} />
          </MenuItem>
        ))}
      </Select>
    </FormControl>
  );
}

export default CollectionSelector;
