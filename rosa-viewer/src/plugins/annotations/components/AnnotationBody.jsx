/**
 * Render sanitized annotation HTML as React elements.
 *
 * The HTML is first sanitized to the IIIF tag set (see utils/sanitize.js) and then
 * converted element by element, so no raw HTML is injected. Editorial notes, which
 * rosa-tool writes as <small>*&nbsp;note</small> where they occur in a transcription,
 * become an asterisk that shows the note on hover, keyboard focus or tap.
 */

import React, { useMemo } from 'react';
import Tooltip from '@mui/material/Tooltip';
import { sanitizeIiifHtmlToFragment } from '../utils/sanitize';

const NOTE_PREFIX = /^\*[\u00a0 ]/;

/**
 * Returns the note text if the element is a note marker, else null.
 * @param {Element} el
 * @returns {string|null}
 */
export function noteText(el) {
  if (el.nodeName.toLowerCase() !== 'small') return null;
  const text = el.textContent || '';
  return NOTE_PREFIX.test(text) ? text.replace(NOTE_PREFIX, '').trim() : null;
}

function stop(event) {
  // A note is inside a selectable annotation; using it should not select it.
  event.stopPropagation();
}

/**
 * Returns the tag of the only element in a paragraph that has no other text,
 * e.g. 'b' for <p><b>Heading</b></p>, or null.
 * @param {Element} p
 * @returns {string|null}
 */
export function soleElement(p) {
  let tag = null;
  for (const child of p.childNodes) {
    if (child.nodeType === 3) {
      if (child.nodeValue.trim()) return null;
    } else if (child.nodeType === 1) {
      if (tag) return null;
      tag = child.nodeName.toLowerCase();
    }
  }
  return tag;
}

// Paragraph roles from the body HTML conventions (see docs/iiif-generation.md)
const PARAGRAPH_CLASS = { b: 'rosa-heading', i: 'rosa-translation', small: 'rosa-meta' };

/**
 * An asterisk that reveals a note.
 * @param {Object} props
 * @param {string} props.text
 */
export function NoteMarker({ text }) {
  return (
    <span className="rosa-note">
      <Tooltip
        title={text}
        describeChild
        arrow
        enterTouchDelay={0}
        leaveTouchDelay={10000}
        slotProps={{ tooltip: { className: 'rosa-note-tooltip' } }}
      >
        <button
          type="button"
          className="rosa-note-marker"
          aria-label="Note"
          onClick={stop}
          onKeyDown={stop}
        >
          *
        </button>
      </Tooltip>
      <span className="rosa-sr-only">{text}</span>
    </span>
  );
}

function toReact(node, key) {
  if (node.nodeType === 3) return node.nodeValue;
  if (node.nodeType !== 1) return null;

  const tag = node.nodeName.toLowerCase();
  const note = noteText(node);
  if (note) return <NoteMarker key={key} text={note} />;

  const children = Array.from(node.childNodes, (child, i) => toReact(child, i));
  switch (tag) {
    case 'br':
      return <br key={key} />;
    case 'img':
      return (
        <img key={key} src={node.getAttribute('src') || ''} alt={node.getAttribute('alt') || ''} />
      );
    case 'a': {
      const href = node.getAttribute('href');
      if (!href) return <React.Fragment key={key}>{children}</React.Fragment>;
      return (
        <a key={key} href={href} target="_blank" rel="noopener noreferrer" onClick={stop}>
          {children}
        </a>
      );
    }
    case 'p': {
      // CSS cannot tell <p><b>Heading</b></p> from <p><b>Label:</b> value</p>
      // (text nodes do not count for :only-child), so mark the paragraph roles.
      const className = PARAGRAPH_CLASS[soleElement(node)];
      return React.createElement(tag, className ? { key, className } : { key }, ...children);
    }
    case 'b':
    case 'i':
    case 'small':
    case 'span':
    case 'sub':
    case 'sup':
      return React.createElement(tag, { key }, ...children);
    default:
      // Unreachable after sanitizing; keep the text
      return <React.Fragment key={key}>{children}</React.Fragment>;
  }
}

/**
 * Convert annotation HTML to React nodes.
 * @param {string} html
 * @returns {React.ReactNode[]}
 */
export function htmlToReact(html) {
  const fragment = sanitizeIiifHtmlToFragment(html);
  return Array.from(fragment.childNodes, (child, i) => toReact(child, i));
}

/**
 * @param {Object} props
 * @param {string} props.html - Annotation body HTML
 * @param {string} [props.language] - Body language
 */
export function AnnotationBody({ html, language }) {
  const content = useMemo(() => htmlToReact(html), [html]);
  return (
    <div className="rosa-anno-body" lang={language || undefined}>
      {content}
    </div>
  );
}

export default AnnotationBody;
