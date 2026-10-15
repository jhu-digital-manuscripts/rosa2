import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';

import { HighlightedText, formatFieldLabel } from './HighlightedText';

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
});
