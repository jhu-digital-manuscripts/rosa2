/**
 * Opensearch query construction utilities.
 */

/**
 * Expand field names that have subfields into their full paths.
 * e.g., 'marginalia' with subfields ['en', 'fr'] becomes ['marginalia.en', 'marginalia.fr']
 *
 * @param {string[]} fieldNames - Array of field names to expand
 * @param {Object[]} fieldDefinitions - Field definitions from jhsearch.json
 * @returns {string[]} Expanded field names
 */
export function expandFieldsWithSubfields(fieldNames, fieldDefinitions) {
  const expanded = [];

  for (const name of fieldNames) {
    const fieldDef = fieldDefinitions.find((f) => f.name === name);
    if (fieldDef?.subfields && fieldDef.subfields.length > 0) {
      // Expand to all subfields
      for (const subfield of fieldDef.subfields) {
        expanded.push(`${name}.${subfield}`);
      }
    } else {
      expanded.push(name);
    }
  }

  return expanded;
}

/**
 * Build highlight configuration for search fields.
 *
 * @param {string[]} fields - Field names to highlight
 * @returns {Object} Highlight configuration for Opensearch
 */
export function buildHighlightFields(fields) {
  const highlightFields = {};
  for (const field of fields) {
    highlightFields[field] = {};
  }
  return highlightFields;
}

/**
 * Build facet filter clauses from selected facets.
 *
 * @param {Object} facets - Object mapping category names to selected values
 * @returns {Object[]} Array of filter clauses
 */
export function buildFacetFilters(facets) {
  const filters = [];

  for (const [category, values] of Object.entries(facets)) {
    if (values && values.length > 0) {
      filters.push({
        terms: { [category]: values },
      });
    }
  }

  return filters;
}

/**
 * Build aggregations for facet categories.
 *
 * Categories with a 'quantize-interval' property use histogram aggregation
 * for numeric bucketing. All other categories use terms aggregation.
 *
 * @param {Object[]} categories - Category definitions from jhsearch.json
 * @returns {Object} Aggregations configuration for Opensearch
 */
export function buildFacetAggregations(categories) {
  const aggs = {};

  for (const category of categories) {
    if (category['quantize-interval']) {
      // Use histogram aggregation for numeric fields with quantize-interval
      aggs[category.name] = {
        histogram: {
          field: category.name,
          interval: category['quantize-interval'],
          min_doc_count: 1,
        },
      };
    } else {
      aggs[category.name] = {
        terms: {
          field: category.name,
          size: 100, // Return up to 100 facet values
        },
      };
    }
  }

  return aggs;
}

/**
 * Build a browse query (no search terms, just collection filtering and facets).
 *
 * @param {Object} options - Query options
 * @param {string} options.collectionId - Collection ID to filter by
 * @param {Object} options.facets - Selected facet values
 * @param {number} options.page - Page number (0-based)
 * @param {number} options.pageSize - Results per page
 * @param {Object[]} options.categories - Category definitions for aggregations
 * @param {string} options.sortField - Sort field ('_score' or 'label.keyword')
 * @param {string} options.sortOrder - Sort order ('asc' or 'desc')
 * @returns {Object} Opensearch query body
 */
export function buildBrowseQuery({
  collectionId,
  facets = {},
  page = 0,
  pageSize = 20,
  categories = [],
  sortField = 'label.keyword',
  sortOrder = 'asc',
}) {
  const filters = [{ terms: { collection_id: [collectionId] } }, ...buildFacetFilters(facets)];

  return {
    query: {
      bool: {
        filter: filters,
      },
    },
    from: page * pageSize,
    size: pageSize,
    sort: [{ [sortField]: sortOrder }],
    aggs: buildFacetAggregations(categories),
  };
}

/**
 * Build a simple search query.
 *
 * @param {Object} options - Query options
 * @param {string} options.query - Search query string
 * @param {string[]} options.defaultFields - Default fields to search
 * @param {Object[]} options.fieldDefinitions - Field definitions for subfield expansion
 * @param {string} options.collectionId - Collection ID to filter by
 * @param {Object} options.facets - Selected facet values
 * @param {number} options.page - Page number (0-based)
 * @param {number} options.pageSize - Results per page
 * @param {Object[]} options.categories - Category definitions for aggregations
 * @param {string} options.sortField - Sort field
 * @param {string} options.sortOrder - Sort order
 * @returns {Object} Opensearch query body
 */
export function buildSimpleSearchQuery({
  query,
  defaultFields,
  fieldDefinitions,
  collectionId,
  facets = {},
  page = 0,
  pageSize = 20,
  categories = [],
  sortField = '_score',
  sortOrder = 'desc',
}) {
  const searchFields = expandFieldsWithSubfields(defaultFields, fieldDefinitions);
  const filters = [{ terms: { collection_id: [collectionId] } }, ...buildFacetFilters(facets)];

  const body = {
    query: {
      bool: {
        must: [
          {
            multi_match: {
              query,
              fields: searchFields,
              type: 'best_fields',
            },
          },
        ],
        filter: filters,
      },
    },
    from: page * pageSize,
    size: pageSize,
    highlight: {
      fields: buildHighlightFields(searchFields),
      pre_tags: ['<mark>'],
      post_tags: ['</mark>'],
    },
    aggs: buildFacetAggregations(categories),
  };

  // Only add sort if not using relevance (default)
  if (sortField !== '_score') {
    body.sort = [{ [sortField]: sortOrder }];
  }

  return body;
}

/**
 * Build an advanced search query with multiple field-specific clauses.
 *
 * @param {Object} options - Query options
 * @param {Object[]} options.rows - Search rows with field, value, operator
 * @param {Object[]} options.fieldDefinitions - Field definitions for subfield expansion
 * @param {string} options.collectionId - Collection ID to filter by
 * @param {Object} options.facets - Selected facet values
 * @param {number} options.page - Page number (0-based)
 * @param {number} options.pageSize - Results per page
 * @param {Object[]} options.categories - Category definitions for aggregations
 * @param {string} options.sortField - Sort field
 * @param {string} options.sortOrder - Sort order
 * @returns {Object} Opensearch query body
 */
export function buildAdvancedSearchQuery({
  rows,
  fieldDefinitions,
  collectionId,
  facets = {},
  page = 0,
  pageSize = 20,
  categories = [],
  sortField = '_score',
  sortOrder = 'desc',
}) {
  // Build clause array from rows
  const clauses = rows
    .filter((row) => row.value && row.value.trim())
    .map((row) => {
      const fieldDef = fieldDefinitions.find((f) => f.name === row.field);
      const searchFields =
        fieldDef?.subfields && fieldDef.subfields.length > 0
          ? fieldDef.subfields.map((sf) => `${row.field}.${sf}`)
          : [row.field];

      return {
        clause: {
          multi_match: {
            query: row.value,
            fields: searchFields,
            type: 'best_fields',
          },
        },
        operator: row.operator || 'AND',
        searchFields,
      };
    });

  if (clauses.length === 0) {
    // No valid search terms, return browse query
    return buildBrowseQuery({ collectionId, facets, page, pageSize, categories });
  }

  // Combine clauses: first clause is always required, subsequent use their operator
  const must = [];
  const should = [];
  const allSearchFields = [];

  clauses.forEach((item, index) => {
    allSearchFields.push(...item.searchFields);

    if (index === 0 || item.operator === 'AND') {
      must.push(item.clause);
    } else {
      should.push(item.clause);
    }
  });

  const filters = [{ terms: { collection_id: [collectionId] } }, ...buildFacetFilters(facets)];

  const boolQuery = {
    must,
    filter: filters,
  };

  if (should.length > 0) {
    boolQuery.should = should;
    boolQuery.minimum_should_match = 1;
  }

  const body = {
    query: {
      bool: boolQuery,
    },
    from: page * pageSize,
    size: pageSize,
    highlight: {
      fields: buildHighlightFields([...new Set(allSearchFields)]),
      pre_tags: ['<mark>'],
      post_tags: ['</mark>'],
    },
    aggs: buildFacetAggregations(categories),
  };

  if (sortField !== '_score') {
    body.sort = [{ [sortField]: sortOrder }];
  }

  return body;
}

/**
 * Build a query to get collection aggregations.
 *
 * @param {string[]} collectionIds - Collection IDs to include
 * @returns {Object} Opensearch query body
 */
export function buildCollectionAggregationQuery(collectionIds) {
  return {
    query: {
      bool: {
        filter: [{ terms: { collection_id: collectionIds } }],
      },
    },
    size: 0,
    aggs: {
      collections: {
        terms: {
          field: 'collection_id',
          size: 100,
        },
      },
    },
  };
}
