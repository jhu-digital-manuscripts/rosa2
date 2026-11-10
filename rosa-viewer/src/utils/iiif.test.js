import { describe, it, expect } from 'vitest';
import { iiifLabel } from './iiif';

describe('iiifLabel', () => {
  it('returns a plain string as it is', () => {
    expect(iiifLabel('Douce 195')).toBe('Douce 195');
  });

  it('prefers English, then the language-neutral value, then the first language', () => {
    expect(iiifLabel({ fr: ['Rose'], en: ['Rose (en)'] })).toBe('Rose (en)');
    expect(iiifLabel({ fr: ['Rose'], none: ['1r'] })).toBe('1r');
    expect(iiifLabel({ fr: ['Rose', 'Autre'] })).toBe('Rose');
  });

  it('accepts a value that is not in an array', () => {
    expect(iiifLabel({ en: 'Douce 195' })).toBe('Douce 195');
  });

  it('gives an empty string when there is no label', () => {
    expect(iiifLabel(undefined)).toBe('');
    expect(iiifLabel(null)).toBe('');
    expect(iiifLabel({})).toBe('');
    expect(iiifLabel({ en: [] })).toBe('');
  });
});
