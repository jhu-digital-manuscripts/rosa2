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

import * as selectors from '../state/selectors';
import { buildThumbnailUrl, parseManifestId } from '../utils/urlBuilder';

/**
 * LazyThumbnail - thumbnail that only loads when visible.
 */
function LazyThumbnail({ src, alt }) {
  const [isVisible, setIsVisible] = React.useState(false);
  const [hasError, setHasError] = React.useState(false);
  const ref = React.useRef(null);

  React.useEffect(() => {
    const observer = new IntersectionObserver(
      ([entry]) => {
        if (entry.isIntersecting) {
          setIsVisible(true);
          observer.disconnect();
        }
      },
      { threshold: 0.1 }
    );

    if (ref.current) {
      observer.observe(ref.current);
    }

    return () => observer.disconnect();
  }, []);

  return (
    <Box
      ref={ref}
      sx={{
        width: 80,
        height: 100,
        bgcolor: 'grey.200',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        borderRadius: 1,
        overflow: 'hidden',
        flexShrink: 0,
      }}
    >
      {isVisible && !hasError ? (
        <Box
          component="img"
          src={src}
          alt={alt}
          onError={() => setHasError(true)}
          sx={{
            maxWidth: '100%',
            maxHeight: '100%',
            objectFit: 'contain',
          }}
        />
      ) : hasError ? (
        <ImageIcon color="disabled" />
      ) : null}
    </Box>
  );
}

/**
 * HighlightedText - renders HTML highlighting from Opensearch.
 */
function HighlightedText({ html, fieldLabel }) {
  return (
    <Typography
      variant="body2"
      color="text.secondary"
      sx={{
        '& mark': {
          bgcolor: 'warning.light',
          px: 0.25,
        },
        overflow: 'hidden',
        textOverflow: 'ellipsis',
        display: '-webkit-box',
        WebkitLineClamp: 2,
        WebkitBoxOrient: 'vertical',
      }}
    >
      <Typography component="span" variant="body2" fontWeight="medium">
        {fieldLabel}:{' '}
      </Typography>
      <span dangerouslySetInnerHTML={{ __html: html }} />
    </Typography>
  );
}

/**
 * CanvasResult displays a single canvas/page result.
 */
export function CanvasResult({ result, onClick }) {
  const thumbnailConfig = useSelector(selectors.getThumbnailConfig);

  const { source, highlight } = result;
  const {
    label,
    manifest_id,
    page_num,
    iiif_image_id,
  } = source;

  // Parse manifest ID to get book info
  const manifestInfo = parseManifestId(manifest_id);
  const bookId = manifestInfo?.bookId || manifest_id;

  // Build thumbnail URL
  const thumbnailUrl = iiif_image_id
    ? buildThumbnailUrl(iiif_image_id, thumbnailConfig)
    : null;

  // Get highlighted fields
  const highlightEntries = Object.entries(highlight || {}).slice(0, 3);

  return (
    <Card variant="outlined" sx={{ mb: 1 }}>
      <CardActionArea onClick={() => onClick(result)} sx={{ p: 1.5 }}>
        <Box sx={{ display: 'flex', gap: 2 }}>
          {/* Thumbnail */}
          {thumbnailUrl && (
            <LazyThumbnail src={thumbnailUrl} alt={`Page ${label}`} />
          )}

          {/* Content */}
          <Box sx={{ flex: 1, minWidth: 0 }}>
            {/* Title row */}
            <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 0.5 }}>
              <Typography
                variant="body1"
                fontWeight="medium"
                sx={{
                  overflow: 'hidden',
                  textOverflow: 'ellipsis',
                  whiteSpace: 'nowrap',
                  flex: 1,
                }}
              >
                {label}
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

            {/* Book reference */}
            <Typography variant="body2" color="text.secondary" sx={{ mb: 1 }}>
              from {bookId} (page {page_num + 1})
            </Typography>

            {/* Highlighted matches */}
            {highlightEntries.map(([field, fragments]) => (
              <HighlightedText
                key={field}
                html={fragments[0]}
                fieldLabel={field.split('.')[0]}
              />
            ))}
          </Box>
        </Box>
      </CardActionArea>
    </Card>
  );
}

export default CanvasResult;
