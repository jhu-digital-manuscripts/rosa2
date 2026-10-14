/**
 * A book description: the notesStmt sections of a TEI description (see
 * utils/tei.js) as collapsible sections styled like the RosaAnnotations panel.
 * Folio references that match a canvas are buttons that show that canvas.
 */

import React, { useCallback, useId, useState } from 'react';

const NO_SECTIONS = [];

const slug = (key) =>
  String(key)
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, '-')
    .replace(/^-|-$/g, '');

function Locus({ node, resolveLocus, onNavigate }) {
  const target = resolveLocus ? resolveLocus(node) : null;
  if (!target || !onNavigate) {
    return <span className="rosa-desc-locus">{node.text}</span>;
  }
  return (
    <button
      type="button"
      className="rosa-desc-locus"
      title={`Go to folio ${target.folio}`}
      aria-label={`${node.text}, go to folio ${target.folio}`}
      onClick={() => onNavigate(target.canvasId)}
    >
      {node.text}
    </button>
  );
}

/**
 * Render inline description content as React elements.
 * @param {Array} nodes
 * @param {Object} ctx - { resolveLocus, onNavigate }
 */
export function renderInline(nodes, ctx = {}) {
  return (nodes || []).map((node, i) => {
    switch (node.type) {
      case 'text':
        return <React.Fragment key={i}>{node.text}</React.Fragment>;
      case 'break':
        return <br key={i} />;
      case 'italic':
        return <i key={i}>{renderInline(node.children, ctx)}</i>;
      case 'bold':
        return <b key={i}>{renderInline(node.children, ctx)}</b>;
      case 'sup':
        return <sup key={i}>{renderInline(node.children, ctx)}</sup>;
      case 'locus':
        return <Locus key={i} node={node} {...ctx} />;
      default:
        return null;
    }
  });
}

function Block({ block, ctx }) {
  if (block.type === 'paragraph') {
    return <p>{renderInline(block.content, ctx)}</p>;
  }
  if (block.type === 'list') {
    const caption = block.caption ? (
      <p className="rosa-desc-caption">{renderInline(block.caption, ctx)}</p>
    ) : null;
    if (block.labeled) {
      return (
        <>
          {caption}
          <dl className="rosa-desc-collation">
            {block.items.map((item, i) => (
              <div key={i}>
                <dt>{item.label}</dt>
                <dd>{renderInline(item.content, ctx)}</dd>
              </div>
            ))}
          </dl>
        </>
      );
    }
    return (
      <>
        {caption}
        <ul className="rosa-desc-list">
          {block.items.map((item, i) => (
            <li key={i}>
              {item.label ? <b>{`${item.label} `}</b> : null}
              {renderInline(item.content, ctx)}
            </li>
          ))}
        </ul>
      </>
    );
  }
  return null;
}

const Blocks = ({ blocks, ctx }) =>
  blocks.map((block, i) => <Block key={i} block={block} ctx={ctx} />);

function DescriptionSection({ section, open, onToggle, ctx }) {
  const contentId = useId();
  return (
    <section className={`rosa-anno-group rosa-desc-section rosa-desc-${slug(section.key)}`}>
      <h4 className="rosa-anno-group-heading">
        <button
          type="button"
          className="rosa-anno-group-toggle"
          aria-expanded={open}
          aria-controls={contentId}
          onClick={onToggle}
        >
          <span className="rosa-anno-group-label">{section.heading}</span>
          <svg
            className="rosa-anno-group-chevron"
            viewBox="0 0 24 24"
            width="20"
            height="20"
            aria-hidden="true"
            focusable="false"
          >
            <path d="M16.59 8.59 12 13.17 7.41 8.59 6 10l6 6 6-6z" />
          </svg>
        </button>
      </h4>
      <div id={contentId} className="rosa-desc-body" hidden={!open}>
        <Blocks blocks={section.blocks} ctx={ctx} />
      </div>
    </section>
  );
}

/**
 * @param {Object} props
 * @param {{sections: Array}} props.description - From parseDescription
 * @param {Function} [props.resolveLocus] - (locus) => {canvasId, folio} | null
 * @param {Function} [props.onNavigate] - (canvasId) => void
 */
export function BookDescription({ description, resolveLocus, onNavigate }) {
  const sections = description?.sections || NO_SECTIONS;
  const [closed, setClosed] = useState({});
  const toggle = useCallback((i) => setClosed((prev) => ({ ...prev, [i]: !prev[i] })), []);
  const ctx = { resolveLocus, onNavigate };

  if (sections.length === 0) return null;

  return (
    <div className="rosa-annos rosa-desc">
      {sections.map((section, i) =>
        section.heading ? (
          <DescriptionSection
            key={i}
            section={section}
            open={!closed[i]}
            onToggle={() => toggle(i)}
            ctx={ctx}
          />
        ) : (
          <div key={i} className="rosa-desc-credit">
            <Blocks blocks={section.blocks} ctx={ctx} />
          </div>
        )
      )}
    </div>
  );
}

export default BookDescription;
