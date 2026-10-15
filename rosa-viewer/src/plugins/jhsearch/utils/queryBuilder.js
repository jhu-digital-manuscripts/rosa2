/**
 * Opensearch query construction utilities.
 */

import { buildCategoryFilter } from './facets';
import { buildAllowedBooksFilter, buildMatchedBookAggregations } from './bookFacets';

/**
 * Facet key holding the manifest ids picked in the Name filter. It is stored with the
 * other facets so it shares their URL encoding (`f.id=rose.Douce195`) and clearing.
 */
export const NAME_FACET = 'id';

/** Fields of the `label_suggest` search_as_you_type field that a bool_prefix query uses. */
export const NAME_SUGGEST_FIELDS = [
  'label_suggest',
  'label_suggest._2gram',
  'label_suggest._3gram',
];

/** Source fields the Name filter needs to show a book. */
const NAME_SOURCE = ['id', 'label', 'logo'];

/**
 * Build the filter clause for books picked in the Name filter.
 *
 * A manifest's `id` is the book id, but a canvas's `id` is the page id and its book
 * is in `manifest_id`. Searches run over both indexes, so either has to match; the
 * selected ids are ORed.
 *
 * @param {string[]} ids - Manifest ids
 * @returns {Object|null} Filter clause, or null when nothing is selected
 */
export function buildNameFilter(ids) {
  if (!ids || ids.length === 0) {
    return null;
  }
  const values = ids.map(String);
  return {
    bool: {
      should: [{ terms: { id: values } }, { terms: { manifest_id: values } }],
      minimum_should_match: 1,
    },
  };
}

/**
 * Expand field names that have subfields into wildcard patterns.
 * e.g., 'marginalia' with has_subfields=true becomes ['marginalia.*']
 * Fields without subfields are passed through unchanged.
 *
 * @param {string[]} fieldNames - Array of field names to expand
 * @param {Object[]} fieldDefinitions - Field definitions from jhsearch.json
 * @returns {string[]} Expanded field names with wildcards where appropriate
 */
export function expandFieldsWithSubfields(fieldNames, fieldDefinitions) {
  const expanded = [];

  for (const name of fieldNames) {
    const fieldDef = fieldDefinitions.find((f) => f.name === name);
    if (fieldDef?.has_subfields) {
      // Use wildcard to match all subfields
      expanded.push(`${name}.*`);
    } else {
      expanded.push(name);
    }
  }

  return expanded;
}

/**
 * Whether an advanced search row has anything to search for: a value, or the
 * optional extra text that enumerated fields with sub-fields accept.
 *
 * @param {{value?: string, text?: string}} row - Advanced search row
 * @returns {boolean}
 */
export function rowHasQuery(row) {
  return Boolean((row.value && row.value.trim()) || (row.text && row.text.trim()));
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
 * Categories with a 'quantize-interval' are aggregated as histograms, so their selected
 * values are bucket lower bounds standing for a range. Those become `range` clauses;
 * everything else becomes a `terms` clause. See utils/facets.js.
 *
 * Given the facet fields of every book in the collection, the category selections
 * instead become one clause naming the books they allow, which also keeps those
 * books' pages. See utils/bookFacets.js.
 *
 * @param {Object} facets - Object mapping category names to selected values
 * @param {Object[]} [categories] - Category definitions from jhsearch.json
 * @param {Object[]|null} [books] - Facet fields of every book in the collection
 * @returns {Object[]} Array of filter clauses
 */
export function buildFacetFilters(facets, categories = [], books = null) {
  const filters = [];
  const nameFilter = buildNameFilter(facets[NAME_FACET]);
  if (nameFilter) {
    filters.push(nameFilter);
  }

  const categoryFacets = Object.entries(facets).filter(([name]) => name !== NAME_FACET);

  if (books) {
    const filter = buildAllowedBooksFilter(books, Object.fromEntries(categoryFacets), categories);
    if (filter) {
      filters.push(filter);
    }
    return filters;
  }

  for (const [name, values] of categoryFacets) {
    const filter = buildCategoryFilter(
      name,
      values,
      categories.find((c) => c.name === name)
    );
    if (filter) {
      filters.push(filter);
    }
  }

  return filters;
}

/**
 * The aggregations for a search: facet aggregations, or with the facet fields of every
 * book loaded, the ids of the books matched so the facets can be counted locally.
 *
 * @param {Object[]} categories - Category definitions from jhsearch.json
 * @param {Object[]|null} books - Facet fields of every book in the collection
 * @returns {Object} Aggregations configuration for Opensearch
 */
function buildSearchAggregations(categories, books) {
  return books ? buildMatchedBookAggregations() : buildFacetAggregations(categories);
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
 * Build the sort clause for a sort field, or null for relevance ordering.
 *
 * `page_num` only exists in the canvas mapping, and a simple or advanced search
 * runs against manifest and canvas together. Sorting on a field a shard does not
 * know about is an error there, not an empty value: without `unmapped_type` every
 * manifest shard fails with `No mapping found for [page_num] in order to sort
 * on`. Opensearch still answers 200 with whatever the canvas shards returned, so
 * the symptom is manifests silently vanishing from the results rather than a
 * visible failure. `unmapped_type` makes those shards treat the field as absent,
 * which is what `missing: '_last'` then acts on to put manifests at the end.
 *
 * @param {string} sortField - Field to sort on, or '_score' for relevance
 * @param {string} sortOrder - 'asc' or 'desc'
 * @returns {Object[]|null} Opensearch sort clause, or null to leave it unset
 */
export function buildSortClause(sortField, sortOrder) {
  if (sortField === '_score') {
    return null;
  }

  if (sortField === 'page_num') {
    return [{ [sortField]: { order: sortOrder, missing: '_last', unmapped_type: 'long' } }];
  }

  return [{ [sortField]: sortOrder }];
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
 * @param {Object[]|null} [options.books] - Facet fields of every book in the collection, to
 *   work facets out locally (see utils/bookFacets.js)
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
  books = null,
  sortField = 'label.keyword',
  sortOrder = 'asc',
}) {
  const filters = [
    { terms: { collection_id: [collectionId] } },
    ...buildFacetFilters(facets, categories, books),
  ];

  return {
    query: {
      bool: {
        filter: filters,
      },
    },
    from: page * pageSize,
    size: pageSize,
    sort: buildSortClause(sortField, sortOrder) || [{ 'label.keyword': 'asc' }],
    aggs: buildSearchAggregations(categories, books),
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
 * @param {Object[]|null} [options.books] - Facet fields of every book in the collection, to
 *   work facets out locally (see utils/bookFacets.js)
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
  books = null,
  sortField = '_score',
  sortOrder = 'desc',
}) {
  const searchFields = expandFieldsWithSubfields(defaultFields, fieldDefinitions);
  const filters = [
    { terms: { collection_id: [collectionId] } },
    ...buildFacetFilters(facets, categories, books),
  ];

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
    aggs: buildSearchAggregations(categories, books),
  };

  // Only add sort if not using relevance (default)
  const sort = buildSortClause(sortField, sortOrder);
  if (sort) {
    body.sort = sort;
  }

  return body;
}

/**
 * Build an advanced search query with multiple field-specific clauses.
 *
 * @param {Object} options - Query options
 * @param {Object[]} options.rows - Search rows with field, value, optional text, operator
 * @param {Object[]} options.fieldDefinitions - Field definitions for subfield expansion
 * @param {string} options.collectionId - Collection ID to filter by
 * @param {Object} options.facets - Selected facet values
 * @param {number} options.page - Page number (0-based)
 * @param {number} options.pageSize - Results per page
 * @param {Object[]} options.categories - Category definitions for aggregations
 * @param {Object[]|null} [options.books] - Facet fields of every book in the collection, to
 *   work facets out locally (see utils/bookFacets.js)
 * @param {string} options.sortField - Sort field
 * @param {string} options.sortOrder - Sort order
 * @returns {Object} Opensearch query body
 */
export function buildAdvancedSearchQuery({
  rows,
  fieldDefinitions = [],
  collectionId,
  facets = {},
  page = 0,
  pageSize = 20,
  categories = [],
  books = null,
  sortField = '_score',
  sortOrder = 'desc',
}) {
  // Build clause array from rows
  // Skip rows for fields the collection does not have, such as ones from an old
  // bookmarked URL; the search proxy would reject them.
  const isKnownField = (field) =>
    fieldDefinitions.length === 0 || fieldDefinitions.some((f) => f.name === field);
  const clauses = rows
    .filter((row) => row.field && rowHasQuery(row) && isKnownField(row.field))
    .map((row) => {
      const fieldDef = fieldDefinitions.find((f) => f.name === row.field);
      const searchFields = fieldDef?.has_subfields ? [`${row.field}.*`] : [row.field];
      const match = (query) => ({
        multi_match: {
          query,
          fields: searchFields,
          type: 'best_fields',
        },
      });

      // A value picked from a dropdown is an exact keyword: match it with a term
      // query on the keyword sub-field, or on the field itself when it is a plain
      // keyword field, rather than running it through every analyzed sub-field.
      const value = row.value && row.value.trim();
      const text = row.text && row.text.trim();
      const hasValues = Boolean(fieldDef?.values && fieldDef.values.length > 0);
      const valueClause = !value
        ? null
        : hasValues
          ? { term: { [fieldDef.has_subfields ? `${row.field}.keyword` : row.field]: value } }
          : match(row.value);

      // A row for an enumerated field with sub-fields can carry free text as well
      // as the chosen value. Both must match, so 'dash' plus 'regnum' finds pages
      // with a dash mark and 'regnum' in the marks' referenced text.
      const parts = [valueClause, text ? match(text) : null].filter(Boolean);
      const clause = parts.length === 1 ? parts[0] : { bool: { must: parts } };

      return {
        clause,
        operator: row.operator || 'AND',
        searchFields,
      };
    });

  if (clauses.length === 0) {
    // No valid search terms, return browse query
    return buildBrowseQuery({ collectionId, facets, page, pageSize, categories, books });
  }

  // Combine clauses with AND binding tighter than OR: an OR row starts a new group,
  // the rows in a group are ANDed, and the groups are ORed. So `a AND b OR c` is
  // `(a AND b) OR c`. The first row's operator is ignored.
  const groups = [];
  const allSearchFields = [];

  clauses.forEach((item, index) => {
    allSearchFields.push(...item.searchFields);

    if (index === 0 || item.operator === 'OR') {
      groups.push([item.clause]);
    } else {
      groups[groups.length - 1].push(item.clause);
    }
  });

  const filters = [
    { terms: { collection_id: [collectionId] } },
    ...buildFacetFilters(facets, categories, books),
  ];

  const boolQuery =
    groups.length === 1
      ? { must: groups[0], filter: filters }
      : {
          should: groups.map((group) =>
            group.length === 1 ? group[0] : { bool: { must: group } }
          ),
          minimum_should_match: 1,
          filter: filters,
        };

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
    aggs: buildSearchAggregations(categories, books),
  };

  const sort = buildSortClause(sortField, sortOrder);
  if (sort) {
    body.sort = sort;
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

/**
 * Build the type-ahead query for the Name filter, run against the manifest index.
 *
 * Every typed word must match, the last one as a prefix. Suggestions are limited to
 * the selected collection and the other active facets, but not to the books already
 * picked, so the user can add more.
 *
 * @param {Object} options
 * @param {string} options.text - Text typed so far
 * @param {string} options.collectionId - Collection ID to filter by
 * @param {Object} [options.facets] - Selected facet values
 * @param {Object[]} [options.categories] - Category definitions from jhsearch.json
 * @param {number} [options.size] - Maximum number of suggestions
 * @returns {Object} Opensearch query body
 */
export function buildNameSuggestQuery({
  text,
  collectionId,
  facets = {},
  categories = [],
  size = 10,
}) {
  const otherFacets = { ...facets };
  delete otherFacets[NAME_FACET];

  return {
    query: {
      bool: {
        must: [
          {
            multi_match: {
              query: text,
              type: 'bool_prefix',
              operator: 'and',
              fields: NAME_SUGGEST_FIELDS,
            },
          },
        ],
        filter: [
          { terms: { collection_id: [collectionId] } },
          ...buildFacetFilters(otherFacets, categories),
        ],
      },
    },
    size,
    _source: NAME_SOURCE,
  };
}

/**
 * Build a query fetching the label and logo of the given manifests, used to show
 * Name filter selections restored from a URL.
 *
 * @param {string[]} ids - Manifest ids
 * @returns {Object} Opensearch query body
 */
export function buildNameLookupQuery(ids) {
  const values = ids.map(String);
  return {
    query: {
      bool: {
        filter: [{ terms: { id: values } }],
      },
    },
    size: values.length,
    _source: NAME_SOURCE,
  };
}
