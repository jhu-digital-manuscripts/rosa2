/**
 * Parse a TEI book description into a plain model for display.
 *
 * Only the notes of <notesStmt> are read. Each note becomes a section, headed by
 * its rend attribute ("BASIC INFORMATION" -> "Basic information"); notes without
 * rend, such as "Description by ...", become unheaded sections. The model holds
 * only text and a small set of node types, so it can be rendered as React
 * elements without any HTML from the file reaching the page.
 *
 * Model:
 *   { sections: [{ key, heading, blocks }] }
 *   block  = { type: 'paragraph', content: Inline[] }
 *          | { type: 'list', caption: Inline[] | null, labeled: boolean,
 *              items: [{ label: string | null, content: Inline[] }] }
 *   Inline = { type: 'text', text }
 *          | { type: 'italic' | 'bold' | 'sup', children: Inline[] }
 *          | { type: 'break' }
 *          | { type: 'locus', text, from, to }
 */

export const TEI_NS = 'http://www.tei-c.org/ns/1.0';

/** The XML could not be parsed. */
export class TeiParseError extends Error {
  constructor(message) {
    super(message);
    this.name = 'TeiParseError';
  }
}

const ELEMENT = 1;
const TEXT = 3;
const CDATA = 4;

const HI_TYPES = {
  italic: 'italic',
  italics: 'italic',
  i: 'italic',
  bold: 'bold',
  b: 'bold',
  sup: 'sup',
  superscript: 'sup',
};

/**
 * "BASIC INFORMATION" -> "Basic information".
 * @param {string} rend
 * @returns {string}
 */
export function humanizeRend(rend) {
  const s = String(rend ?? '')
    .replace(/[_\s]+/g, ' ')
    .trim()
    .toLowerCase();
  return s ? s.charAt(0).toUpperCase() + s.slice(1) : '';
}

const childElements = (el, name) =>
  Array.from(el.childNodes).filter((n) => n.nodeType === ELEMENT && n.localName === name);

const attr = (el, name) => {
  const v = el.getAttribute(name);
  return v == null || v.trim() === '' ? null : v.trim();
};

/** Plain text of an element, whitespace collapsed. */
const textOf = (el) => (el.textContent || '').replace(/\s+/g, ' ').trim();

/**
 * Convert the inline content of an element. Text whitespace is collapsed;
 * normalizeInline then removes the spaces left at line starts and ends.
 */
function inlineNodes(el) {
  const out = [];
  for (const node of Array.from(el.childNodes)) {
    if (node.nodeType === TEXT || node.nodeType === CDATA) {
      out.push({ type: 'text', text: node.nodeValue.replace(/\s+/g, ' ') });
    } else if (node.nodeType === ELEMENT) {
      out.push(...inlineElement(node));
    }
  }
  return out;
}

function inlineElement(el) {
  switch (el.localName) {
    case 'lb':
      return [{ type: 'break' }];
    case 'locus':
      return [{ type: 'locus', text: textOf(el), from: attr(el, 'from'), to: attr(el, 'to') }];
    case 'hi': {
      const rend = (attr(el, 'rend') || '').toLowerCase();
      // "italcs" occurs in the archive
      const type = HI_TYPES[rend] || (rend.startsWith('ital') ? 'italic' : null);
      const children = inlineNodes(el);
      return type ? [{ type, children }] : children;
    }
    case 'title':
    case 'foreign':
    case 'emph':
      return [{ type: 'italic', children: inlineNodes(el) }];
    case 'note':
      // A nested note is an aside within the text
      return [{ type: 'text', text: ' (' }, ...inlineNodes(el), { type: 'text', text: ') ' }];
    default:
      // material, persName, unknown elements: keep their content
      return inlineNodes(el);
  }
}

const isBlankText = (n) => n.type === 'text' && n.text.trim() === '';

/**
 * Merge adjacent text, collapse spaces across node boundaries, trim the ends of
 * the content and of each line, and drop empty wrappers.
 * @param {Array} nodes
 * @returns {Array}
 */
export function normalizeInline(nodes) {
  // Spacing state is shared across wrappers so spaces are fixed across their boundaries
  const result = [];
  let pendingSpace = false;
  let atLineStart = true;

  const walk = (list, target) => {
    for (const node of list) {
      if (node.type === 'text') {
        let text = node.text;
        if (/^\s/.test(text)) pendingSpace = true;
        text = text.trim();
        if (text) {
          // "<locus>33-39</locus>(1 leaf wanting)" reads better with a space
          const afterLocus = target[target.length - 1]?.type === 'locus' && text.startsWith('(');
          if ((pendingSpace || afterLocus) && !atLineStart) appendText(target, ' ');
          appendText(target, text);
          atLineStart = false;
          pendingSpace = /\s$/.test(node.text);
        } else if (node.text) {
          pendingSpace = true;
        }
      } else if (node.type === 'break') {
        target.push({ type: 'break' });
        atLineStart = true;
        pendingSpace = false;
      } else if (node.type === 'locus') {
        if (pendingSpace && !atLineStart) appendText(target, ' ');
        target.push(node);
        atLineStart = false;
        pendingSpace = false;
      } else if (node.children) {
        if (node.children.every(isBlankText)) {
          if (node.children.length) pendingSpace = true;
          continue;
        }
        // A space before the wrapper, or at the start of its text, goes outside it
        const leading = firstText(node.children);
        if (leading && /^\s/.test(leading)) pendingSpace = true;
        if (pendingSpace && !atLineStart) appendText(target, ' ');
        pendingSpace = false;
        const wrapped = { type: node.type, children: [] };
        const wasLineStart = atLineStart;
        atLineStart = true; // no space at the start inside the wrapper
        walk(node.children, wrapped.children);
        if (wrapped.children.length) target.push(wrapped);
        else atLineStart = wasLineStart;
      }
    }
  };

  walk(nodes, result);
  // Remove breaks at the start and end
  while (result.length && result[0].type === 'break') result.shift();
  while (result.length && result[result.length - 1].type === 'break') result.pop();
  return result;
}

function firstText(nodes) {
  for (const n of nodes) {
    if (n.type === 'text') return n.text;
    if (n.children) return firstText(n.children);
    return null;
  }
  return null;
}

function appendText(target, text) {
  const last = target[target.length - 1];
  if (last && last.type === 'text') last.text += text;
  else target.push({ type: 'text', text });
}

const hasContent = (inline) =>
  inline.some((n) => n.type === 'locus' || (n.type === 'text' && n.text.trim()) || n.children);

function parseList(el) {
  const heads = childElements(el, 'head');
  const caption = heads.length ? normalizeInline(heads.flatMap((h) => inlineNodes(h))) : null;
  const itemEls = childElements(el, 'item');
  const items = itemEls.map((item) => ({
    label: attr(item, 'n'),
    content: normalizeInline(inlineNodes(item)),
  }));
  return {
    type: 'list',
    caption: caption && hasContent(caption) ? caption : null,
    labeled: items.length > 0 && items.every((i) => i.label),
    items: items.filter((i) => i.label || hasContent(i.content)),
  };
}

/** The blocks of a note: paragraphs and lists; loose inline content becomes a paragraph. */
function parseBlocks(note) {
  const blocks = [];
  let loose = [];
  const flush = () => {
    const content = normalizeInline(loose);
    if (hasContent(content)) blocks.push({ type: 'paragraph', content });
    loose = [];
  };
  for (const node of Array.from(note.childNodes)) {
    if (node.nodeType === ELEMENT && (node.localName === 'p' || node.localName === 'ab')) {
      flush();
      // A list inside a paragraph is split out as its own block
      let part = [];
      for (const child of Array.from(node.childNodes)) {
        if (child.nodeType === ELEMENT && child.localName === 'list') {
          loose = part;
          flush();
          part = [];
          const list = parseList(child);
          if (list.items.length || list.caption) blocks.push(list);
        } else if (child.nodeType === ELEMENT) {
          part.push(...inlineElement(child));
        } else if (child.nodeType === TEXT || child.nodeType === CDATA) {
          part.push({ type: 'text', text: child.nodeValue.replace(/\s+/g, ' ') });
        }
      }
      loose = part;
      flush();
    } else if (node.nodeType === ELEMENT && node.localName === 'list') {
      flush();
      const list = parseList(node);
      if (list.items.length || list.caption) blocks.push(list);
    } else if (node.nodeType === ELEMENT && node.localName === 'head') {
      flush();
      const content = normalizeInline(inlineNodes(node));
      if (hasContent(content))
        blocks.push({ type: 'paragraph', content: [{ type: 'bold', children: content }] });
    } else if (node.nodeType === ELEMENT) {
      loose.push(...inlineElement(node));
    } else if (node.nodeType === TEXT || node.nodeType === CDATA) {
      loose.push({ type: 'text', text: node.nodeValue.replace(/\s+/g, ' ') });
    }
  }
  flush();
  return blocks;
}

/**
 * Parse a parsed TEI document.
 * @param {Document} doc
 * @returns {{sections: Array}|null} null when there is no notesStmt with content
 */
export function parseDescriptionDocument(doc) {
  let notesStmt = doc.getElementsByTagNameNS(TEI_NS, 'notesStmt')[0];
  if (!notesStmt) notesStmt = doc.getElementsByTagName('notesStmt')[0];
  if (!notesStmt) return null;

  const sections = [];
  for (const note of childElements(notesStmt, 'note')) {
    const blocks = parseBlocks(note);
    if (!blocks.length) continue;
    const rend = attr(note, 'rend');
    sections.push({
      key: rend ? rend.toUpperCase() : null,
      heading: rend ? humanizeRend(rend) : null,
      blocks,
    });
  }
  return sections.length ? { sections } : null;
}

/**
 * Parse TEI description XML.
 * @param {string} xml
 * @param {typeof DOMParser} [Parser=globalThis.DOMParser]
 * @returns {{sections: Array}|null} null when there is no notesStmt with content
 * @throws {TeiParseError} when the XML is not well formed
 */
export function parseDescription(xml, Parser = globalThis.DOMParser) {
  if (typeof xml !== 'string' || xml.trim() === '') return null;
  const doc = new Parser().parseFromString(xml, 'application/xml');
  const root = doc.documentElement;
  if (!root || root.localName === 'parsererror' || doc.getElementsByTagName('parsererror').length) {
    throw new TeiParseError('The book description is not well-formed XML');
  }
  return parseDescriptionDocument(doc);
}

/**
 * Plain text of inline content, e.g. for accessible labels.
 * @param {Array} inline
 * @returns {string}
 */
export function inlineText(inline) {
  return (inline || [])
    .map((n) => {
      if (n.type === 'text' || n.type === 'locus') return n.text;
      if (n.type === 'break') return ' ';
      return inlineText(n.children);
    })
    .join('');
}
