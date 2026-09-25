/**
 * Digital Library of Medieval Manuscripts site.
 *
 * Everything here is fixed for the DLMM website. Deployment-specific URLs come
 * from window.rosaSiteEnv, set in index.html.
 */

import '../../src/site/site.css';
import './site.css';
import { createSiteViewer } from '../../src/site';

const displayFont =
  "'Iowan Old Style', 'Palatino Linotype', Palatino, 'Book Antiqua', Georgia, serif";

createSiteViewer({
  id: 'dlmm',
  title: 'Digital Library of Medieval Manuscripts',
  jhsearch: {
    collectionId: 'dlmm',
    childCollectionIds: ['rose', 'pizan'],
  },
  // The floral bas-de-page border of the opening of the Roman de la Rose in
  // Bodleian MS. Douce 195, fol. 1r.
  banner: {
    imageId: 'rose%2FDouce195%2Fcropped%2FDouce195.001r',
    region: '0,4300,3661,720',
    width: 1800,
  },
  theme: {
    palette: {
      primary: { main: '#9b2c3a' },
      secondary: { main: '#9b2c3a' },
      shades: { dark: '#e6e4df', main: '#ffffff', light: '#f3f2ef' },
      background: { default: '#f3f2ef', paper: '#ffffff' },
    },
    typography: {
      h5: { fontFamily: displayFont },
      h6: { fontFamily: displayFont },
      subtitle1: { fontFamily: displayFont },
    },
  },
});
