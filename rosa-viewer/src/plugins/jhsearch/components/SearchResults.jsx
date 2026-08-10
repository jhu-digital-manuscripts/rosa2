/**
 * SearchResults component - displays browse and search results.
 */

import React from 'react';
import { useSelector, useDispatch } from 'react-redux';
import Box from '@mui/material/Box';
import Typography from '@mui/material/Typography';
import CircularProgress from '@mui/material/CircularProgress';

import { ManifestResult } from './ManifestResult';
import { CanvasResult } from './CanvasResult';
import { Pagination } from './Pagination';
import { SearchError } from './ErrorDisplay';

import * as selectors from '../state/selectors';
import { buildManifestUrl, buildCanvasUrl } from '../utils/urlBuilder';

/**
 * Empty state display.
 */
function EmptyState({ message }) {
  return (
    <Box
      sx={{
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        justifyContent: 'center',
        height: '100%',
        p: 4,
        textAlign: 'center',
      }}
    >
      <Typography variant="h6" color="text.secondary" gutterBottom>
        No Results Found
      </Typography>
      <Typography variant="body2" color="text.secondary">
        {message || 'Try adjusting your search or filters.'}
      </Typography>
    </Box>
  );
}

/**
 * Loading state display.
 */
function LoadingState() {
  return (
    <Box
      sx={{
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        justifyContent: 'center',
        height: '100%',
        p: 4,
      }}
    >
      <CircularProgress />
      <Typography variant="body2" color="text.secondary" sx={{ mt: 2 }}>
        Searching...
      </Typography>
    </Box>
  );
}

/**
 * Results header with count.
 */
function ResultsHeader({ viewMode, total }) {
  const label = viewMode === 'search' ? 'Search Results' : 'Browse Collection';

  return (
    <Box sx={{ p: 2, borderBottom: 1, borderColor: 'divider' }}>
      <Typography variant="h6">{label}</Typography>
      {total > 0 && (
        <Typography variant="body2" color="text.secondary">
          {total.toLocaleString()} {total === 1 ? 'result' : 'results'}
        </Typography>
      )}
    </Box>
  );
}

/**
 * SearchResults displays the list of results.
 */
export function SearchResults({ onRetry }) {
  const dispatch = useDispatch();

  const results = useSelector(selectors.getResultItems);
  const isLoading = useSelector(selectors.isResultsLoading);
  const error = useSelector(selectors.getResultsError);
  const total = useSelector(selectors.getTotalResults);
  const viewMode = useSelector(selectors.getViewMode);
  const iiifBaseUrl = useSelector(selectors.getIiifBaseUrl);

  const handleManifestClick = (result) => {
    const manifestUrl = buildManifestUrl(result.source.id, iiifBaseUrl);
    
    // Dispatch Mirador action to open the manifest
    dispatch({
      type: 'mirador/ADD_WINDOW',
      window: {
        manifestId: manifestUrl,
      },
    });
  };

  const handleCanvasClick = (result) => {
    const { manifest_id, page_num } = result.source;
    const manifestUrl = buildManifestUrl(manifest_id, iiifBaseUrl);
    const canvasUrl = buildCanvasUrl(manifest_id, page_num, iiifBaseUrl);
    
    // Dispatch Mirador action to open the manifest at the specific canvas
    dispatch({
      type: 'mirador/ADD_WINDOW',
      window: {
        manifestId: manifestUrl,
        canvasId: canvasUrl,
      },
    });
  };

  return (
    <Box
      sx={{
        flex: 1,
        display: 'flex',
        flexDirection: 'column',
        overflow: 'hidden',
      }}
    >
      {/* Header */}
      <ResultsHeader viewMode={viewMode} total={total} />

      {/* Content area */}
      <Box
        data-results-container
        sx={{
          flex: 1,
          overflow: 'auto',
          p: 2,
        }}
      >
        {/* Loading state */}
        {isLoading && results.length === 0 && <LoadingState />}

        {/* Error state */}
        {error && <SearchError error={error} onRetry={onRetry} />}

        {/* Empty state */}
        {!isLoading && !error && results.length === 0 && (
          <EmptyState
            message={
              viewMode === 'search'
                ? 'No results matched your search. Try different terms or remove some filters.'
                : 'No items found in this collection.'
            }
          />
        )}

        {/* Results list */}
        {!error && results.length > 0 && (
          <Box>
            {results.map((result) => {
              // Determine if this is a manifest or canvas result
              const isCanvas = result.index === 'canvas';

              return isCanvas ? (
                <CanvasResult
                  key={result.id}
                  result={result}
                  onClick={handleCanvasClick}
                />
              ) : (
                <ManifestResult
                  key={result.id}
                  result={result}
                  onClick={handleManifestClick}
                />
              );
            })}
          </Box>
        )}
      </Box>

      {/* Pagination */}
      {!error && total > 0 && <Pagination />}
    </Box>
  );
}

export default SearchResults;
