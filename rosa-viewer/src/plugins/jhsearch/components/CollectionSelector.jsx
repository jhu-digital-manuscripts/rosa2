/**
 * CollectionSelector component - dropdown for selecting which collection to browse/search.
 */

import React from 'react';
import { useSelector, useDispatch } from 'react-redux';
import FormControl from '@mui/material/FormControl';
import InputLabel from '@mui/material/InputLabel';
import Select from '@mui/material/Select';
import MenuItem from '@mui/material/MenuItem';
import ListItemIcon from '@mui/material/ListItemIcon';
import ListItemText from '@mui/material/ListItemText';
import Box from '@mui/material/Box';

import * as selectors from '../state/selectors';
import { selectCollection } from '../state/actions';
import { buildLogoUrl } from '../utils/urlBuilder';

const LOGO_SIZE = 28;

/**
 * Collection logo, scaled to fit a fixed square since the logo images differ in
 * size and aspect ratio.
 */
function CollectionLogo({ src }) {
  return (
    <Box
      component="img"
      src={src}
      alt=""
      sx={{ width: LOGO_SIZE, height: LOGO_SIZE, objectFit: 'contain', flexShrink: 0 }}
    />
  );
}

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
          return (
            <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
              {selected?.logo && <CollectionLogo src={buildLogoUrl(selected.logo, logoBaseUrl)} />}
              <Box component="span" sx={{ overflow: 'hidden', textOverflow: 'ellipsis' }}>
                {selected?.label || value}
              </Box>
            </Box>
          );
        }}
      >
        {collections.map((collection) => (
          <MenuItem key={collection.id} value={collection.id}>
            {collection.logo && (
              <ListItemIcon>
                <CollectionLogo src={buildLogoUrl(collection.logo, logoBaseUrl)} />
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
