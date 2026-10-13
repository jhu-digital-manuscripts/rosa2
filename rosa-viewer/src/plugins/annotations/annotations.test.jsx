import React from 'react';
import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent, within } from '@testing-library/react';

vi.mock('mirador', () => ({
  ScrollTo: ({ children }) => children,
  getAnnotationResourcesByMotivationForCanvas: vi.fn(() => ['resources']),
}));

import { RosaCanvasAnnotations } from './components/RosaCanvasAnnotations';
import { PlaceIcon } from './components/PlaceIcon';
import { htmlToReact } from './components/AnnotationBody';
import { mapStateToProps, rosaAnnotationsPlugin } from './index';
import { addStylesheet } from './utils/stylesheet';

/** A Mirador annotation resource wrapping recipe 0045 annotation JSON. */
function anno(id, styleClass, value, language) {
  return {
    resource: {
      id,
      type: 'Annotation',
      motivation: 'commenting',
      body: {
        type: 'SpecificResource',
        styleClass,
        source: { type: 'TextualBody', format: 'text/html', value, language },
      },
      target: 'canvas',
    },
  };
}

const aor = [
  anno(
    'p',
    'rosa-anno aor-page',
    '<p><b>323</b></p><p><small><b>Reader:</b> Harvey</small></p>',
    'en'
  ),
  anno(
    'h',
    'rosa-anno aor-marginalia aor-place-head',
    '<p><b>Marginal note</b></p><p>Tergemina Anchora.</p>',
    'la'
  ),
  anno('l1', 'rosa-anno aor-mark aor-place-left_margin', '<p><b>Mark</b> Bracket</p>'),
  anno('l2', 'rosa-anno aor-symbol aor-place-left_margin', '<p><b>Symbol</b> Sun</p>'),
  anno(
    'i',
    'rosa-anno aor-underline aor-place-intext',
    '<p><b>Underline</b></p><p>“ego solus”</p>',
    'la'
  ),
];

function renderPanel(props = {}) {
  const actions = {
    selectAnnotation: vi.fn(),
    deselectAnnotation: vi.fn(),
    hoverAnnotation: vi.fn(),
  };
  const view = render(
    <RosaCanvasAnnotations
      annotationResources={aor}
      windowId="w1"
      label="343"
      {...actions}
      {...props}
    />
  );
  return { ...view, ...actions };
}

describe('RosaCanvasAnnotations', () => {
  it('shows the page header and groups by place with icons and counts', () => {
    const { container } = renderPanel();
    expect(screen.getByText('343')).toBeInTheDocument();
    expect(screen.getByText('Reader:')).toBeInTheDocument();

    const toggles = screen.getAllByRole('button', { expanded: true });
    expect(toggles.map((b) => b.querySelector('.rosa-anno-group-label').textContent)).toEqual([
      'Head',
      'Left margin',
      'In text',
    ]);
    expect(within(toggles[1]).getByText('2')).toBeInTheDocument();
    expect(toggles[1]).toHaveAccessibleName(/Left margin\s*2 annotations/);
    expect(container.querySelectorAll('.rosa-anno-group svg[data-place]').length).toBe(3);
    expect(container.querySelector('svg[data-place="head"]')).toHaveAttribute(
      'aria-hidden',
      'true'
    );
  });

  it('puts the styleClass values and language on the entries', () => {
    const { container } = renderPanel();
    const entry = container.querySelector('[data-annotation-id="h"]');
    expect(entry).toHaveClass('rosa-anno-entry', 'rosa-anno', 'aor-marginalia', 'aor-place-head');
    expect(entry.querySelector('.rosa-anno-body')).toHaveAttribute('lang', 'la');
  });

  it('collapses and expands a group', () => {
    renderPanel();
    const toggle = screen.getByRole('button', { name: /Left margin/ });
    const list = document.getElementById(toggle.getAttribute('aria-controls'));
    expect(list).toBeVisible();
    fireEvent.click(toggle);
    expect(toggle).toHaveAttribute('aria-expanded', 'false');
    expect(list).not.toBeVisible();
    fireEvent.click(toggle);
    expect(toggle).toHaveAttribute('aria-expanded', 'true');
  });

  it('selects, deselects and hovers annotations', () => {
    const { container, selectAnnotation, deselectAnnotation, hoverAnnotation, rerender } =
      renderPanel();
    const select = container.querySelector('[data-annotation-id="i"] .rosa-anno-select');
    fireEvent.click(select);
    expect(selectAnnotation).toHaveBeenCalledWith('w1', 'i');
    fireEvent.keyDown(select, { key: 'Enter' });
    expect(selectAnnotation).toHaveBeenCalledTimes(2);
    fireEvent.mouseEnter(select);
    expect(hoverAnnotation).toHaveBeenCalledWith('w1', ['i']);
    fireEvent.mouseLeave(select);
    expect(hoverAnnotation).toHaveBeenLastCalledWith('w1', []);

    rerender(
      <RosaCanvasAnnotations
        annotationResources={aor}
        windowId="w1"
        selectedAnnotationId="i"
        hoveredAnnotationIds={['l1']}
        selectAnnotation={selectAnnotation}
        deselectAnnotation={deselectAnnotation}
        hoverAnnotation={hoverAnnotation}
      />
    );
    const selected = container.querySelector('[data-annotation-id="i"]');
    expect(selected).toHaveClass('rosa-anno-selected');
    expect(selected.querySelector('.rosa-anno-select')).toHaveAttribute('aria-pressed', 'true');
    expect(container.querySelector('[data-annotation-id="l1"]')).toHaveClass('rosa-anno-hovered');
    fireEvent.click(selected.querySelector('.rosa-anno-select'));
    expect(deselectAnnotation).toHaveBeenCalledWith('w1', 'i');
  });

  it('groups DLMM annotations by kind', () => {
    renderPanel({
      annotationResources: [
        anno('c', 'rosa-anno dlmm-commentary', '<p>Commentary</p>'),
        anno('a', 'rosa-anno dlmm-transcription dlmm-column-a', '<p><b>Column a</b></p>'),
        anno('il', 'rosa-anno dlmm-illustration', '<p><b>Illustration</b></p>'),
      ],
    });
    const labels = screen
      .getAllByRole('button', { expanded: true })
      .map((b) => b.querySelector('.rosa-anno-group-label').textContent);
    expect(labels).toEqual(['Illustration', 'Transcription', 'Commentary']);
  });

  it('renders nothing without annotations', () => {
    const { container } = renderPanel({ annotationResources: [] });
    expect(container).toBeEmptyDOMElement();
  });
});

describe('notes', () => {
  it('turns note markers into a focusable asterisk with the note text', async () => {
    const { container } = render(
      <div>
        {htmlToReact('<p><span>Ce sui gie<small>*&nbsp;"p. tex" (Lecoy)</small></span></p>')}
      </div>
    );
    const marker = screen.getByRole('button', { name: 'Note' });
    expect(marker).toHaveTextContent('*');
    // The note is available to screen readers without opening the tooltip
    expect(container.querySelector('.rosa-sr-only')).toHaveTextContent('"p. tex" (Lecoy)');
    expect(container.querySelector('small')).toBeNull();

    fireEvent.focus(marker);
    fireEvent.mouseOver(marker);
    expect(await screen.findByRole('tooltip')).toHaveTextContent('"p. tex" (Lecoy)');
    expect(marker).toHaveAttribute('aria-describedby');
  });

  it('does not select the annotation when the note is used', () => {
    const selectAnnotation = vi.fn();
    render(
      <RosaCanvasAnnotations
        annotationResources={[
          anno(
            't',
            'rosa-anno dlmm-transcription',
            '<p><span>v<small>*&nbsp;note</small></span></p>'
          ),
        ]}
        windowId="w1"
        selectAnnotation={selectAnnotation}
      />
    );
    fireEvent.click(screen.getByRole('button', { name: 'Note' }));
    expect(selectAnnotation).not.toHaveBeenCalled();
  });

  it('keeps ordinary small text', () => {
    render(<div>{htmlToReact('<p><small>pen</small></p>')}</div>);
    expect(screen.queryByRole('button')).toBeNull();
    expect(screen.getByText('pen').tagName).toBe('SMALL');
  });

  it('opens links in a new tab', () => {
    render(<div>{htmlToReact('<p><a href="https://example.org/">Licence</a></p>')}</div>);
    const link = screen.getByRole('link', { name: 'Licence' });
    expect(link).toHaveAttribute('target', '_blank');
    expect(link).toHaveAttribute('rel', 'noopener noreferrer');
  });
});

describe('PlaceIcon', () => {
  it('draws each place and nothing for unknown places', () => {
    for (const place of ['head', 'tail', 'left_margin', 'right_margin', 'intext', 'full_page']) {
      const { container, unmount } = render(<PlaceIcon place={place} />);
      expect(container.querySelector('.rosa-place-icon-region')).not.toBeNull();
      unmount();
    }
    const { container } = render(<PlaceIcon place="nowhere" />);
    expect(container).toBeEmptyDOMElement();
  });
});

describe('plugin', () => {
  it('wraps CanvasAnnotations and maps raw resources and hovered ids', () => {
    expect(rosaAnnotationsPlugin[0]).toMatchObject({ target: 'CanvasAnnotations', mode: 'wrap' });
    const state = { windows: { w1: { hoveredAnnotationIds: ['x'] } } };
    expect(mapStateToProps(state, { canvasId: 'c', windowId: 'w1' })).toEqual({
      annotationResources: ['resources'],
      hoveredAnnotationIds: ['x'],
    });
    expect(mapStateToProps({}, { canvasId: 'c', windowId: 'w2' }).hoveredAnnotationIds).toEqual([]);
  });
});

describe('addStylesheet', () => {
  it('adds a link once per URL', () => {
    const first = addStylesheet('/styles/aor.css');
    const again = addStylesheet('/styles/aor.css');
    expect(again).toBe(first);
    expect(document.head.querySelectorAll('link[href="/styles/aor.css"]').length).toBe(1);
    expect(first.rel).toBe('stylesheet');
    expect(addStylesheet(null)).toBeNull();
    first.remove();
  });
});
