import { describe, it, expect } from 'vitest';
import { parseFolioRef, buildFolioIndex, resolveLocus, canvasLabel } from './folio';

const canvas = (i, label) => ({
  id: `https://x/rose/Douce332/canvas/${i}`,
  label: { en: [label] },
});

// Labels as generated for rose/Douce332
const DOUCE332 = [
  canvas(0, 'binding front cover'),
  canvas(1, 'front matter pastedown'),
  canvas(2, 'front matter 1r'),
  canvas(3, 'front matter 1v'),
  canvas(4, '1r'),
  canvas(5, '1v'),
  canvas(6, '2r'),
  canvas(7, '2v'),
  canvas(8, '9r'),
  canvas(9, '9v'),
  canvas(10, '1r insert'),
  canvas(11, '38r'),
  canvas(12, '200r'),
];

describe('parseFolioRef', () => {
  it.each([
    ['9', '9r'],
    ['9r', '9r'],
    ['9V', '9v'],
    ['009v', '9v'],
    ['fol. 2r', '2r'],
    ['ff. 12', '12r'],
    ['200rb', '200r'],
    ['12va', '12v'],
  ])('%s -> %s', (ref, key) => {
    expect(parseFolioRef(ref)?.key).toBe(key);
  });

  it.each(['21 bis', 'ii', '', '0', 'front matter 1r', null, undefined])(
    'does not parse %s',
    (ref) => {
      expect(parseFolioRef(ref)).toBeNull();
    }
  );
});

describe('canvasLabel', () => {
  it('reads language maps and strings', () => {
    expect(canvasLabel({ label: { en: ['1r'] } })).toBe('1r');
    expect(canvasLabel({ label: { none: ['2v'] } })).toBe('2v');
    expect(canvasLabel({ label: { fr: ['3r'] } })).toBe('3r');
    expect(canvasLabel({ label: '4r' })).toBe('4r');
    expect(canvasLabel({})).toBe('');
  });
});

describe('buildFolioIndex', () => {
  it('indexes only canvases labelled with a folio', () => {
    const index = buildFolioIndex(DOUCE332);
    expect([...index.keys()]).toEqual(['1r', '1v', '2r', '2v', '9r', '9v', '38r', '200r']);
    // front matter 1r and 1r insert do not take the place of 1r
    expect(index.get('1r')).toBe('https://x/rose/Douce332/canvas/4');
  });

  it('reads bare-number labels as rectos', () => {
    const index = buildFolioIndex([canvas(0, '1'), canvas(1, '2'), canvas(2, '003')]);
    expect(index.get('1r')).toBe('https://x/rose/Douce332/canvas/0');
    expect(index.get('3r')).toBe('https://x/rose/Douce332/canvas/2');
  });

  it('handles missing input', () => {
    expect(buildFolioIndex(undefined).size).toBe(0);
    expect(buildFolioIndex([{ label: { en: ['1r'] } }]).size).toBe(0);
  });
});

describe('resolveLocus', () => {
  const index = buildFolioIndex(DOUCE332);
  const at = (i) => `https://x/rose/Douce332/canvas/${i}`;

  it('resolves a folio with or without side', () => {
    expect(resolveLocus({ text: '9r' }, index)).toEqual({ canvasId: at(8), folio: '9r' });
    expect(resolveLocus({ text: '9v' }, index)).toEqual({ canvasId: at(9), folio: '9v' });
    expect(resolveLocus({ text: '9' }, index)).toEqual({ canvasId: at(8), folio: '9r' });
    expect(resolveLocus({ text: '200rb' }, index)?.canvasId).toBe(at(12));
  });

  it('resolves ranges and alternatives to their first folio', () => {
    expect(resolveLocus({ from: '1', to: '8', text: '1-8' }, index)?.canvasId).toBe(at(4));
    expect(resolveLocus({ from: '38', to: '39', text: '38/39' }, index)?.canvasId).toBe(at(11));
    expect(resolveLocus({ text: '38/39' }, index)?.canvasId).toBe(at(11));
    expect(resolveLocus({ from: '1r', to: '195r', text: '1r-195r' }, index)?.canvasId).toBe(at(4));
  });

  it('prefers from over the text, and falls back to the text', () => {
    expect(resolveLocus({ from: '2', text: '9r' }, index)?.canvasId).toBe(at(6));
    expect(resolveLocus({ from: '500', text: '9r' }, index)?.canvasId).toBe(at(8));
  });

  it('returns null when nothing matches', () => {
    expect(resolveLocus({ text: '21 bis' }, index)).toBeNull();
    expect(resolveLocus({ text: '17r' }, index)).toBeNull();
    expect(resolveLocus({ text: '9r' }, new Map())).toBeNull();
    expect(resolveLocus(null, index)).toBeNull();
  });
});
