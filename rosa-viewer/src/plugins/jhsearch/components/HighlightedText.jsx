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

const MARKED = /<mark>([\s\S]*?)<\/mark>/;

/**
 * Split a highlight fragment into plain and matched text.
 *
 * A fragment is indexed text with the matches wrapped in the <mark> tags the query
 * asks for (queryBuilder.js). Opensearch does not escape the text, so it is never
 * parsed as HTML: only those tags are recognised, and everything else, including any
 * `<` or `&` in the text, is shown as written.
 *
 * @param {string} fragment - Highlight fragment with <mark> tags
 * @returns {Array<{text: string, marked: boolean}>} Non-empty parts in order
 */
export function splitHighlight(fragment) {
  // split() with one capture group alternates unmatched text and captured matches
  return String(fragment ?? '')
    .split(MARKED)
    .map((text, i) => ({ text, marked: i % 2 === 1 }))
    .filter((part) => part.text !== '');
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
      <span>
        {splitHighlight(html).map((part, i) =>
          part.marked ? <mark key={i}>{part.text}</mark> : part.text
        )}
      </span>
    </Typography>
  );
}

export default HighlightedText;
