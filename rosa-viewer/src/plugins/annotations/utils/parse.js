/**
 * Read the parts of a IIIF Presentation 3 commenting annotation the Rosa panel needs.
 *
 * Rosa annotations follow IIIF cookbook recipe 0045: the body is a SpecificResource
 * whose styleClass names CSS classes and whose source is an HTML TextualBody.
 * Plain TextualBody bodies (and lists of bodies) from other sources are handled too.
 */

function asArray(value) {
  if (value === undefined || value === null) return [];
  return Array.isArray(value) ? value : [value];
}

function escapeText(text) {
  return String(text).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
}

function splitClasses(styleClass) {
  return typeof styleClass === 'string' ? styleClass.split(/\s+/).filter(Boolean) : [];
}

/**
 * @param {Object} raw - Annotation JSON
 * @returns {{id: string, classes: string[], html: string, language: (string|undefined),
 *   stylesheet: (string|undefined)}|null} null when the annotation has no textual content
 */
export function parseAnnotation(raw) {
  if (!raw || typeof raw !== 'object') return null;

  const classes = [];
  const parts = [];
  let language;

  for (const body of asArray(raw.body)) {
    if (!body || typeof body !== 'object' || body.purpose === 'tagging') continue;
    let textual = body;
    if (body.type === 'SpecificResource') {
      for (const c of splitClasses(body.styleClass)) {
        if (!classes.includes(c)) classes.push(c);
      }
      textual = body.source;
    }
    if (!textual || typeof textual !== 'object' || typeof textual.value !== 'string') continue;
    const value = textual.value.trim();
    if (!value) continue;
    parts.push(textual.format === 'text/html' ? value : `<p>${escapeText(value)}</p>`);
    if (!language && typeof textual.language === 'string' && textual.language) {
      language = textual.language;
    }
  }

  if (parts.length === 0) return null;
  const stylesheet =
    typeof raw.stylesheet === 'string'
      ? raw.stylesheet
      : typeof raw.stylesheet?.id === 'string'
        ? raw.stylesheet.id
        : undefined;
  return { id: raw.id, classes, html: parts.join(''), language, stylesheet };
}
