/**
 * Thumbnail - a page thumbnail in a fixed box, loaded when it nears the viewport.
 */

import React from 'react';
import Box from '@mui/material/Box';

/**
 * @param {string} src - Image URL
 * @param {string} alt - Alternative text
 * @param {number} width - Box width in pixels
 * @param {number} height - Box height in pixels
 * @param {React.ElementType} FallbackIcon - Icon shown when the image fails to load
 */
export function Thumbnail({ src, alt, width, height, FallbackIcon }) {
  const [hasError, setHasError] = React.useState(false);

  return (
    <Box
      sx={{
        width,
        height,
        bgcolor: 'grey.200',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        borderRadius: 1,
        overflow: 'hidden',
        flexShrink: 0,
      }}
    >
      {hasError ? (
        <FallbackIcon color="disabled" />
      ) : (
        <Box
          component="img"
          src={src}
          alt={alt}
          loading="lazy"
          onError={() => setHasError(true)}
          sx={{
            maxWidth: '100%',
            maxHeight: '100%',
            objectFit: 'contain',
          }}
        />
      )}
    </Box>
  );
}

export default Thumbnail;
