/**
 * Names for canvases in viewer URLs.
 *
 * Canvas URIs use a 0-based index (`.../rose/Add12042/canvas/14`), which changes
 * if images are added to a book. URLs name a canvas by its image instead, as the
 * previous viewers did: image `Add12042.001v` is `001v`. The image name is read
 * from the canvas's IIIF image service, whose identifier ends in the
 * percent-encoded image path (`rose%2FAdd12042%2FAdd12042.001v`).
 *
 * A canvas without an image of its own (such as one showing the collection's
 * missing-image placeholder) is named by its index, as `i14`.
 */

const INDEX_PREFIX = 'i';

function first(value) {
  return Array.isArray(value) ? value[0] : value;
}

/**
 * The image identifier painted on a IIIF Presentation 3 canvas, decoded, e.g.
 * 'rose/Add12042/Add12042.001v'.
 * @param {Object} canvas - Canvas JSON
 * @returns {string|null}
 */
export function canvasImageId(canvas) {
  const body = first(first(first(canvas?.items)?.items)?.body);
  const service = first(body?.service);
  const serviceId = service?.id || service?.['@id'];
  if (!serviceId) return null;

  const segment = serviceId.replace(/\/+$/, '').split('/').pop();
  try {
    return decodeURIComponent(segment);
  } catch {
    return segment;
  }
}

/**
 * The URL name of a canvas.
 * @param {Object} canvas - Canvas JSON
 * @param {number} index - Index of the canvas in the manifest
 * @param {string} bookId - Book id, e.g. 'Add12042'
 * @returns {string}
 */
export function canvasName(canvas, index, bookId) {
  const imageId = canvasImageId(canvas);
  const file = imageId ? imageId.split('/').pop() : '';
  const prefix = `${bookId}.`;
  return file.startsWith(prefix) && file.length > prefix.length
    ? file.slice(prefix.length)
    : `${INDEX_PREFIX}${index}`;
}

function canvasesOf(manifest) {
  return Array.isArray(manifest?.items) ? manifest.items : [];
}

/**
 * The URL name of a canvas in a manifest.
 * @param {Object} manifest - Manifest JSON
 * @param {string} canvasId
 * @param {string} bookId
 * @returns {string|null} Name, or null when the canvas is not in the manifest
 */
export function nameForCanvasId(manifest, canvasId, bookId) {
  const canvases = canvasesOf(manifest);
  const index = canvases.findIndex((c) => c.id === canvasId);
  return index === -1 ? null : canvasName(canvases[index], index, bookId);
}

/**
 * The id of the canvas a URL name refers to.
 * @param {Object} manifest - Manifest JSON
 * @param {string} name
 * @param {string} bookId
 * @returns {string|null} Canvas id, or null when no canvas has that name
 */
export function canvasIdForName(manifest, name, bookId) {
  if (!name) return null;
  const canvases = canvasesOf(manifest);

  const match = canvases.find((c, i) => canvasName(c, i, bookId) === name);
  if (match) return match.id;

  const indexMatch = name.match(/^i(\d+)$/);
  if (indexMatch) {
    return canvases[Number(indexMatch[1])]?.id || null;
  }
  return null;
}
