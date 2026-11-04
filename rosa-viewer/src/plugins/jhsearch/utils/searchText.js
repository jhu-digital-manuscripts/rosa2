/**
 * Search text syntax: quoted phrases and bare words.
 *
 * A phrase is text between double quotes. Straight quotes and the curly quotes the
 * annotation panel renders referenced text with (`“…”`) all count, so a quotation
 * copied from an annotation searches as a phrase. A quote with no closing quote runs
 * to the end of the text. Everything outside quotes is bare words.
 *
 * How the units combine is up to the query builder (see queryBuilder.js,
 * buildTextClause).
 */

const QUOTES = /["“”]/;

function collapse(text) {
  return text.replace(/\s+/g, ' ').trim();
}

/**
 * Split search text into bare words and quoted phrases.
 *
 * @param {string} text - Search text as typed
 * @returns {{words: string, phrases: string[]}} The bare words with whitespace collapsed
 *   ('' when there are none) and the phrases in order, blank phrases left out
 */
export function parseSearchText(text) {
  const source = text || '';
  const phrases = [];
  let words = '';
  let phrase = null;

  for (const c of source) {
    if (QUOTES.test(c)) {
      if (phrase === null) {
        phrase = '';
      } else {
        if (collapse(phrase)) {
          phrases.push(collapse(phrase));
        }
        phrase = null;
      }
      // A quote separates words on either side of it
      words += ' ';
    } else if (phrase !== null) {
      phrase += c;
    } else {
      words += c;
    }
  }

  if (phrase !== null && collapse(phrase)) {
    phrases.push(collapse(phrase));
  }

  return { words: collapse(words), phrases };
}
