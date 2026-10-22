/**
 * HighlightedText - renders a highlight fragment from Opensearch with the
 * label of the field it matched in.
 */

import React from 'react';
import Typography from '@mui/material/Typography';

/**
 * Format an Opensearch highlight field name for display.
 *
 * Drops sub-field suffixes, turns underscores into spaces and capitalizes the
 * first letter, e.g. 'cross_reference.en' becomes 'Cross reference'.
 *
 * @param {string} field - Highlighted field name
 * @returns {string} Display label
 */
export function formatFieldLabel(field) {
  const name = field.split('.')[0].replace(/_/g, ' ');
  return name.charAt(0).toUpperCase() + name.slice(1);
}

/**
 * @param {string} html - Highlight fragment with <mark> tags
 * @param {string} field - Highlighted field name
 * @param {number} [lineClamp] - Maximum lines to show before truncating
 */
export function HighlightedText({ html, field, lineClamp }) {
  return (
    <Typography
      variant="body2"
      color="text.secondary"
      sx={{
        '& mark': {
          bgcolor: 'warning.light',
          px: 0.25,
        },
        ...(lineClamp && {
          overflow: 'hidden',
          textOverflow: 'ellipsis',
          display: '-webkit-box',
          WebkitLineClamp: lineClamp,
          WebkitBoxOrient: 'vertical',
        }),
      }}
    >
      <Typography component="span" variant="body2" color="text.primary" sx={{ fontWeight: 'bold' }}>
        {formatFieldLabel(field)}:
      </Typography>{' '}
      <span dangerouslySetInnerHTML={{ __html: html }} />
    </Typography>
  );
}

export default HighlightedText;
