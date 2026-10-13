import { describe, it, expect } from 'vitest';
import { sanitizeIiifHtml, sanitizeIiifHtmlToFragment } from './sanitize';
import { parseAnnotation } from './parse';
import { classify, groupAnnotations } from './group';

describe('sanitizeIiifHtml', () => {
  it('keeps the IIIF tags including small, sub and sup', () => {
    const html =
      '<p><b>Mark</b> <i>x</i> <small>pen</small><br><sub>5</sub><sup>10</sup><span>s</span></p>';
    expect(sanitizeIiifHtml(html)).toBe(html);
  });

  it('removes other tags but keeps their text', () => {
    expect(sanitizeIiifHtml('<div><h2>Title</h2><ul><li>one</li></ul></div>')).toBe('Titleone');
  });

  it('removes scripts, handlers, classes and styles', () => {
    // happy-dom runs scripts in parsed documents (browsers do not), so the
    // script here must be harmless
    const out = sanitizeIiifHtml(
      '<p class="x" style="color:red" onclick="alert(1)">a<script>void 0</script></p>'
    );
    expect(out).toBe('<p>a</p>');
    expect(sanitizeIiifHtml('<img src="/x.png" onerror="alert(1)">')).toBe('<img src="/x.png">');
  });

  it('allows only href on a and src/alt on img, with safe URLs', () => {
    expect(sanitizeIiifHtml('<a href="https://x.org" target="_blank" title="t">l</a>')).toBe(
      '<a href="https://x.org">l</a>'
    );
    expect(sanitizeIiifHtml('<a href="javascript:alert(1)">l</a>')).toBe('<a>l</a>');
    expect(sanitizeIiifHtml('<img src="https://x.org/i.jpg" alt="a" width="3">')).toBe(
      '<img src="https://x.org/i.jpg" alt="a">'
    );
    expect(sanitizeIiifHtml('<span href="https://x.org" alt="a">s</span>')).toBe('<span>s</span>');
  });

  it('handles empty and non-string input', () => {
    expect(sanitizeIiifHtml('')).toBe('');
    expect(sanitizeIiifHtml(null)).toBe('');
    expect(sanitizeIiifHtmlToFragment(undefined).childNodes.length).toBe(0);
  });

  it('can return a fragment', () => {
    const frag = sanitizeIiifHtmlToFragment('<p>a</p><p>b</p>');
    expect(frag.childNodes.length).toBe(2);
    expect(frag.firstChild.nodeName).toBe('P');
  });
});

describe('parseAnnotation', () => {
  it('reads a recipe 0045 SpecificResource body', () => {
    const raw = {
      id: 'a1',
      stylesheet: 'https://x.org/styles/aor.css',
      body: {
        type: 'SpecificResource',
        styleClass: 'rosa-anno aor-mark aor-place-intext',
        source: { type: 'TextualBody', format: 'text/html', language: 'la', value: '<p>x</p>' },
      },
    };
    expect(parseAnnotation(raw)).toEqual({
      id: 'a1',
      classes: ['rosa-anno', 'aor-mark', 'aor-place-intext'],
      html: '<p>x</p>',
      language: 'la',
      stylesheet: 'https://x.org/styles/aor.css',
    });
  });

  it('reads plain TextualBody bodies and escapes plain text', () => {
    const raw = {
      id: 'a2',
      body: [
        { type: 'TextualBody', format: 'text/plain', value: 'a < b' },
        { type: 'TextualBody', purpose: 'tagging', value: 'tag' },
      ],
    };
    expect(parseAnnotation(raw)).toMatchObject({ id: 'a2', classes: [], html: '<p>a &lt; b</p>' });
  });

  it('returns null without textual content', () => {
    expect(parseAnnotation({ id: 'x', body: { type: 'Image', id: 'i.jpg' } })).toBeNull();
    expect(parseAnnotation(null)).toBeNull();
  });
});

describe('groupAnnotations', () => {
  const item = (id, ...classes) => ({ id, classes: ['rosa-anno', ...classes] });

  it('classifies classes', () => {
    expect(classify(['aor-page'])).toEqual({ kind: 'header', key: 'page' });
    expect(classify(['aor-mark', 'aor-place-tail'])).toEqual({ kind: 'place', key: 'tail' });
    expect(classify(['dlmm-transcription', 'dlmm-column-a'])).toEqual({
      kind: 'dlmm',
      key: 'transcription',
    });
    expect(classify(['aor-place-nowhere'])).toEqual({ kind: 'other', key: 'other' });
    expect(classify()).toEqual({ kind: 'other', key: 'other' });
  });

  it('orders AoR places around the page and keeps the header apart', () => {
    const { header, groups } = groupAnnotations([
      item('p', 'aor-page'),
      item('t', 'aor-mark', 'aor-place-tail'),
      item('r', 'aor-mark', 'aor-place-right_margin'),
      item('i1', 'aor-underline', 'aor-place-intext'),
      item('h', 'aor-marginalia', 'aor-place-head'),
      item('i2', 'aor-mark', 'aor-place-intext'),
      item('l', 'aor-symbol', 'aor-place-left_margin'),
      item('x', 'aor-link'),
    ]);
    expect(header.map((i) => i.id)).toEqual(['p']);
    expect(groups.map((g) => g.key)).toEqual([
      'head',
      'left_margin',
      'intext',
      'right_margin',
      'tail',
      'other',
    ]);
    expect(groups[2].items.map((i) => i.id)).toEqual(['i1', 'i2']);
    expect(groups[2].label).toBe('In text');
  });

  it('groups DLMM annotations by kind', () => {
    const { groups } = groupAnnotations([
      item('c', 'dlmm-commentary'),
      item('a', 'dlmm-transcription', 'dlmm-column-a'),
      item('il', 'dlmm-illustration'),
      item('b', 'dlmm-transcription', 'dlmm-column-b'),
    ]);
    expect(groups.map((g) => [g.key, g.items.map((i) => i.id)])).toEqual([
      ['illustration', ['il']],
      ['transcription', ['a', 'b']],
      ['commentary', ['c']],
    ]);
  });
});
