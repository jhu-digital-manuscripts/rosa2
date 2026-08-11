/**
 * CategoryFacets component - displays facet categories for filtering.
 */

import React from 'react';
import { useSelector, useDispatch } from 'react-redux';
import Box from '@mui/material/Box';
import Typography from '@mui/material/Typography';
import List from '@mui/material/List';
import ListItem from '@mui/material/ListItem';
import ListItemButton from '@mui/material/ListItemButton';
import ListItemIcon from '@mui/material/ListItemIcon';
import ListItemText from '@mui/material/ListItemText';
import Checkbox from '@mui/material/Checkbox';
import Chip from '@mui/material/Chip';
import Accordion from '@mui/material/Accordion';
import AccordionSummary from '@mui/material/AccordionSummary';
import AccordionDetails from '@mui/material/AccordionDetails';
import ExpandMoreIcon from '@mui/icons-material/ExpandMore';
import Badge from '@mui/material/Badge';

import * as selectors from '../state/selectors';
import { toggleFacet } from '../state/actions';

/**
 * Single facet category display.
 */
function FacetCategory({ category, values, selectedValues, onToggle }) {
  const selectedCount = selectedValues.length;

  // Sort values by count (descending), then alphabetically
  const sortedValues = [...values].sort((a, b) => {
    if (b.count !== a.count) {
      return b.count - a.count;
    }
    // Handle non-string keys (e.g., booleans) by converting to string
    const keyA = String(a.key);
    const keyB = String(b.key);
    return keyA.localeCompare(keyB);
  });

  return (
    <Accordion defaultExpanded={selectedCount > 0} disableGutters>
      <AccordionSummary
        expandIcon={<ExpandMoreIcon />}
        aria-controls={`facet-${category.name}-content`}
        id={`facet-${category.name}-header`}
      >
        <Badge
          badgeContent={selectedCount}
          color="primary"
          invisible={selectedCount === 0}
          sx={{ width: '100%' }}
        >
          <Typography variant="body2" fontWeight="medium">
            {category.label}
          </Typography>
        </Badge>
      </AccordionSummary>
      <AccordionDetails sx={{ p: 0 }}>
        <List dense disablePadding>
          {sortedValues.map((value) => {
            const isSelected = selectedValues.includes(value.key);
            // Convert key to string for display and React key
            const keyStr = String(value.key);
            // Format display value (e.g., "true" -> "Yes", "false" -> "No")
            const displayValue = value.key === true ? 'Yes' 
              : value.key === false ? 'No' 
              : keyStr || '(empty)';

            return (
              <ListItem key={keyStr} disablePadding>
                <ListItemButton
                  role="checkbox"
                  aria-checked={isSelected}
                  onClick={() => onToggle(category.name, value.key)}
                  dense
                >
                  <ListItemIcon sx={{ minWidth: 36 }}>
                    <Checkbox
                      edge="start"
                      checked={isSelected}
                      tabIndex={-1}
                      disableRipple
                      size="small"
                    />
                  </ListItemIcon>
                  <ListItemText
                    primary={
                      <Box
                        sx={{
                          display: 'flex',
                          justifyContent: 'space-between',
                          alignItems: 'center',
                        }}
                      >
                        <Typography
                          variant="body2"
                          sx={{
                            overflow: 'hidden',
                            textOverflow: 'ellipsis',
                            whiteSpace: 'nowrap',
                            flex: 1,
                          }}
                        >
                          {displayValue}
                        </Typography>
                        <Chip
                          label={value.count}
                          size="small"
                          variant="outlined"
                          sx={{ ml: 1, height: 20, fontSize: '0.75rem' }}
                        />
                      </Box>
                    }
                  />
                </ListItemButton>
              </ListItem>
            );
          })}
          {sortedValues.length === 0 && (
            <ListItem>
              <ListItemText
                secondary="No values available"
                sx={{ textAlign: 'center' }}
              />
            </ListItem>
          )}
        </List>
      </AccordionDetails>
    </Accordion>
  );
}

/**
 * SelectedFacets displays chips for currently selected facet values.
 */
function SelectedFacets({ facets, categories, onToggle }) {
  const chips = [];

  for (const [categoryName, values] of Object.entries(facets)) {
    const category = categories.find((c) => c.name === categoryName);
    const categoryLabel = category?.label || categoryName;

    for (const value of values) {
      chips.push({
        key: `${categoryName}:${value}`,
        category: categoryName,
        categoryLabel,
        value,
      });
    }
  }

  if (chips.length === 0) {
    return null;
  }

  return (
    <Box sx={{ mb: 2 }}>
      <Typography variant="body2" color="text.secondary" sx={{ mb: 1 }}>
        Active Filters
      </Typography>
      <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 0.5 }}>
        {chips.map((chip) => (
          <Chip
            key={chip.key}
            label={`${chip.categoryLabel}: ${chip.value}`}
            size="small"
            onDelete={() => onToggle(chip.category, chip.value)}
            color="primary"
            variant="outlined"
          />
        ))}
      </Box>
    </Box>
  );
}

/**
 * CategoryFacets displays all facet categories for filtering results.
 */
export function CategoryFacets() {
  const dispatch = useDispatch();

  const categories = useSelector(selectors.getCategories);
  const aggregations = useSelector(selectors.getAggregations);
  const facets = useSelector(selectors.getFacets);
  const enableFacets = useSelector(selectors.areFacetsEnabled);

  if (!enableFacets || categories.length === 0) {
    return null;
  }

  const handleToggle = (category, value) => {
    dispatch(toggleFacet(category, value));
  };

  return (
    <Box>
      {/* Selected facets display */}
      <SelectedFacets
        facets={facets}
        categories={categories}
        onToggle={handleToggle}
      />

      {/* Facet categories */}
      <Typography variant="body2" color="text.secondary" sx={{ mb: 1 }}>
        Filter By
      </Typography>
      
      {categories.map((category) => (
        <FacetCategory
          key={category.name}
          category={category}
          values={aggregations[category.name] || []}
          selectedValues={facets[category.name] || []}
          onToggle={handleToggle}
        />
      ))}
    </Box>
  );
}

export default CategoryFacets;
