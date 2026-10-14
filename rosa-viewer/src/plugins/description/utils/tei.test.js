// @vitest-environment jsdom
// happy-dom parses application/xml as HTML, so this file uses jsdom.

import { describe, it, expect } from 'vitest';
import { parseDescription, humanizeRend, normalizeInline, inlineText, TeiParseError } from './tei';

const tei = (notes) => `<?xml version="1.0" encoding="UTF-8"?>
<TEI xmlns="http://www.tei-c.org/ns/1.0">
  <teiHeader><fileDesc>
    <titleStmt><title>Roman de la Rose</title></titleStmt>
    <notesStmt>${notes}</notesStmt>
    <sourceDesc><bibl><note type="format">manuscript</note></bibl></sourceDesc>
  </fileDesc></teiHeader>
</TEI>`;

// Abridged from archive/rose/Douce332/Douce332.description_en.xml
const DOUCE332 = tei(`
  <note rend="IDENTIFICATION">
    <p>MS Douce 332, Bodleian Library, Oxford. French, 15th century. </p>
  </note>
  <note rend="BASIC INFORMATION">
    <p>Parchment, 280 x 200 mm, 195 folios.</p>
  </note>
  <note rend="MATERIAL">
    <p><material>Parchment</material> in good condition throughout.</p>
  </note>
  <note rend="QUIRES">
    <list>
      <head>Collation:</head>
      <item n="I">
        <locus from="1" to="8">1-8</locus>
      </item>
      <item n="V"><locus from="33" to="39">33-39</locus>(1 leaf wanting after fol. 39) </item>
    </list>
    <p>ii+195+ii. Remnants of signatures, e.g. fol. <locus>9r</locus>; dual page numbers at
       <locus from="38" to="39">38/39</locus>, and 180/181.</p>
  </note>
  <note rend="SCRIPT">
    <p>Scribe two is responsible for fols. <locus>10r</locus> through
      <locus>15v</locus>.</p>
  </note>
  <note>
    <p>Description by Timothy L. Stinson</p>
  </note>`);

// Abridged from archive/pizan/Francais1176/Francais1176.description_en.xml
const FRANCAIS1176 = tei(`
  <note rend="DECORATION">
    <p>The style has been identified as <hi rend="italics">"aux brins de fantaisie"</hi>. In folio <locus>1</locus>, the initial I.</p>
    <p>There are many small gold leaf initials.</p>
  </note>
  <note rend="AUTHOR">
    <p>
*The information comes from Ouy, Gilbert.<hi rend="italics"> Album Christine de Pizan </hi>. Turnhout: Brepols, 2012. </p>
<p> Description by Mónica Ann Walker Vadillo</p>
  </note>`);

describe('humanizeRend', () => {
  it('turns upper case rend values into sentence case', () => {
    expect(humanizeRend('BASIC INFORMATION')).toBe('Basic information');
    expect(humanizeRend('QUIRES')).toBe('Quires');
    expect(humanizeRend(' text ')).toBe('Text');
    expect(humanizeRend('')).toBe('');
  });
});

describe('parseDescription', () => {
  it('reads every notesStmt note as a section, in order', () => {
    const { sections } = parseDescription(DOUCE332);
    expect(sections.map((s) => s.heading)).toEqual([
      'Identification',
      'Basic information',
      'Material',
      'Quires',
      'Script',
      null,
    ]);
    expect(sections[0].key).toBe('IDENTIFICATION');
    expect(sections[0].blocks).toEqual([
      {
        type: 'paragraph',
        content: [
          { type: 'text', text: 'MS Douce 332, Bodleian Library, Oxford. French, 15th century.' },
        ],
      },
    ]);
  });

  it('keeps a note without rend as an unheaded credit section', () => {
    const credit = parseDescription(DOUCE332).sections.at(-1);
    expect(credit).toEqual({
      key: null,
      heading: null,
      blocks: [
        {
          type: 'paragraph',
          content: [{ type: 'text', text: 'Description by Timothy L. Stinson' }],
        },
      ],
    });
  });

  it('keeps the content of material and other unknown elements', () => {
    const material = parseDescription(DOUCE332).sections[2];
    expect(inlineText(material.blocks[0].content)).toBe('Parchment in good condition throughout.');
  });

  it('reads the collation as a labeled list with a caption', () => {
    const [list, paragraph] = parseDescription(DOUCE332).sections[3].blocks;
    expect(list.type).toBe('list');
    expect(list.labeled).toBe(true);
    expect(list.caption).toEqual([{ type: 'text', text: 'Collation:' }]);
    expect(list.items).toEqual([
      { label: 'I', content: [{ type: 'locus', text: '1-8', from: '1', to: '8' }] },
      {
        label: 'V',
        content: [
          { type: 'locus', text: '33-39', from: '33', to: '39' },
          { type: 'text', text: ' (1 leaf wanting after fol. 39)' },
        ],
      },
    ]);
    expect(paragraph.type).toBe('paragraph');
    expect(paragraph.content.filter((n) => n.type === 'locus')).toEqual([
      { type: 'locus', text: '9r', from: null, to: null },
      { type: 'locus', text: '38/39', from: '38', to: '39' },
    ]);
  });

  it('collapses whitespace around loci', () => {
    const script = parseDescription(DOUCE332).sections[4];
    expect(inlineText(script.blocks[0].content)).toBe(
      'Scribe two is responsible for fols. 10r through 15v.'
    );
  });

  it('reads several paragraphs and italic hi', () => {
    const [decoration, author] = parseDescription(FRANCAIS1176).sections;
    expect(decoration.blocks).toHaveLength(2);
    expect(decoration.blocks[0].content).toEqual([
      { type: 'text', text: 'The style has been identified as ' },
      { type: 'italic', children: [{ type: 'text', text: '"aux brins de fantaisie"' }] },
      { type: 'text', text: '. In folio ' },
      { type: 'locus', text: '1', from: null, to: null },
      { type: 'text', text: ', the initial I.' },
    ]);
    expect(author.heading).toBe('Author');
    expect(author.blocks[0].content).toEqual([
      { type: 'text', text: '*The information comes from Ouy, Gilbert. ' },
      { type: 'italic', children: [{ type: 'text', text: 'Album Christine de Pizan' }] },
      { type: 'text', text: ' . Turnhout: Brepols, 2012.' },
    ]);
    expect(inlineText(author.blocks[1].content)).toBe('Description by Mónica Ann Walker Vadillo');
  });

  it('turns lb into line breaks without stray spaces (verse inscriptions)', () => {
    const { sections } = parseDescription(
      tei(`<note rend="HISTORY"><p>On the flyleaf:
        <hi rend="italics">Mesmes Francoys premier du nom<lb/>
          Roy des francoys de grant renom<lb/></hi>
        </p></note>`)
    );
    expect(sections[0].blocks[0].content).toEqual([
      { type: 'text', text: 'On the flyleaf: ' },
      {
        type: 'italic',
        children: [
          { type: 'text', text: 'Mesmes Francoys premier du nom' },
          { type: 'break' },
          { type: 'text', text: 'Roy des francoys de grant renom' },
          { type: 'break' },
        ],
      },
    ]);
  });

  it('treats the misspelt rend="italcs" as italic and other hi as plain text', () => {
    const { sections } = parseDescription(
      tei(`<note rend="TEXT"><p><hi rend="italcs">Roman</hi> and <hi>plain</hi></p></note>`)
    );
    expect(sections[0].blocks[0].content).toEqual([
      { type: 'italic', children: [{ type: 'text', text: 'Roman' }] },
      { type: 'text', text: ' and plain' },
    ]);
  });

  it('makes loose text in a note a paragraph and skips empty notes', () => {
    const { sections } = parseDescription(
      tei(`<note rend="BINDING">Modern binding.</note><note rend="EMPTY"> <p> </p> </note>`)
    );
    expect(sections).toHaveLength(1);
    expect(sections[0].blocks).toEqual([
      { type: 'paragraph', content: [{ type: 'text', text: 'Modern binding.' }] },
    ]);
  });

  it('reads a list without item labels as a plain list', () => {
    const { sections } = parseDescription(
      tei(`<note rend="TEXT"><list><item>First</item><item>Second</item></list></note>`)
    );
    expect(sections[0].blocks[0]).toMatchObject({ type: 'list', labeled: false, caption: null });
    expect(sections[0].blocks[0].items.map((i) => i.label)).toEqual([null, null]);
  });

  it('returns null without a notesStmt or without content', () => {
    expect(
      parseDescription(
        '<TEI xmlns="http://www.tei-c.org/ns/1.0"><teiHeader><fileDesc/></teiHeader></TEI>'
      )
    ).toBeNull();
    expect(parseDescription(tei('<note rend="A"> </note>'))).toBeNull();
    expect(parseDescription('')).toBeNull();
  });

  it('ignores content outside notesStmt', () => {
    const text = JSON.stringify(parseDescription(DOUCE332));
    expect(text).not.toContain('manuscript');
    expect(text).not.toContain('Roman de la Rose');
  });

  it('throws TeiParseError for malformed XML', () => {
    expect(() => parseDescription('<TEI><notesStmt></TEI>')).toThrow(TeiParseError);
  });

  it('keeps no markup or attributes from the file', () => {
    const { sections } = parseDescription(
      tei(`<note rend="TEXT"><p onclick="x()">A <ref target="javascript:alert(1)">link</ref>
        <hi rend="italics" style="color:red">b</hi> &lt;script&gt;</p></note>`)
    );
    const content = sections[0].blocks[0].content;
    expect(content).toEqual([
      { type: 'text', text: 'A link ' },
      { type: 'italic', children: [{ type: 'text', text: 'b' }] },
      { type: 'text', text: ' <script>' },
    ]);
  });
});

describe('normalizeInline', () => {
  it('merges text, trims the ends and drops empty wrappers', () => {
    expect(
      normalizeInline([
        { type: 'text', text: '  a ' },
        { type: 'italic', children: [{ type: 'text', text: ' ' }] },
        { type: 'text', text: ' b  ' },
        { type: 'break' },
      ])
    ).toEqual([{ type: 'text', text: 'a b' }]);
  });
});
