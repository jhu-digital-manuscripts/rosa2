/**
 * The Digital Library of Medieval Manuscripts site definition: what is fixed for the
 * site, as passed to createSiteViewer() by main.js. Plain data, so the build and
 * packaging scripts can read it too (scripts/site-collections.mjs).
 */

const displayFont =
  "'Iowan Old Style', 'Palatino Linotype', Palatino, 'Book Antiqua', Georgia, serif";

export default {
  id: 'dlmm',
  title: 'Digital Library of Medieval Manuscripts',
  jhsearch: {
    collectionId: 'dlmm',
    childCollectionIds: ['rose', 'pizan'],
    // A book row opens the single image view
    bookClickView: 'single',
    // Shown in the collection selector, each with the logo public/logo/{id}.jpg
    collectionLabels: {
      dlmm: 'Digital Library of Medieval Manuscripts',
      rose: 'Roman de la Rose Digital Library',
      pizan: 'Christine de Pizan Digital Scriptorium',
    },
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
};
