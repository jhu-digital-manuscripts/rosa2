# AoR Annotation Data Model

## Purpose

This document describes the Archaeology of Reading (AoR) annotation transcriptions as they
occur in the archive and how rosa-tool reads them into its Java model. It covers the XML
structure, the annotation types and their attributes, and which parts of the XML the
reader uses.

## Audience

Researchers analyzing AoR annotation patterns and developers working with annotation data.

## Overview

AoR transcriptions record the annotations early modern readers left in printed books. Each
annotated page has one XML file, `<book-id>.aor.<page>.xml` (for example
`FolgersHa2.aor.001r.xml`), next to the page image in the book directory. rosa-tool parses
these files (`ArchiveReaders.readAnnotatedPage`) into the `rosa.archive.model.aor` classes,
which feed the IIIF annotation pages, the Opensearch canvas documents and
`check-aor`.

The files declare the AoR schema with
`xsi:noNamespaceSchemaLocation="http://www.livesandletters.ac.uk/schema/aor2_18112016.xsd"`
(a few older files name `aor_20141111.xsd`). The schema is not part of this repository and
the tool never fetches it; `validate-xml` checks well-formedness, or validates against a
downloaded copy given with `--schema`.

## XML structure

```xml
<?xml version="1.0" encoding="UTF-8"?>
<transcription xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
               xsi:noNamespaceSchemaLocation="http://www.livesandletters.ac.uk/schema/aor2_18112016.xsd">
    <page filename="FolgersHa2.001r.tif" pagination="323" signature="A1r" reader="Harvey"/>
    <annotation>
        <marginalia hand="Italian" method="pen" anchor_text="...">...</marginalia>
        <underline method="pen" type="straight" language="LA" text="..."/>
        <mark name="plus_sign" method="pen" place="intext" language="LA" text="..."/>
        <symbol name="Mars" place="right_margin" language="LA" text="..."/>
        <numeral place="right_margin" language="LA" text="...">3</numeral>
        <errata language="LA" copytext="..." amendedtext="..."/>
        <drawing type="manicule" method="pen" place="left_margin" book_orientation="0">...</drawing>
        <graph type="family_tree" method="pen" place="full_page" book_orientation="0">...</graph>
        <table type="table" place="tail">...</table>
        <calculation type="..." method="pen" place="tail" book_orientation="0">...</calculation>
        <physical_link>...</physical_link>
    </annotation>
</transcription>
```

The root is `<transcription>`. Its first child, the empty `<page>` element, names the
image (`filename`, with its `.tif` extension) and carries the page's `pagination`,
`signature` and `reader`; `origin`, `status` and `host_page` occur on a few pages. The
annotations are the children of the sibling `<annotation>` element, in any order. The
reader uses the first `<page>` and the first `<annotation>` element.

Attributes shared by most annotation types:

| Attribute | Meaning |
|-----------|---------|
| `place` | Where on the page: `head`, `tail`, `left_margin`, `right_margin`, `intext`, `full_page` |
| `language` | Language of the annotation, uppercase ISO 639-1: `LA`, `EN`, `IT`, `EL`, `FR`, `ES`, `DE`, `CY`, `IW` |
| `text` | The printed text the annotation refers to (its anchor) |
| `method` | Writing implement: `pen`, `chalk`, `pencil`, `scoring`, `typewriter` |
| `book_orientation` | Rotation of the book when the annotation was written: `0`, `90`, `180`, `270` |
| `id` | Annotation id, used by internal references and physical links: `{book}:{page token}:{m\|u\|s…}NNNN`, where the page token is the library's original scan name (`00000027`, `615_d_7_f035r`, `84`); `aor/id_locations.csv` maps these ids to pages |
| `colour` | Ink colour (on some marks, underlines and marginalia) |

## Annotation types

| Type | XML element | Content |
|------|-------------|---------|
| Marginalia | `<marginalia>` | A reader's note, in one or more languages and positions, with translation and references |
| Underline | `<underline>` | Underlined printed text (`text`, `method`, `type`) |
| Mark | `<mark>` | A named mark such as `plus_sign` or `dash` next to printed text |
| Symbol | `<symbol>` | A named symbol such as `Mars` or `Sun` next to printed text |
| Numeral | `<numeral>` | A numeral written by the reader (element text) and the text it refers to |
| Errata | `<errata>` | A correction: `copytext` (as printed) and `amendedtext` |
| Drawing | `<drawing>` | A drawing (`type`, e.g. `manicule`) with texts, references and a translation |
| Graph | `<graph>` | A diagram (`type`, e.g. `family_tree`) of nodes, links and texts |
| Table | `<table>` | A table with headers, cells, texts and references |
| Calculation | `<calculation>` | A calculation (`type`, `method`), its content being the element text |
| Physical link | `<physical_link>` | A physical connection between annotations, e.g. a line drawn between them |

### Marginalia

```xml
<marginalia hand="Italian" method="pen" anchor_text="Romae" other_reader="..." date="..." id="FolgersHa2:00000012">
    <language ident="LA">
        <position place="head" book_orientation="0">
            <marginalia_text>Romae [-] Athenaru[m] populares humores</marginalia_text>
            <person name="Cicero"/>
            <book title="De officiis"/>
            <location name="Rome"/>
            <symbol_in_text name="Sun"/>
            <marginalia_symbol name="Mars"/>
            <X-ref person="Erasmus" book_title="Adages" language="LA" text="Omnium horarum homo"/>
            <emphasis method="pen" type="straight" language="LA" text="populares"/>
            <internal_ref text="infra">
                <target ref="FolgersHa2:00000034" text="i[nfr]a"/>
            </internal_ref>
        </position>
    </language>
    <language ident="EN">
        <position place="tail">
            <marginalia_text>...</marginalia_text>
        </position>
    </language>
    <translation>The popular humours of Rome and Athens ...</translation>
</marginalia>
```

A note has one `<language>` per language it uses (`ident` is the uppercase code) and each
language one or more `<position>` elements, each at a `place`. A position holds the note's
text (`<marginalia_text>`, possibly several), the people, books and places it names, the
symbols it contains (`<symbol_in_text>` and `<marginalia_symbol>`), cross-references to
other texts (`<X-ref>`), underlined words within the note (`<emphasis>`) and internal
references to other annotations (`<internal_ref>` with one or more `<target>`). A
`<target>` has a `ref`, the id of the annotation or page referred to (see `id` above),
the `text` the reader wrote (`fol[io] 35`, `60`, `i[nfr]a`) and optionally a `prefix`
and `suffix` around it; `<internal_ref>` may carry a `text` of its own (`infra`). The
`ref` values are inconsistent across books and sometimes wrong (`RCPD126.6:84` for
book `RCPD1266`); rosa-tool resolves them through `id_locations.csv`
(`AorReferenceResolver`) and `check-aor` reports the ones that do not resolve. The
`<translation>` is a direct child of `<marginalia>`. The attributes `hand`, `method`,
`anchor_text` (the printed text the note refers to), `other_reader` (a second reader),
`date`, `topic`, `id`, `book_id` and the continuation attributes
`marginalia_continues_from`/`_to` and `marginalia_from_transcription`/`_to_transcription`
(for notes that continue across pages) occur in the data.

### Drawing, graph, table, calculation, physical link

- `<drawing>` contains `<text hand language anchor_text>` elements (the writing in the
  drawing), `<person>`, `<book>`, `<location>`, `<symbol_in_text>` and `<translation>`.
- `<graph>` contains `<node id person anchor_text>` elements (the node text as content),
  `<link from to relationship>` elements between nodes, and `<graph_text>` elements holding
  `<note id hand language anchor_text internal_link>` elements with `<person>`, `<book>`,
  `<location>`, `<symbol_in_text>` and `<translation>`.
- `<table>` contains `<text>`, `<person>`, `<book>`, `<location>`, `<symbol_in_text>`,
  `<translation>` and rows `<tr>` of `<th label>` headers and `<td anchor_text>` cells.
- `<calculation>`'s content is the calculation as text.
- `<physical_link>` contains `<relation from to type>` elements naming the linked
  annotation ids and `<marginal_annotation idref>` elements.

## Java model

All annotation types implement the sealed interface `Annotation` through the sealed
abstract class `AbstractAnnotation`, which holds the shared fields `id`, `text` (the
anchor text, `getReferencedText()`), `location` (the `place`, as the `Location` enum),
`language`, `internalRef` and `generatedId` (meant to be true when the file gave no `id`;
nothing sets it today). The eleven
types are final, so code can switch over them exhaustively (as
`OpensearchIngestGenerator` does). `Marginalia` and `Graph` also implement `MultiPart`
(annotations that continue across pages).

| XML element | Java class | Fields read from the XML | Not read |
|-------------|-----------|--------------------------|----------|
| `<marginalia>` | `Marginalia` | `hand`, `method`, `date`, `other_reader`, `topic`, `anchor_text` (text), `place`, `id`, `translation`, `languages` | `book_id`, `colour`, `marginalia_continues_from/to`, `marginalia_from/to_transcription` (fields exist, never filled) |
| `<language>` | `MarginaliaLanguage` | `ident` (`lang`), `positions` | |
| `<position>` | `Position` | `place`, `book_orientation`, `marginalia_text` (texts), `person`, `book`, `location`, `symbol_in_text` and `marginalia_symbol` (symbols), `X-ref` (`XRef`), `emphasis` (as `Underline`), `internal_ref` (`InternalReference` with `ReferenceTarget`s) | `marginaliaRefs` is never filled |
| `<underline>` | `Underline` | `text`, `method`, `type`, `language`, `id`; place is always `INTEXT` | `colour` |
| `<mark>` | `Mark` | `text`, `name`, `method`, `language`, `place`, `id` | `colour` |
| `<symbol>` | `Symbol` | `text`, `name`, `language`, `place`, `id` | |
| `<numeral>` | `Numeral` | `text`, element content (`numeral`), `language`, `place`, `id` | |
| `<errata>` | `Errata` | `language`, `copytext` (text), `amendedtext` | `place` (set to `INTEXT`) |
| `<drawing>` | `Drawing` | `anchor_text`, `name`, `method`, `language`, `type`, `place`, `text` elements (`TextEl`), `person`, `book`, `location`, `symbol_in_text`, `translation` | `colour`, `book_orientation`, `internal_ref` |
| `<graph>` | `Graph` | `type`, `method`, `book_orientation`, `place`, `node` (`GraphNode`), `graph_text` (`GraphText`: `note` as `GraphNote`, people, books, locations, symbols, translations) | `link` (`links` stays empty), `graph_continues_*`, `graph_*_transcription` |
| `<table>` | `Table` | `type`, `place`, `text`, `person`, `book`, `location`, `symbol_in_text`, `tr/th` (`TableHeader`), `tr/td` (`TableCell`, row and column are always 0), `translation` | `internal_ref`, `aggregatedInfo` |
| `<calculation>` | `Calculation` | `type`, `method`, `book_orientation`, `place`, content | `data`, `internal_ref` |
| `<physical_link>` | `PhysicalLink` | `relation` (`AnnotationLink` with source, target, relationship) | `marginal_annotation` |
| `<page>` | `AnnotatedPage` | `filename` (page), `pagination`, `signature`, `reader`, plus one list per annotation type and `getAnnotations()` over all of them | `origin`, `status`, `host_page`, `related_page` |

`Location` has the values `HEAD`, `TAIL`, `LEFT_MARGIN`, `RIGHT_MARGIN`, `INTEXT` and
`FULL_PAGE`, matched to the `place` attribute ignoring case; an unknown or empty `place`
gives `null`.

## Data in the current archive

The 7,603 transcriptions in the `aor` collection contain 28,325 marginalia (29,088
`<language>` elements: 25,235 `LA`, 2,584 `EN`, then `IT`, `EL`, `FR`, `ES`, `DE`, `CY`,
`IW`; 25,961 translations), 107,869 underlines, 38,020 marks, 7,081 numerals, 5,082
symbols, 2,268 drawings, 1,214 errata, 108 physical links, 68 graphs (with 320 `<link>`
elements), 53 tables and 22 calculations, plus 188 cross-references (`<X-ref>`, 173 of whose persons
and 170 of whose titles are in the reference sheets) and 495 internal references with
734 targets (all but one resolve to a page through `id_locations.csv`). The `topic`
attribute does not occur.
