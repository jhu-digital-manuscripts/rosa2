/**
 * The public/styles files are part of the IIIF output contract: rosa-tool
 * references them from each annotation's stylesheet property and the sites load
 * them. Check they exist and style the classes rosa-tool emits.
 */

import { describe, it, expect } from 'vitest';
import { readFileSync } from 'node:fs';
import { resolve } from 'node:path';

const stylesDir = resolve(__dirname, '../../../public/styles');
const read = (name) => readFileSync(resolve(stylesDir, name), 'utf8');

describe('collection stylesheets', () => {
  it('aor.css styles the AoR classes', () => {
    const css = read('aor.css');
    for (const cls of [
      '.aor-page',
      '.aor-marginalia',
      '.aor-underline',
      '.aor-mark',
      '.rosa-place-icon',
    ]) {
      expect(css).toContain(cls);
    }
    expect(css).not.toMatch(/url\(/);
  });

  it('dlmm.css styles the DLMM classes', () => {
    const css = read('dlmm.css');
    for (const cls of [
      '.dlmm-transcription',
      '.dlmm-illustration',
      '.dlmm-commentary',
      'sub',
      'sup',
    ]) {
      expect(css).toContain(cls);
    }
    expect(css).not.toMatch(/url\(/);
  });
});
