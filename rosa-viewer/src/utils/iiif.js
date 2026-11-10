/**
 * Helpers for IIIF Presentation JSON.
 */

/**
 * The plain text of a IIIF label: a Presentation 3 language map
 * (`{ en: ['Douce 195'] }`) or a plain string. English is preferred, then the
 * language-neutral `none`, then whichever language comes first.
 *
 * @param {Object|string} label
 * @returns {string} The first value, or '' when there is none
 */
export function iiifLabel(label) {
  if (typeof label === 'string') return label;
  if (!label || typeof label !== 'object') return '';

  const values = label.en || label.none || Object.values(label)[0];
  if (Array.isArray(values)) return values[0] ?? '';
  return typeof values === 'string' ? values : '';
}
