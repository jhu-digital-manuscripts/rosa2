/**
 * Sanitize annotation body HTML to the IIIF cookbook rules for HTML in annotations
 * (recipe 0019): only a, b, br, i, img, p, small, span, sub and sup, with href on a
 * and src/alt on img.
 *
 * Mirador's own "iiif" sanitization rule set drops small, sub and sup, which the
 * Rosa annotations use for metadata and line numbers, and its rule sets cannot be
 * extended. So annotations are sanitized here with DOMPurify instead.
 */

import DOMPurify from 'dompurify';

export const ALLOWED_TAGS = ['a', 'b', 'br', 'i', 'img', 'p', 'small', 'span', 'sub', 'sup'];
export const ALLOWED_ATTR = ['href', 'src', 'alt'];

// Only web links and in-page anchors; no javascript:, data: or other schemes.
const ALLOWED_URI_REGEXP = /^(?:https?:|mailto:|#|\/|\.{0,2}\/)/i;

const CONFIG = {
  ALLOWED_TAGS,
  ALLOWED_ATTR,
  ALLOWED_URI_REGEXP,
  KEEP_CONTENT: true,
};

let purifier = null;

function getPurifier() {
  if (!purifier) {
    purifier = DOMPurify(globalThis.window);
    purifier.addHook('afterSanitizeAttributes', (node) => {
      // href is only meaningful on a, src/alt only on img
      const tag = node.nodeName.toLowerCase();
      if (tag !== 'a') node.removeAttribute('href');
      if (tag !== 'img') {
        node.removeAttribute('src');
        node.removeAttribute('alt');
      }
    });
  }
  return purifier;
}

/**
 * Sanitize HTML and return it as a string.
 * @param {string} html
 * @returns {string}
 */
export function sanitizeIiifHtml(html) {
  if (typeof html !== 'string' || html === '') return '';
  return getPurifier().sanitize(html, CONFIG);
}

/**
 * Sanitize HTML and return a DocumentFragment, for conversion to React elements.
 * @param {string} html
 * @returns {DocumentFragment}
 */
export function sanitizeIiifHtmlToFragment(html) {
  return getPurifier().sanitize(typeof html === 'string' ? html : '', {
    ...CONFIG,
    RETURN_DOM_FRAGMENT: true,
  });
}
