import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';

import { HighlightedText, formatFieldLabel, splitHighlight } from './HighlightedText';

describe('formatFieldLabel', () => {
  it('capitalizes the field name', () => {
    expect(formatFieldLabel('marginalia')).toBe('Marginalia');
  });

  it('drops sub-field suffixes', () => {
    expect(formatFieldLabel('transcription.la')).toBe('Transcription');
    expect(formatFieldLabel('mark.keyword')).toBe('Mark');
  });

  it('replaces underscores with spaces', () => {
    expect(formatFieldLabel('cross_reference.en')).toBe('Cross reference');
  });
});

describe('HighlightedText', () => {
  it('renders a bold field label separate from the highlighted match', () => {
    const { container } = render(
      <HighlightedText field="anchor_text.en" html="some <mark>match</mark> here" />
    );

    const label = screen.getByText('Anchor text:');
    expect(label).toHaveStyle({ fontWeight: 700 });
    expect(container.querySelector('mark')).toHaveTextContent('match');
    expect(label).not.toContainElement(container.querySelector('mark'));
  });

  it('shows markup in the indexed text as text', () => {
    const { container } = render(
      <HighlightedText
        field="transcription.la"
        html={'a <b and <img src=x onerror="alert(1)"> &amp; <mark>match</mark>'}
      />
    );

    expect(container.querySelector('img')).toBeNull();
    expect(container.querySelectorAll('mark')).toHaveLength(1);
    expect(container).toHaveTextContent('a <b and <img src=x onerror="alert(1)"> &amp; match');
  });
});

describe('splitHighlight', () => {
  it('alternates plain and matched text', () => {
    expect(splitHighlight('<mark>a</mark> b <mark>c</mark>')).toEqual([
      { text: 'a', marked: true },
      { text: ' b ', marked: false },
      { text: 'c', marked: true },
    ]);
  });

  it('returns nothing for an empty fragment', () => {
    expect(splitHighlight('')).toEqual([]);
    expect(splitHighlight(undefined)).toEqual([]);
  });
});
