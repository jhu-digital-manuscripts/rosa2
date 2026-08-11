/**
 * ManifestResult component - displays a single manifest in search/browse results.
 */

import React from 'react';
import { useSelector } from 'react-redux';
import Box from '@mui/material/Box';
import Card from '@mui/material/Card';
import CardActionArea from '@mui/material/CardActionArea';
import Typography from '@mui/material/Typography';
import Avatar from '@mui/material/Avatar';
import Chip from '@mui/material/Chip';
import BookIcon from '@mui/icons-material/Book';

import * as selectors from '../state/selectors';
import { buildThumbnailUrl, buildLogoUrl } from '../utils/urlBuilder';

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
        width: 60,
        height: 80,
        bgcolor: 'grey.200',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        borderRadius: 1,
        overflow: 'hidden',
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
        <BookIcon color="disabled" />
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
 * ManifestResult displays a single manifest result.
 */
export function ManifestResult({ result, onClick }) {
  const imageBaseUrl = useSelector(selectors.getImageBaseUrl);
  const thumbnailTemplate = useSelector(selectors.getThumbnailTemplate);
  const thumbnailWidth = useSelector(selectors.getThumbnailWidth);
  const logoBaseUrl = useSelector(selectors.getLogoBaseUrl);

  const { source, highlight } = result;
  const {
    label,
    num_pages,
    thumbnail: thumbnails,
    logo,
  } = source;

  // Build thumbnail config object for urlBuilder functions
  const thumbnailConfig = { imageBaseUrl, thumbnailTemplate, thumbnailWidth };

  // Build thumbnail URLs
  const thumbnailUrls = (thumbnails || []).slice(0, 3).map((thumb) => ({
    url: buildThumbnailUrl(thumb.iiif_image_id, thumbnailConfig),
    pageNum: thumb.page_num,
  }));

  // Build logo URL
  const logoUrl = logo ? buildLogoUrl(logo, logoBaseUrl) : null;

  // Get highlighted fields
  const highlightEntries = Object.entries(highlight || {}).slice(0, 2);

  return (
    <Card variant="outlined" sx={{ mb: 1 }}>
      <CardActionArea onClick={() => onClick(result)} sx={{ p: 1.5 }}>
        <Box sx={{ display: 'flex', gap: 2 }}>
          {/* Logo */}
          {logoUrl && (
            <Avatar
              src={logoUrl}
              alt=""
              sx={{ width: 32, height: 32, flexShrink: 0 }}
            />
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
                icon={<BookIcon />}
                label="Book"
                size="small"
                variant="outlined"
                sx={{ flexShrink: 0 }}
              />
            </Box>

            {/* Metadata row */}
            <Typography variant="body2" color="text.secondary" sx={{ mb: 1 }}>
              {num_pages} pages
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

          {/* Thumbnails */}
          <Box sx={{ display: 'flex', gap: 0.5, flexShrink: 0 }}>
            {thumbnailUrls.map((thumb, index) => (
              <LazyThumbnail
                key={index}
                src={thumb.url}
                alt={`Page ${thumb.pageNum}`}
              />
            ))}
          </Box>
        </Box>
      </CardActionArea>
    </Card>
  );
}

export default ManifestResult;
