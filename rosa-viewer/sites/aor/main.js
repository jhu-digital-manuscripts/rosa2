/**
 * The Archaeology of Reading site.
 *
 * Everything here is fixed for the AoR website. Deployment-specific URLs come
 * from window.rosaSiteEnv, set in index.html.
 */

import '../../src/site/site.css';
import './site.css';
import { createSiteViewer } from '../../src/site';

const displayFont =
  "'Iowan Old Style', 'Palatino Linotype', Palatino, 'Book Antiqua', Georgia, serif";

createSiteViewer({
  id: 'aor',
  title: 'The Archaeology of Reading',
  jhsearch: {
    collectionId: 'aor',
    childCollectionIds: [],
    enableFacets: false,
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
});
