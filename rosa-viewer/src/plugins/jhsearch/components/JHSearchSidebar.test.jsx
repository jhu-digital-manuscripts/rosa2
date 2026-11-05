/**
 * Tests for resizing the sidebar by dragging its handle.
 *
 * happy-dom does no layout, so the view's width is stubbed and the sidebar's
 * left edge is at 0: the pointer's clientX is the width it asks for. Its
 * default window (1024px) is in the wide layout, where the handle is shown.
 */

import React from 'react';
import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { Provider } from 'react-redux';

import { JHSearchSidebar } from './JHSearchSidebar';
import { mockReduxState } from '../../../test/mocks';

vi.mock('./CollectionSelector', () => ({ CollectionSelector: () => null }));
vi.mock('./CategoryFacets', () => ({ CategoryFacets: () => null }));
vi.mock('./SimpleSearch', () => ({ SimpleSearch: () => null }));
vi.mock('./AdvancedSearch', () => ({ AdvancedSearch: () => null }));
vi.mock('./SortOrder', () => ({ SortOrder: () => null }));

const VIEW_WIDTH = 1136;

/** Render the sidebar in a view VIEW_WIDTH wide; returns the sidebar and a drag helper. */
function renderSidebar() {
  const store = { getState: () => mockReduxState, dispatch: vi.fn(), subscribe: () => () => {} };
  const { container } = render(
    <Provider store={store}>
      <JHSearchSidebar />
    </Provider>
  );
  Object.defineProperty(container, 'clientWidth', { configurable: true, value: VIEW_WIDTH });

  const sidebar = screen.getByRole('complementary', { name: 'Search controls' });
  const handle = screen.getByRole('separator', { name: 'Resize sidebar' });

  /** Press the handle, move the pointer through each clientX, release. */
  const drag = (...positions) => {
    fireEvent.mouseDown(handle);
    positions.forEach((clientX) => fireEvent.mouseMove(document, { clientX }));
    fireEvent.mouseUp(document);
  };

  return { sidebar, drag, width: () => getComputedStyle(sidebar).width };
}

describe('JHSearchSidebar resizing', () => {
  it('starts at the default width', () => {
    const { width } = renderSidebar();
    expect(width()).toBe('450px');
  });

  it('follows the pointer while dragged', () => {
    const { drag, width } = renderSidebar();
    drag(500, 600);
    expect(width()).toBe('600px');
  });

  it('leaves the results 300px when dragged past the view', () => {
    const { drag, width } = renderSidebar();
    drag(1100, 1260);
    expect(width()).toBe(`${VIEW_WIDTH - 300}px`);
  });

  it('ignores pointer positions below the minimum width', () => {
    // Firefox reports a stray position like this when the pointer leaves the
    // window; it used to collapse the sidebar to its minimum
    const { drag, width } = renderSidebar();
    drag(800, 5);
    expect(width()).toBe('800px');
  });

  it('keeps 300px for the results if the window shrinks after a drag', () => {
    // happy-dom does not resolve calc(), so check the rule itself
    const { sidebar } = renderSidebar();
    expect(getComputedStyle(sidebar).maxWidth).toBe('calc(100% - 300px)');
  });
});
