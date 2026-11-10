/**
 * SearchResults component - displays browse and search results.
 */

import React, { useEffect, useRef } from 'react';
import { useSelector, useDispatch } from 'react-redux';
import Box from '@mui/material/Box';
import Typography from '@mui/material/Typography';
import CircularProgress from '@mui/material/CircularProgress';
import LinearProgress from '@mui/material/LinearProgress';
import Alert from '@mui/material/Alert';
import Button from '@mui/material/Button';
import RefreshIcon from '@mui/icons-material/Refresh';
import { addWindow } from 'mirador';

import { ManifestResult } from './ManifestResult';
import { CanvasResult } from './CanvasResult';
import { Pagination } from './Pagination';
import { SearchError } from './ErrorDisplay';

import * as selectors from '../state/selectors';
import { buildManifestUrl, buildCanvasUrl } from '../utils/urlBuilder';
import { MAX_RESULT_WINDOW } from '../utils/queryBuilder';
import { getTranslations, t } from '../../../i18n/jhsearch';

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
      {total > MAX_RESULT_WINDOW && (
        <Typography variant="body2" color="text.secondary">
          {t('resultsLimited', getTranslations(), { limit: MAX_RESULT_WINDOW.toLocaleString() })}
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
  const partial = useSelector(selectors.getResultsPartial);
  const total = useSelector(selectors.getTotalResults);
  const viewMode = useSelector(selectors.getViewMode);
  const iiifBaseUrl = useSelector(selectors.getIiifBaseUrl);

  const bookClickView = useSelector(selectors.getBookClickView);

  const rootRef = useRef(null);
  const listRef = useRef(null);

  // Show the start of each new set of results, however it arrived: pagination,
  // Back/Forward (site history restores the search state), or a sort or filter
  // change. Runs when the results arrive rather than when they are requested,
  // so the old page doesn't jump while the new one loads.
  useEffect(() => {
    const root = rootRef.current;
    const list = listRef.current;
    if (!root || !list) return;

    // From md up the list scrolls on its own
    list.scrollTop = 0;

    // Below md the whole view (JHSearchView, the parent) scrolls instead. Bring
    // the top of the results back only if it has scrolled out above, so that a
    // search made in the sidebar doesn't scroll the sidebar away.
    const view = root.parentElement;
    if (!view) return;
    const offset = root.getBoundingClientRect().top - view.getBoundingClientRect().top;
    if (offset < 0) view.scrollTop += offset;
  }, [results]);

  /**
   * Open a book in a new Mirador window and hide the JHSearch view.
   * Without a canvas Mirador starts at the first page of the book.
   */
  const openWindow = ({ manifestId, canvasId, view }) => {
    dispatch(addWindow({ manifestId, view, ...(canvasId ? { canvasId } : {}) }));

    dispatch({
      type: 'mirador/SET_WORKSPACE_ADD_VISIBILITY',
      isWorkspaceAddVisible: false,
    });
  };

  const handleManifestClick = (result) => {
    openWindow({
      manifestId: buildManifestUrl(result.source.id, iiifBaseUrl),
      view: bookClickView,
    });
  };

  const handleThumbnailClick = (result, pageNum) => {
    const { id } = result.source;
    openWindow({
      manifestId: buildManifestUrl(id, iiifBaseUrl),
      canvasId: buildCanvasUrl(id, pageNum, iiifBaseUrl),
      view: 'single',
    });
  };

  const handleCanvasClick = (result) => {
    const { manifest_id, page_num } = result.source;
    openWindow({
      manifestId: buildManifestUrl(manifest_id, iiifBaseUrl),
      canvasId: buildCanvasUrl(manifest_id, page_num, iiifBaseUrl),
      view: 'single',
    });
  };

  // From md up the results sit beside the sidebar and the list scrolls on its own.
  // Below md they sit below it at their full height and the whole view scrolls.
  return (
    <Box
      ref={rootRef}
      sx={{
        flex: { xs: 'none', md: 1 },
        minWidth: 0,
        display: 'flex',
        flexDirection: 'column',
        overflow: { xs: 'visible', md: 'hidden' },
      }}
    >
      {/* Header */}
      <ResultsHeader viewMode={viewMode} total={total} />

      {/* A search replacing results already shown: they stay until the new ones
          arrive, so say that something is happening. The bar keeps its height
          while idle so the list does not shift. */}
      <Box sx={{ height: 4 }}>
        {isLoading && results.length > 0 && <LinearProgress aria-label="Searching" />}
      </Box>

      {/* Content area */}
      <Box
        ref={listRef}
        aria-busy={isLoading}
        sx={{
          flex: { xs: 'none', md: 1 },
          overflow: { xs: 'visible', md: 'auto' },
          p: 2,
        }}
      >
        {/* Loading state */}
        {isLoading && results.length === 0 && <LoadingState />}

        {/* Error state */}
        {error && <SearchError error={error} onRetry={onRetry} />}

        {/* Incomplete response. A successful search that is missing matches, so it
            is a warning beside the results rather than an error instead of them. */}
        {!error && partial && (
          <Alert
            severity="warning"
            sx={{ mb: 2 }}
            action={
              onRetry ? (
                <Button color="inherit" size="small" startIcon={<RefreshIcon />} onClick={onRetry}>
                  Retry
                </Button>
              ) : null
            }
          >
            {partial.message}
          </Alert>
        )}

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

        {/* Results list. A size container, so each result lays itself out by the
            width of the list rather than of the viewport. */}
        {!error && results.length > 0 && (
          <Box sx={{ containerType: 'inline-size' }}>
            {results.map((result) => {
              // Determine if this is a manifest or canvas result
              const isCanvas = result.index === 'canvas';

              return isCanvas ? (
                <CanvasResult key={result.id} result={result} onClick={handleCanvasClick} />
              ) : (
                <ManifestResult
                  key={result.id}
                  result={result}
                  onClick={handleManifestClick}
                  onThumbnailClick={handleThumbnailClick}
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
