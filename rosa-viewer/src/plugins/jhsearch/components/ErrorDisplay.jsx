/**
 * Error display component for showing initialization and search errors.
 */

import React from 'react';
import Box from '@mui/material/Box';
import Typography from '@mui/material/Typography';
import Alert from '@mui/material/Alert';
import AlertTitle from '@mui/material/AlertTitle';
import Button from '@mui/material/Button';
import RefreshIcon from '@mui/icons-material/Refresh';

/**
 * ErrorDisplay shows an error message with optional retry functionality.
 */
export function ErrorDisplay({ error, title, onRetry, fullScreen = false }) {
  const errorMessage = typeof error === 'string' ? error : error?.message || 'An unexpected error occurred';

  const content = (
    <Alert
      severity="error"
      sx={{
        maxWidth: fullScreen ? 600 : '100%',
        mx: fullScreen ? 'auto' : 0,
      }}
      action={
        onRetry ? (
          <Button
            color="inherit"
            size="small"
            startIcon={<RefreshIcon />}
            onClick={onRetry}
          >
            Retry
          </Button>
        ) : null
      }
    >
      <AlertTitle>{title || 'Error'}</AlertTitle>
      <Typography variant="body2">{errorMessage}</Typography>
    </Alert>
  );

  if (fullScreen) {
    return (
      <Box
        sx={{
          display: 'flex',
          flexDirection: 'column',
          alignItems: 'center',
          justifyContent: 'center',
          height: '100%',
          p: 3,
        }}
      >
        {content}
      </Box>
    );
  }

  return content;
}

/**
 * Initialization error display - shown when the plugin fails to initialize.
 */
export function InitializationError({ error, onRetry }) {
  return (
    <ErrorDisplay
      error={error}
      title="Failed to Initialize Search"
      onRetry={onRetry}
      fullScreen
    />
  );
}

/**
 * Search error display - shown in the results area when a search fails.
 */
export function SearchError({ error, onRetry }) {
  return (
    <ErrorDisplay
      error={error}
      title="Search Failed"
      onRetry={onRetry}
    />
  );
}

export default ErrorDisplay;
