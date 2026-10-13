/**
 * Small page diagrams marking where on the page an annotation is: a page outline
 * with a block of text lines, and the relevant region shaded.
 */

import React from 'react';

// Page 20x26 with a 1px border; text block in the middle
const PAGE = { x: 1.5, y: 1.5, w: 17, h: 23 };
const TEXT = { x: 6, y: 7, w: 8, h: 12 };

const REGIONS = {
  head: { x: PAGE.x, y: PAGE.y, w: PAGE.w, h: TEXT.y - PAGE.y - 1 },
  tail: {
    x: PAGE.x,
    y: TEXT.y + TEXT.h + 1,
    w: PAGE.w,
    h: PAGE.y + PAGE.h - (TEXT.y + TEXT.h + 1),
  },
  left_margin: { x: PAGE.x, y: PAGE.y, w: TEXT.x - PAGE.x - 1, h: PAGE.h },
  right_margin: {
    x: TEXT.x + TEXT.w + 1,
    y: PAGE.y,
    w: PAGE.x + PAGE.w - (TEXT.x + TEXT.w + 1),
    h: PAGE.h,
  },
  intext: { x: TEXT.x - 0.5, y: TEXT.y - 0.5, w: TEXT.w + 1, h: TEXT.h + 1 },
  full_page: PAGE,
};

/**
 * @param {Object} props
 * @param {string} props.place - head, tail, left_margin, right_margin, intext or full_page
 * @param {string} [props.className]
 */
export function PlaceIcon({ place, className = 'rosa-place-icon' }) {
  const region = REGIONS[place];
  if (!region) return null;
  const lines = [];
  for (let y = TEXT.y + 1; y < TEXT.y + TEXT.h; y += 2) {
    lines.push(<line key={y} x1={TEXT.x} x2={TEXT.x + TEXT.w} y1={y} y2={y} />);
  }
  return (
    <svg
      className={className}
      data-place={place}
      viewBox="0 0 20 26"
      width="20"
      height="26"
      aria-hidden="true"
      focusable="false"
    >
      <rect
        className="rosa-place-icon-region"
        x={region.x}
        y={region.y}
        width={region.w}
        height={region.h}
        rx="0.5"
      />
      <rect
        className="rosa-place-icon-page"
        x={PAGE.x}
        y={PAGE.y}
        width={PAGE.w}
        height={PAGE.h}
        rx="1"
        fill="none"
      />
      <g className="rosa-place-icon-text">{lines}</g>
    </svg>
  );
}

export default PlaceIcon;
