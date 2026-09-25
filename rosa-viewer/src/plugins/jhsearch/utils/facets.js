/**
 * Facet value semantics.
 *
 * A category declared in `jhsearch.json` with a `quantize-interval` is aggregated with a
 * histogram rather than a terms aggregation. A histogram returns one bucket per interval,
 * keyed by the bucket's lower bound, so the bucket key `20` with interval `10` stands for
 * the half-open range `[20, 30)` — not the single value `20`.
 *
 * Both the filter sent to Opensearch and the label shown to the user have to be derived
 * from that range. Filtering a quantized bucket with a `terms` query matches only
 * documents whose value equals the lower bound exactly, which is almost always a tiny
 * subset of the bucket the user clicked on.
 *
 * Buckets are uniform: there is no special case for zero. With an interval of 20, the
 * first bucket is `0-19` and covers zero along with everything up to 19.
 */

/**
 * Get the histogram interval for a category, or null if it is not quantized.
 *
 * @param {Object} category - Category definition from jhsearch.json
 * @returns {number|null} A positive interval, or null for a plain terms category
 */
export function getQuantizeInterval(category) {
  const interval = Number(category?.['quantize-interval']);
  return Number.isFinite(interval) && interval > 0 ? interval : null;
}

/**
 * Get the half-open numeric range a histogram bucket represents.
 *
 * @param {number|string} bucketKey - The bucket key (its lower bound; may be a float like 20.0)
 * @param {number} interval - The histogram interval
 * @returns {{gte: number, lt: number}|null} Range bounds, or null if the key is not numeric
 */
export function getBucketRange(bucketKey, interval) {
  // Guard explicitly: null, undefined, '' and booleans all coerce to a number, which
  // would silently produce a filter for the wrong bucket.
  if (bucketKey === null || bucketKey === undefined || bucketKey === '') {
    return null;
  }
  if (typeof bucketKey !== 'number' && typeof bucketKey !== 'string') {
    return null;
  }

  const from = Number(bucketKey);
  if (!Number.isFinite(from)) {
    return null;
  }
  return { gte: from, lt: from + interval };
}

/**
 * Format a facet value for display.
 *
 * Quantized buckets render as an inclusive range ("20-29"), booleans as Yes/No, and
 * everything else as its string form.
 *
 * @param {*} value - The bucket key
 * @param {Object} [category] - Category definition from jhsearch.json
 * @returns {string} Label to show in the facet list
 */
export function formatFacetValue(value, category) {
  const interval = getQuantizeInterval(category);

  if (interval !== null) {
    const range = getBucketRange(value, interval);
    if (range) {
      // Inclusive upper bound reads more naturally than the half-open bound.
      return `${range.gte}-${range.lt - 1}`;
    }
  }

  if (value === true) return 'Yes';
  if (value === false) return 'No';

  return String(value) || '(empty)';
}

/**
 * Build the Opensearch filter clause for one category's selected values.
 *
 * @param {string} name - The Opensearch field name
 * @param {Array} values - Selected bucket keys
 * @param {Object} [category] - Category definition from jhsearch.json
 * @returns {Object|null} A filter clause, or null if there is nothing to filter on
 */
export function buildCategoryFilter(name, values, category) {
  if (!values || values.length === 0) {
    return null;
  }

  const interval = getQuantizeInterval(category);
  if (interval === null) {
    return { terms: { [name]: values } };
  }

  // Each selected bucket becomes a range; multiple buckets are OR-ed together.
  const ranges = values
    .map((value) => getBucketRange(value, interval))
    .filter((range) => range !== null)
    .map((range) => ({ range: { [name]: range } }));

  if (ranges.length === 0) {
    return null;
  }

  return { bool: { should: ranges, minimum_should_match: 1 } };
}
