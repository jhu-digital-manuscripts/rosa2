/**
 * The Archaeology of Reading site definition: what is fixed for the
 * site, as passed to createSiteViewer() by main.js. Plain data, so the build and
 * packaging scripts can read it too (scripts/site-collections.mjs).
 */

import { displayFont } from '../../src/site/theme.js';

export default {
  id: 'aor',
  title: 'The Archaeology of Reading',
  jhsearch: {
    collectionId: 'aor',
    childCollectionIds: [],
    // Facets on, for the Name filter. jhsearch.json for aor lists no categories,
    // so Name is the only filter shown.
    enableFacets: true,
    // A book row opens the first page in single image view
    bookClickView: 'single',
  },
  // Gabriel Harvey's marginalia across the head of a page in his copy of
  // Domenichi's Facetie (Folger H.a.2, fol. 12r), as on the old site's banner.
  banner: {
    imageId: 'aor%2FFolgersHa2%2FFolgersHa2.012r',
    region: '300,90,2650,260',
    width: 1800,
  },
  theme: {
    palette: {
      primary: { main: '#8a2b21' },
      secondary: { main: '#8a2b21' },
      shades: { dark: '#e9e2d4', main: '#fffdf8', light: '#f5f0e6' },
      background: { default: '#f5f0e6', paper: '#fffdf8' },
    },
    typography: {
      h5: { fontFamily: displayFont },
      h6: { fontFamily: displayFont },
      subtitle1: { fontFamily: displayFont },
    },
  },
};
