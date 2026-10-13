/**
 * Add a stylesheet link to the document head once.
 *
 * @param {string} href - Stylesheet URL
 * @param {Document} [doc=document]
 * @returns {HTMLLinkElement|null} The link element, or null without a document or URL
 */
export function addStylesheet(href, doc = globalThis.document) {
  if (!doc || !href) return null;
  for (const link of doc.querySelectorAll('link[rel="stylesheet"][data-rosa-stylesheet]')) {
    if (link.getAttribute('href') === href) return link;
  }
  const link = doc.createElement('link');
  link.rel = 'stylesheet';
  link.href = href;
  link.setAttribute('data-rosa-stylesheet', '');
  doc.head.appendChild(link);
  return link;
}
