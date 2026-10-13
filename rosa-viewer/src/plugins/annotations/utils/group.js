/**
 * Group parsed annotations for display in the sidebar.
 *
 * AoR annotations (aor-*) are grouped by where they are on the page, in reading
 * order around the page: head, left margin, in text, right margin, tail, full page.
 * The page header (aor-page) is kept apart. DLMM annotations (dlmm-*) are grouped
 * by kind. Anything else ends up in a final "Other" group.
 */

export const PLACES = [
  { key: 'head', label: 'Head' },
  { key: 'left_margin', label: 'Left margin' },
  { key: 'intext', label: 'In text' },
  { key: 'right_margin', label: 'Right margin' },
  { key: 'tail', label: 'Tail' },
  { key: 'full_page', label: 'Full page' },
];

export const DLMM_KINDS = [
  { key: 'illustration', label: 'Illustration' },
  { key: 'transcription', label: 'Transcription' },
  { key: 'commentary', label: 'Commentary' },
];

const OTHER = { key: 'other', label: 'Other annotations' };

/**
 * Classify one annotation.
 * @param {string[]} classes
 * @returns {{kind: ('header'|'place'|'dlmm'|'other'), key: string}}
 */
export function classify(classes = []) {
  if (classes.includes('aor-page')) return { kind: 'header', key: 'page' };
  const place = classes.find((c) => c.startsWith('aor-place-'));
  if (place) {
    const key = place.slice('aor-place-'.length);
    if (PLACES.some((p) => p.key === key)) return { kind: 'place', key };
  }
  for (const k of DLMM_KINDS) {
    if (classes.includes(`dlmm-${k.key}`)) return { kind: 'dlmm', key: k.key };
  }
  return { kind: 'other', key: OTHER.key };
}

/**
 * @param {Array<{classes: string[]}>} items - Parsed annotations in document order
 * @returns {{header: Array, groups: Array<{key: string, kind: string, label: string, items: Array}>}}
 *   Groups are in display order and only include non-empty ones.
 */
export function groupAnnotations(items = []) {
  const header = [];
  const buckets = new Map();
  for (const item of items) {
    if (!item) continue;
    const { kind, key } = classify(item.classes);
    if (kind === 'header') {
      header.push(item);
      continue;
    }
    const id = `${kind}:${key}`;
    if (!buckets.has(id)) buckets.set(id, { kind, key, items: [] });
    buckets.get(id).items.push(item);
  }

  const groups = [];
  const take = (kind, defs) => {
    for (const def of defs) {
      const bucket = buckets.get(`${kind}:${def.key}`);
      if (bucket) groups.push({ ...bucket, label: def.label });
    }
  };
  take('place', PLACES);
  take('dlmm', DLMM_KINDS);
  take('other', [OTHER]);
  return { header, groups };
}
