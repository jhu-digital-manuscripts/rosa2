import React from 'react';
import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { BookDescription } from './BookDescription';

const t = (text) => ({ type: 'text', text });

const description = {
  sections: [
    {
      key: 'IDENTIFICATION',
      heading: 'Identification',
      blocks: [{ type: 'paragraph', content: [t('MS Douce 332, Bodleian Library.')] }],
    },
    {
      key: 'QUIRES',
      heading: 'Quires',
      blocks: [
        {
          type: 'list',
          caption: [t('Collation:')],
          labeled: true,
          items: [
            { label: 'I', content: [{ type: 'locus', text: '1-8', from: '1', to: '8' }] },
            { label: 'II', content: [{ type: 'locus', text: '21 bis', from: null, to: null }] },
          ],
        },
        {
          type: 'paragraph',
          content: [
            t('Signatures, e.g. fol. '),
            { type: 'locus', text: '9r', from: null, to: null },
            t('.'),
          ],
        },
      ],
    },
    {
      key: 'HISTORY',
      heading: 'History',
      blocks: [
        {
          type: 'paragraph',
          content: [
            {
              type: 'italic',
              children: [t('Mesmes Francoys'), { type: 'break' }, t('Roy des francoys')],
            },
          ],
        },
        {
          type: 'list',
          caption: null,
          labeled: false,
          items: [{ label: null, content: [t('Bookplate')] }],
        },
      ],
    },
    {
      key: null,
      heading: null,
      blocks: [{ type: 'paragraph', content: [t('Description by Timothy L. Stinson')] }],
    },
  ],
};

const targets = { 1: { canvasId: 'c-1r', folio: '1r' }, '9r': { canvasId: 'c-9r', folio: '9r' } };
const resolveLocus = (locus) => targets[locus.from] || targets[locus.text] || null;

function renderDescription(props = {}) {
  const onNavigate = vi.fn();
  const utils = render(
    <BookDescription
      description={description}
      resolveLocus={resolveLocus}
      onNavigate={onNavigate}
      {...props}
    />
  );
  return { ...utils, onNavigate };
}

describe('BookDescription', () => {
  it('renders the headed sections as open, collapsible groups', () => {
    const { container } = renderDescription();
    const toggles = screen.getAllByRole('button', { expanded: true });
    expect(toggles.map((b) => b.textContent)).toEqual(['Identification', 'Quires', 'History']);
    expect(container.querySelector('.rosa-annos.rosa-desc')).toBeInTheDocument();
    expect(container.querySelector('.rosa-desc-quires')).toBeInTheDocument();
    expect(screen.getByText('MS Douce 332, Bodleian Library.')).toBeVisible();
  });

  it('collapses and expands a section', () => {
    renderDescription();
    const toggle = screen.getByRole('button', { name: 'Identification' });
    const body = document.getElementById(toggle.getAttribute('aria-controls'));
    fireEvent.click(toggle);
    expect(toggle).toHaveAttribute('aria-expanded', 'false');
    expect(body).toHaveAttribute('hidden');
    fireEvent.click(toggle);
    expect(toggle).toHaveAttribute('aria-expanded', 'true');
    expect(body).not.toHaveAttribute('hidden');
  });

  it('renders the collation as a definition list', () => {
    const { container } = renderDescription();
    const dl = container.querySelector('dl.rosa-desc-collation');
    expect([...dl.querySelectorAll('dt')].map((n) => n.textContent)).toEqual(['I', 'II']);
    expect(screen.getByText('Collation:')).toHaveClass('rosa-desc-caption');
  });

  it('makes resolved folio references buttons that navigate', () => {
    const { onNavigate } = renderDescription();
    fireEvent.click(screen.getByRole('button', { name: '9r, go to folio 9r' }));
    expect(onNavigate).toHaveBeenCalledWith('c-9r');
    fireEvent.click(screen.getByRole('button', { name: '1-8, go to folio 1r' }));
    expect(onNavigate).toHaveBeenLastCalledWith('c-1r');
  });

  it('shows unresolved folio references as text', () => {
    renderDescription();
    const locus = screen.getByText('21 bis');
    expect(locus.tagName).toBe('SPAN');
    expect(locus).toHaveClass('rosa-desc-locus');
  });

  it('shows folio references as text without navigation', () => {
    renderDescription({ onNavigate: undefined });
    expect(screen.queryByRole('button', { name: /go to folio/ })).toBeNull();
    expect(screen.getByText('9r').tagName).toBe('SPAN');
  });

  it('renders italics, line breaks, lists and credit lines', () => {
    const { container } = renderDescription();
    const italic = container.querySelector('.rosa-desc-history i');
    expect(italic.querySelector('br')).toBeInTheDocument();
    expect(italic.textContent).toBe('Mesmes FrancoysRoy des francoys');
    expect(container.querySelector('ul.rosa-desc-list li').textContent).toBe('Bookplate');
    expect(container.querySelector('.rosa-desc-credit').textContent).toBe(
      'Description by Timothy L. Stinson'
    );
  });

  it('renders nothing without sections', () => {
    const { container } = render(<BookDescription description={null} />);
    expect(container).toBeEmptyDOMElement();
  });
});
