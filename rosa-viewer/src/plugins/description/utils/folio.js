/**
 * Match folio references in book descriptions (<locus>) to canvases.
 *
 * DLMM canvas labels are folio names such as "1r" and "12v", or a bare number
 * ("12") in some books. Other canvases are labelled "front matter 1r",
 * "binding front cover", "1r insert" and so on, and never match. A reference
 * without a side is taken to be the recto: "9" -> "9r".
 */

const FOLIO = /^0*(\d+)\s*([rv])?[ab]?$/i;
const PREFIX = /^(?:fols?\.?|ff?\.|folios?)\s*/i;

/**
 * Parse a single folio reference: "9", "9r", "009v", "fol. 2r", "200rb".
 * @param {string} ref
 * @returns {{num: string, side: 'r'|'v', key: string}|null}
 */
export function parseFolioRef(ref) {
  if (typeof ref !== 'string') return null;
  const match = ref.trim().replace(PREFIX, '').match(FOLIO);
  if (!match) return null;
  const num = match[1];
  if (num === '0') return null;
  const side = (match[2] || 'r').toLowerCase();
  return { num, side, key: `${num}${side}` };
}

/** First folio of a range or alternative: "1-8", "38/39", "17-21, 22". */
function firstOfRange(ref) {
  if (typeof ref !== 'string') return null;
  return ref.split(/\s*[-–—/,;]\s*/)[0];
}

/**
 * The plain label of a canvas from manifest JSON (a IIIF 3 language map) or a string.
 * @param {Object} canvas
 * @returns {string}
 */
export function canvasLabel(canvas) {
  const label = canvas?.label;
  if (typeof label === 'string') return label;
  if (label && typeof label === 'object') {
    const values = label.en || label.none || Object.values(label)[0];
    if (Array.isArray(values)) return values[0] ?? '';
    if (typeof values === 'string') return values;
  }
  return '';
}

/**
 * Map folio keys ("9r") to canvas ids. Only canvases whose whole label is a
 * folio are indexed; the first canvas wins.
 * @param {Array<{id: string, label: *}>} canvases - Manifest items
 * @returns {Map<string, string>}
 */
export function buildFolioIndex(canvases) {
  const index = new Map();
  for (const canvas of canvases || []) {
    if (!canvas?.id) continue;
    const label = canvasLabel(canvas).trim();
    const match = label.match(/^0*(\d+)([rv])?$/i);
    if (!match) continue;
    const key = `${match[1]}${(match[2] || 'r').toLowerCase()}`;
    if (!index.has(key)) index.set(key, canvas.id);
  }
  return index;
}

/**
 * Find the canvas of a locus: its from attribute first, then its text. Ranges
 * resolve to their first folio.
 * @param {{from?: string|null, text?: string}} locus
 * @param {Map<string, string>} index
 * @returns {{canvasId: string, folio: string}|null}
 */
export function resolveLocus(locus, index) {
  if (!locus || !index || index.size === 0) return null;
  for (const candidate of [locus.from, locus.text]) {
    const ref = parseFolioRef(firstOfRange(candidate));
    if (ref && index.has(ref.key)) return { canvasId: index.get(ref.key), folio: ref.key };
  }
  return null;
}
