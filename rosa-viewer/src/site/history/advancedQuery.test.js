import { describe, it, expect } from 'vitest';
import { formatAdvancedQuery, parseAdvancedQuery } from './advancedQuery';

describe('parseAdvancedQuery', () => {
  it('parses a single term from an old bookmark', () => {
    expect(parseAdvancedQuery("description:'sword'")).toEqual([
      { field: 'description', value: 'sword', operator: 'AND' },
    ]);
  });

  it('parses a flat group with both operators', () => {
    expect(parseAdvancedQuery("(description:'sword' & title:'rose' | char_name:'Amant')")).toEqual([
      { field: 'description', value: 'sword', operator: 'AND' },
      { field: 'title', value: 'rose', operator: 'AND' },
      { field: 'char_name', value: 'Amant', operator: 'OR' },
    ]);
  });

  it('flattens the nested groups the old viewer wrote when the operator changed', () => {
    // The operator in front of a group applies to the group's first term
    expect(parseAdvancedQuery("(a:'1' & (b:'2' | (c:'3' & d:'4')))")).toEqual([
      { field: 'a', value: '1', operator: 'AND' },
      { field: 'b', value: '2', operator: 'AND' },
      { field: 'c', value: '3', operator: 'OR' },
      { field: 'd', value: '4', operator: 'AND' },
    ]);
  });

  it('unescapes quotes and backslashes', () => {
    expect(parseAdvancedQuery("marginalia:'l\\'homme \\\\ x'")).toEqual([
      { field: 'marginalia', value: "l'homme \\ x", operator: 'AND' },
    ]);
  });

  it('maps renamed fields to their current names', () => {
    expect(
      parseAdvancedQuery("(repo:'Bodleian' & place:'Paris' & book:'Livy')").map((r) => r.field)
    ).toEqual(['repository', 'locations', 'books']);
  });

  it('parses extra text after a value', () => {
    expect(parseAdvancedQuery("(mark:'dash'+'regnum' & mark:'' + 'l\\'homme')")).toEqual([
      { field: 'mark', value: 'dash', text: 'regnum', operator: 'AND' },
      { field: 'mark', value: '', text: "l'homme", operator: 'AND' },
    ]);
  });

  it('returns null for text it cannot parse', () => {
    expect(parseAdvancedQuery('green')).toBeNull();
    expect(parseAdvancedQuery("description:'sword")).toBeNull();
    expect(parseAdvancedQuery("(description:'sword'")).toBeNull();
    expect(parseAdvancedQuery("description:'a' extra")).toBeNull();
    expect(parseAdvancedQuery("mark:'dash'+")).toBeNull();
    expect(parseAdvancedQuery('')).toBeNull();
  });
});

describe('formatAdvancedQuery', () => {
  it('formats one row without parentheses', () => {
    expect(formatAdvancedQuery([{ field: 'description', value: 'sword', operator: 'AND' }])).toBe(
      "description:'sword'"
    );
  });

  it('formats several rows as a flat group and skips incomplete rows', () => {
    expect(
      formatAdvancedQuery([
        { field: 'description', value: ' sword ', operator: 'AND' },
        { field: '', value: 'ignored', operator: 'AND' },
        { field: 'title', value: '', operator: 'AND' },
        { field: 'title', value: "l'amant", operator: 'OR' },
      ])
    ).toBe("(description:'sword' | title:'l\\'amant')");
  });

  it('returns an empty string when no row has a value', () => {
    expect(formatAdvancedQuery([{ field: '', value: '', operator: 'AND' }])).toBe('');
  });

  it('formats extra text and keeps rows that have only extra text', () => {
    expect(
      formatAdvancedQuery([
        { field: 'mark', value: 'dash', text: ' regnum ', operator: 'AND' },
        { field: 'symbol', value: '', text: 'sol', operator: 'OR' },
        { field: 'mark', value: 'dot', text: ' ', operator: 'AND' },
      ])
    ).toBe("(mark:'dash'+'regnum' | symbol:''+'sol' & mark:'dot')");
  });

  it('round-trips through parseAdvancedQuery', () => {
    const rows = [
      { field: 'marginalia', value: 'rome', operator: 'AND' },
      { field: 'people', value: 'Livy', operator: 'OR' },
      { field: 'mark', value: 'a\\b', operator: 'AND' },
      { field: 'mark', value: 'dash', text: "it's", operator: 'AND' },
    ];
    expect(parseAdvancedQuery(formatAdvancedQuery(rows))).toEqual(rows);
  });
});
