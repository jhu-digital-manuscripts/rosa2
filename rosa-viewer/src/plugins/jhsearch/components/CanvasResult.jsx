/**
 * CanvasResult component - displays a single canvas (page) in search results.
 */

import React from 'react';
import { useSelector } from 'react-redux';
import Box from '@mui/material/Box';
import Card from '@mui/material/Card';
import CardActionArea from '@mui/material/CardActionArea';
import Typography from '@mui/material/Typography';
import Chip from '@mui/material/Chip';
import ImageIcon from '@mui/icons-material/Image';

import { HighlightedText } from './HighlightedText';
import { Thumbnail } from './Thumbnail';
import * as selectors from '../state/selectors';
import { buildThumbnailUrl, parseManifestId } from '../utils/urlBuilder';

/**
 * CanvasResult displays a single canvas/page result.
 */
export function CanvasResult({ result, onClick }) {
  const imageBaseUrl = useSelector(selectors.getImageBaseUrl);
  const thumbnailTemplate = useSelector(selectors.getThumbnailTemplate);
  const thumbnailWidth = useSelector(selectors.getThumbnailWidth);

  const { source, highlight } = result;
  const { label, manifest_id, manifest_label, iiif_image_id } = source;

  // Book label, falling back to the book ID for indexes built without manifest_label
  const bookLabel = manifest_label || parseManifestId(manifest_id)?.bookId || manifest_id;

  // Build thumbnail config object for urlBuilder functions
  const thumbnailConfig = { imageBaseUrl, thumbnailTemplate, thumbnailWidth };

  // Build thumbnail URL
  const thumbnailUrl = iiif_image_id ? buildThumbnailUrl(iiif_image_id, thumbnailConfig) : null;

  // Get highlighted fields
  const highlightEntries = Object.entries(highlight || {}).slice(0, 3);

  return (
    <Card variant="outlined" sx={{ mb: 1 }}>
      <CardActionArea onClick={() => onClick(result)} sx={{ p: 1.5 }}>
        <Box sx={{ display: 'flex', gap: 2 }}>
          {/* Thumbnail */}
          {thumbnailUrl && (
            <Thumbnail
              src={thumbnailUrl}
              alt={`Page ${label}`}
              width={80}
              height={100}
              FallbackIcon={ImageIcon}
            />
          )}

          {/* Content */}
          <Box sx={{ flex: 1, minWidth: 0 }}>
            {/* Title row: wraps rather than truncating so the page label stays visible */}
            <Box sx={{ display: 'flex', alignItems: 'flex-start', gap: 1, mb: 1 }}>
              <Typography
                variant="body1"
                fontWeight="medium"
                sx={{ flex: 1, minWidth: 0, overflowWrap: 'anywhere' }}
              >
                {bookLabel}: {label}
              </Typography>
              <Chip
                icon={<ImageIcon />}
                label="Page"
                size="small"
                variant="outlined"
                color="secondary"
                sx={{ flexShrink: 0 }}
              />
            </Box>

            {/* Highlighted matches */}
            {highlightEntries.map(([field, fragments]) => (
              <HighlightedText key={field} html={fragments[0]} field={field} lineClamp={2} />
            ))}
          </Box>
        </Box>
      </CardActionArea>
    </Card>
  );
}

export default CanvasResult;
