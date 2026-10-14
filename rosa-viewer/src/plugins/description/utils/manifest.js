/**
 * Find the TEI book description linked from a IIIF manifest's seeAlso
 * (IIIF cookbook recipe 0053), as written by rosa-tool generate-iiif-pres.
 */

import { TEI_NS } from './tei';

export const TEI_FORMAT = 'application/tei+xml';

const asArray = (value) => (Array.isArray(value) ? value : value ? [value] : []);

const isTei = (entry) => {
  const profiles = asArray(entry?.profile);
  return (
    profiles.some((p) => typeof p === 'string' && p.replace(/\/+$/, '') === TEI_NS) ||
    entry?.format === TEI_FORMAT
  );
};

/**
 * URL of the TEI description in manifest JSON, or null.
 * @param {Object} manifest - IIIF Presentation 3 manifest JSON
 * @returns {string|null}
 */
export function selectTeiDescriptionUrl(manifest) {
  const entry = asArray(manifest?.seeAlso).find(isTei);
  const id = entry?.id ?? entry?.['@id'];
  return typeof id === 'string' && id !== '' ? id : null;
}

/**
 * Whether the manifest links a TEI description.
 * @param {Object} manifest
 * @returns {boolean}
 */
export function hasTeiDescription(manifest) {
  return selectTeiDescriptionUrl(manifest) !== null;
}
