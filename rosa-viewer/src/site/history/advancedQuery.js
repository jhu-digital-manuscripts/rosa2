/**
 * Text form of an advanced search, as used in the `q` parameter of advanced
 * search URLs.
 *
 * The grammar is the one the previous AoR and DLMM viewers put in their URLs, so
 * old bookmarks keep working:
 *
 *   term      := field ":" quoted ("+" quoted)?
 *   quoted    := "'" chars "'"                 (\ escapes ' and \ in chars)
 *   query     := term | "(" query (op query)* ")"
 *   op        := "&" (AND) | "|" (OR)
 *
 * The old viewer nested groups whenever the operator changed, e.g.
 * `(a:'x' & (b:'y' | c:'z'))`. Advanced search here is a flat list of rows, each
 * with the operator joining it to the rows before it, so parsing flattens groups
 * the same way the old viewer did: the operator in front of a group applies to
 * the group's first term. Formatting writes the flat form `(a:'x' & b:'y' | c:'z')`.
 *
 * The optional `+ quoted` part is the extra free text a row for an enumerated
 * field with sub-fields can carry beside its chosen value, e.g. `mark:'dash'+'regnum'`.
 * The value may then be empty: `mark:''+'regnum'`. The old viewers never wrote it.
 */

/**
 * Field names used by the previous viewers that have since been renamed.
 */
export const LEGACY_FIELD_ALIASES = {
  repo: 'repository',
  place: 'locations',
  book: 'books',
};

const OPERATORS = { '&': 'AND', '|': 'OR' };
const OPERATOR_SYMBOLS = { AND: '&', OR: '|' };

function escapeValue(value) {
  return value.replace(/\\/g, '\\\\').replace(/'/g, "\\'");
}

function hasText(value) {
  return Boolean(value && value.trim());
}

function formatTerm(row) {
  const term = `${row.field}:'${escapeValue((row.value || '').trim())}'`;
  return hasText(row.text) ? `${term}+'${escapeValue(row.text.trim())}'` : term;
}

/**
 * Format advanced search rows as query text.
 *
 * Rows without a field, or with neither a value nor extra text, are left out, as
 * they are when searching.
 *
 * @param {Array<{field: string, value: string, text?: string, operator?: string}>} rows
 * @returns {string} Query text, or '' when there is nothing to search for
 */
export function formatAdvancedQuery(rows = []) {
  const terms = rows.filter((row) => row.field && (hasText(row.value) || hasText(row.text)));
  if (terms.length === 0) {
    return '';
  }

  const text = terms
    .map((row, index) => {
      const term = formatTerm(row);
      if (index === 0) {
        return term;
      }
      return ` ${OPERATOR_SYMBOLS[row.operator] || '&'} ${term}`;
    })
    .join('');

  return terms.length === 1 ? text : `(${text})`;
}

class Input {
  constructor(text) {
    this.text = text;
    this.pos = 0;
  }

  skipWhitespace() {
    while (this.pos < this.text.length && /\s/.test(this.text[this.pos])) {
      this.pos += 1;
    }
  }

  peek() {
    return this.text[this.pos];
  }

  next() {
    if (this.pos >= this.text.length) {
      throw new Error('Unexpected end of query');
    }
    const c = this.text[this.pos];
    this.pos += 1;
    return c;
  }

  more() {
    return this.pos < this.text.length;
  }
}

function parseField(input) {
  input.skipWhitespace();
  const start = input.pos;
  while (input.more() && /[\p{L}\p{N}_-]/u.test(input.peek())) {
    input.pos += 1;
  }
  if (input.pos === start) {
    throw new Error('Expecting a field name');
  }
  return input.text.slice(start, input.pos);
}

function parseValue(input) {
  input.skipWhitespace();
  if (input.next() !== "'") {
    throw new Error("A value must start with '");
  }

  let value = '';
  for (;;) {
    const c = input.next();
    if (c === '\\') {
      value += input.next();
    } else if (c === "'") {
      return value;
    } else {
      value += c;
    }
  }
}

function parseTerm(input) {
  const field = parseField(input);
  input.skipWhitespace();
  if (input.next() !== ':') {
    throw new Error("Expecting ':' after a field name");
  }
  const value = parseValue(input);

  input.skipWhitespace();
  if (input.peek() === '+') {
    input.next();
    const text = parseValue(input);
    return [text ? { field, value, text } : { field, value }];
  }
  return [{ field, value }];
}

function parseGroup(input) {
  input.next(); // '('
  const terms = parseQuery(input);

  for (;;) {
    input.skipWhitespace();
    const c = input.next();
    if (c === ')') {
      return terms;
    }
    const operator = OPERATORS[c];
    if (!operator) {
      throw new Error('Expecting & or |');
    }
    const group = parseQuery(input);
    group[0] = { ...group[0], operator };
    terms.push(...group);
  }
}

function parseQuery(input) {
  input.skipWhitespace();
  return input.peek() === '(' ? parseGroup(input) : parseTerm(input);
}

/**
 * Parse query text into advanced search rows.
 *
 * Renamed fields from the previous viewers are mapped to their current names.
 *
 * @param {string} text - Query text
 * @returns {Array<{field: string, value: string, text?: string, operator: string}>|null} Rows, or
 *   null when the text cannot be parsed
 */
export function parseAdvancedQuery(text) {
  if (!text || !text.trim()) {
    return null;
  }

  try {
    const input = new Input(text);
    const terms = parseQuery(input);
    input.skipWhitespace();
    if (input.more()) {
      return null;
    }
    return terms.map((term) => ({
      field: LEGACY_FIELD_ALIASES[term.field] || term.field,
      value: term.value,
      ...(term.text ? { text: term.text } : {}),
      operator: term.operator || 'AND',
    }));
  } catch {
    return null;
  }
}
