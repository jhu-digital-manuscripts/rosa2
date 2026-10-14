/**
 * Hooks backing the Name filter: type-ahead suggestions and restoring the labels of
 * books selected through a URL.
 */

import { useEffect, useRef, useState } from 'react';
import { useSelector, useDispatch } from 'react-redux';
import * as selectors from '../state/selectors';
import { setNameLabels } from '../state/actions';
import { searchManifests } from '../services/opensearch';
import { buildNameSuggestQuery, buildNameLookupQuery } from '../utils/queryBuilder';

/** Shortest text that is sent to Opensearch. */
export const MIN_SUGGEST_LENGTH = 2;

/** Wait this long after the last keystroke before querying. */
export const SUGGEST_DEBOUNCE_MS = 250;

/** Most suggestions shown at once. */
export const MAX_SUGGESTIONS = 10;

const NO_OPTIONS = [];

/** Turn manifest hits into {id, label, logo}. */
function toNames(response) {
  return (response?.hits?.hits || [])
    .map((hit) => hit._source || {})
    .filter((source) => source.id)
    .map((source) => ({
      id: String(source.id),
      label: source.label || String(source.id),
      logo: source.logo || null,
    }));
}

/**
 * Suggest books whose label matches the text typed so far.
 *
 * Nothing is sent for fewer than MIN_SUGGEST_LENGTH characters. Requests are
 * debounced, and a response is dropped if a newer request has been started, so
 * suggestions never go back to an older text.
 *
 * @param {string} text - Current input text
 * @returns {{options: Array<{id: string, label: string, logo: ?string}>, loading: boolean, error: ?string}}
 */
export function useNameSuggestions(text) {
  const opensearchUrl = useSelector(selectors.getOpensearchUrl);
  const collectionId = useSelector(selectors.getSelectedCollectionId);
  const facets = useSelector(selectors.getFacets);
  const categories = useSelector(selectors.getCategories);

  const [state, setState] = useState({ options: NO_OPTIONS, loading: false, error: null });
  const requestId = useRef(0);

  const trimmed = (text || '').trim();

  useEffect(() => {
    const id = ++requestId.current;

    if (trimmed.length < MIN_SUGGEST_LENGTH || !opensearchUrl || !collectionId) {
      setState({ options: NO_OPTIONS, loading: false, error: null });
      return undefined;
    }

    setState((prev) => ({ ...prev, loading: true, error: null }));

    const timer = setTimeout(async () => {
      const query = buildNameSuggestQuery({
        text: trimmed,
        collectionId,
        facets,
        categories,
        size: MAX_SUGGESTIONS,
      });
      try {
        const response = await searchManifests(opensearchUrl, query);
        if (id === requestId.current) {
          setState({ options: toNames(response), loading: false, error: null });
        }
      } catch (err) {
        if (id === requestId.current) {
          setState({
            options: NO_OPTIONS,
            loading: false,
            error: err?.message || 'Suggestions failed',
          });
        }
      }
    }, SUGGEST_DEBOUNCE_MS);

    return () => clearTimeout(timer);
  }, [trimmed, opensearchUrl, collectionId, facets, categories]);

  return state;
}

/**
 * Fetch the label and logo of selected books that are not yet known, which is the
 * case for selections restored from a URL. All missing ids go in one request. An id
 * that is not found is remembered with itself as the label, so it is not asked for
 * again.
 */
export function useNameLabelRestore() {
  const dispatch = useDispatch();
  const opensearchUrl = useSelector(selectors.getOpensearchUrl);
  const ids = useSelector(selectors.getSelectedNameIds);
  const labels = useSelector(selectors.getNameLabels);
  const pending = useRef(new Set());

  useEffect(() => {
    if (!opensearchUrl) {
      return;
    }
    const missing = ids.map(String).filter((id) => !labels[id] && !pending.current.has(id));
    if (missing.length === 0) {
      return;
    }

    missing.forEach((id) => pending.current.add(id));

    searchManifests(opensearchUrl, buildNameLookupQuery(missing))
      .then((response) => {
        const found = Object.fromEntries(
          toNames(response).map(({ id, label, logo }) => [id, { label, logo }])
        );
        const update = {};
        for (const id of missing) {
          update[id] = found[id] || { label: id, logo: null };
        }
        dispatch(setNameLabels(update));
      })
      .catch(() => {
        // Leave them unknown so a later render can try again; chips show the id.
      })
      .finally(() => {
        missing.forEach((id) => pending.current.delete(id));
      });
  }, [dispatch, opensearchUrl, ids, labels]);
}
