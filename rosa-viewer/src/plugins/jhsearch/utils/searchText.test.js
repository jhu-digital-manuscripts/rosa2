import { describe, it, expect } from 'vitest';
import { parseSearchText } from './searchText';

describe('parseSearchText', () => {
  it('returns plain text as words', () => {
    expect(parseSearchText('ad  fontes ')).toEqual({ words: 'ad fontes', phrases: [] });
  });

  it('returns no units for blank text', () => {
    expect(parseSearchText('')).toEqual({ words: '', phrases: [] });
    expect(parseSearchText('  ')).toEqual({ words: '', phrases: [] });
    expect(parseSearchText(undefined)).toEqual({ words: '', phrases: [] });
  });

  it('takes quoted text as a phrase', () => {
    expect(parseSearchText('"Julius Caesar"')).toEqual({
      words: '',
      phrases: ['Julius Caesar'],
    });
  });

  it('separates phrases from bare words', () => {
    expect(parseSearchText('rex "ad fontes" lex')).toEqual({
      words: 'rex lex',
      phrases: ['ad fontes'],
    });
  });

  it('keeps several phrases in order', () => {
    expect(parseSearchText('"a b" c "d e"')).toEqual({ words: 'c', phrases: ['a b', 'd e'] });
  });

  it('runs an unterminated quote to the end of the text', () => {
    expect(parseSearchText('rex "ad fontes')).toEqual({ words: 'rex', phrases: ['ad fontes'] });
  });

  it('accepts curly quotes', () => {
    expect(parseSearchText('“vide infra” rex')).toEqual({
      words: 'rex',
      phrases: ['vide infra'],
    });
    expect(parseSearchText('“vide infra"')).toEqual({ words: '', phrases: ['vide infra'] });
  });

  it('drops blank phrases', () => {
    expect(parseSearchText('""')).toEqual({ words: '', phrases: [] });
    expect(parseSearchText('rex " " lex')).toEqual({ words: 'rex lex', phrases: [] });
  });

  it('keeps a single quoted word as a phrase', () => {
    expect(parseSearchText('"Cicero"')).toEqual({ words: '', phrases: ['Cicero'] });
  });

  it('separates words a quote touches', () => {
    expect(parseSearchText('rex"ad fontes"lex')).toEqual({
      words: 'rex lex',
      phrases: ['ad fontes'],
    });
  });
});
