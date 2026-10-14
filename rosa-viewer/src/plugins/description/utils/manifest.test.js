import { describe, it, expect } from 'vitest';
import { selectTeiDescriptionUrl, hasTeiDescription } from './manifest';

const tei = {
  id: 'https://x/iiif/rose/Douce332/description.xml',
  type: 'Dataset',
  label: { en: ['Book description (TEI)'] },
  format: 'application/tei+xml',
  profile: 'http://www.tei-c.org/ns/1.0',
};

describe('selectTeiDescriptionUrl', () => {
  it('finds the TEI seeAlso entry', () => {
    const other = { id: 'https://x/mods.xml', type: 'Dataset', format: 'application/mods+xml' };
    expect(selectTeiDescriptionUrl({ seeAlso: [other, tei] })).toBe(tei.id);
    expect(hasTeiDescription({ seeAlso: [tei] })).toBe(true);
  });

  it('matches on the profile or the format alone', () => {
    expect(
      selectTeiDescriptionUrl({ seeAlso: [{ id: 'a', profile: 'http://www.tei-c.org/ns/1.0/' }] })
    ).toBe('a');
    expect(selectTeiDescriptionUrl({ seeAlso: [{ id: 'b', format: 'application/tei+xml' }] })).toBe(
      'b'
    );
  });

  it('accepts a single seeAlso object and IIIF 2 @id', () => {
    expect(selectTeiDescriptionUrl({ seeAlso: tei })).toBe(tei.id);
    expect(
      selectTeiDescriptionUrl({ seeAlso: { '@id': 'c', format: 'application/tei+xml' } })
    ).toBe('c');
  });

  it('returns null without a TEI seeAlso', () => {
    expect(selectTeiDescriptionUrl({})).toBeNull();
    expect(selectTeiDescriptionUrl(null)).toBeNull();
    expect(selectTeiDescriptionUrl({ seeAlso: [{ id: 'x', format: 'text/html' }] })).toBeNull();
    expect(selectTeiDescriptionUrl({ seeAlso: [{ format: 'application/tei+xml' }] })).toBeNull();
    expect(hasTeiDescription(undefined)).toBe(false);
  });
});
