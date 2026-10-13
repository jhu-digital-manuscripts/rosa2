/**
 * Replacement for Mirador's CanvasAnnotations list: the commenting annotations
 * of one canvas, grouped (AoR by place on the page, DLMM by kind) under
 * collapsible headings, styled by the annotation styleClass values.
 */

import React, { useCallback, useId, useMemo, useState } from 'react';
import { ScrollTo } from 'mirador';
import { parseAnnotation } from '../utils/parse';
import { groupAnnotations } from '../utils/group';
import { AnnotationBody } from './AnnotationBody';
import { PlaceIcon } from './PlaceIcon';

const NO_IDS = [];

/**
 * One selectable annotation.
 */
function AnnotationEntry({
  item,
  selected,
  hovered,
  containerRef,
  onToggle,
  onHover,
  onUnhover,
  as: Tag = 'li',
}) {
  const handleKeyDown = (event) => {
    if (event.target !== event.currentTarget) return;
    if (event.key === 'Enter' || event.key === ' ') {
      event.preventDefault();
      onToggle(item);
    }
  };
  const className = [
    'rosa-anno-entry',
    ...item.classes,
    selected ? 'rosa-anno-selected' : '',
    hovered ? 'rosa-anno-hovered' : '',
  ]
    .filter(Boolean)
    .join(' ');

  return (
    <ScrollTo containerRef={containerRef} offsetTop={96} scrollTo={selected}>
      <Tag className={className} data-annotation-id={item.id}>
        <div
          className="rosa-anno-select"
          role="button"
          tabIndex={0}
          aria-pressed={selected}
          onClick={() => onToggle(item)}
          onKeyDown={handleKeyDown}
          onMouseEnter={() => onHover(item)}
          onMouseLeave={onUnhover}
          onFocus={() => onHover(item)}
          onBlur={onUnhover}
        >
          <AnnotationBody html={item.html} language={item.language} />
        </div>
      </Tag>
    </ScrollTo>
  );
}

/**
 * A collapsible group of annotations.
 */
function AnnotationGroup({ group, open, onToggleOpen, renderEntry }) {
  const contentId = useId();
  return (
    <section className={`rosa-anno-group rosa-anno-group-${group.key}`}>
      <h4 className="rosa-anno-group-heading">
        <button
          type="button"
          className="rosa-anno-group-toggle"
          aria-expanded={open}
          aria-controls={contentId}
          onClick={() => onToggleOpen(group)}
        >
          {group.kind === 'place' ? <PlaceIcon place={group.key} /> : null}
          <span className="rosa-anno-group-label">{group.label}</span>
          <span className="rosa-anno-group-count">
            {group.items.length}
            <span className="rosa-sr-only">
              {group.items.length === 1 ? ' annotation' : ' annotations'}
            </span>
          </span>
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
      <ul id={contentId} className="rosa-anno-list" hidden={!open}>
        {group.items.map((item) => renderEntry(item))}
      </ul>
    </section>
  );
}

/**
 * @param {Object} props - Props of Mirador's CanvasAnnotations plus plugin state
 * @param {Array} props.annotationResources - Mirador annotation resources for the canvas
 * @param {string[]} props.hoveredAnnotationIds
 * @param {string} props.windowId
 * @param {string} [props.label] - Canvas label
 * @param {number} [props.index]
 * @param {number} [props.totalSize]
 * @param {string} [props.selectedAnnotationId]
 * @param {Function} props.selectAnnotation - (windowId, id)
 * @param {Function} props.deselectAnnotation - (windowId, id)
 * @param {Function} props.hoverAnnotation - (windowId, ids)
 * @param {Object} [props.containerRef]
 */
export function RosaCanvasAnnotations({
  annotationResources = NO_IDS,
  hoveredAnnotationIds = NO_IDS,
  windowId,
  label,
  index = 0,
  totalSize = 1,
  selectedAnnotationId,
  selectAnnotation,
  deselectAnnotation,
  hoverAnnotation,
  containerRef,
}) {
  const { header, groups } = useMemo(() => {
    const items = annotationResources
      .map((resource) => parseAnnotation(resource?.resource ?? resource))
      .filter(Boolean);
    return groupAnnotations(items);
  }, [annotationResources]);

  const [closed, setClosed] = useState({});
  const toggleOpen = useCallback(
    (group) => setClosed((prev) => ({ ...prev, [group.key]: !prev[group.key] })),
    []
  );

  const onToggle = useCallback(
    (item) => {
      if (selectedAnnotationId === item.id) deselectAnnotation?.(windowId, item.id);
      else selectAnnotation?.(windowId, item.id);
    },
    [selectedAnnotationId, selectAnnotation, deselectAnnotation, windowId]
  );
  const onHover = useCallback(
    (item) => hoverAnnotation?.(windowId, [item.id]),
    [hoverAnnotation, windowId]
  );
  const onUnhover = useCallback(() => hoverAnnotation?.(windowId, []), [hoverAnnotation, windowId]);

  if (header.length === 0 && groups.length === 0) return null;

  const renderEntry = (item, as) => (
    <AnnotationEntry
      key={item.id}
      as={as}
      item={item}
      selected={selectedAnnotationId === item.id}
      hovered={hoveredAnnotationIds.includes(item.id)}
      containerRef={containerRef}
      onToggle={onToggle}
      onHover={onHover}
      onUnhover={onUnhover}
    />
  );

  return (
    <div className="rosa-annos">
      {label ? (
        <p className="rosa-annos-canvas">
          {totalSize > 1 ? `${label} (${index + 1}/${totalSize})` : label}
        </p>
      ) : null}
      {header.map((item) => renderEntry(item, 'div'))}
      {groups.map((group) => (
        <AnnotationGroup
          key={group.key}
          group={group}
          open={!closed[group.key]}
          onToggleOpen={toggleOpen}
          renderEntry={renderEntry}
        />
      ))}
    </div>
  );
}

export default RosaCanvasAnnotations;
