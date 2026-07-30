# AoR Annotation Data Model

## Purpose

This document describes the Archaeology of Reading (AoR) annotation data model as represented in the rosa2 codebase. It covers the annotation types, their XML representation, and how they map to the internal Java data model.

## Audience

Researchers analyzing AoR annotation patterns and developers working with annotation data.

## Overview

AoR annotations record evidence of early modern reading practices. Each annotated page in a book is represented by an XML file containing structured annotation elements. The rosa2 tool parses these XML files into a typed data model for statistics generation, IIIF annotation output, and Opensearch indexing.

## Annotation Types

| Type | XML Element | Description |
|------|-------------|-------------|
| Marginalia | `<marginalia>` | Marginal notes with multi-language text, person/book/location references |
| Underline | `<underline>` | Underlined text passages |
| Mark | `<mark>` | Named marks with referenced text |
| Symbol | `<symbol>` | Symbolic annotations with referenced text |
| Numeral | `<numeral>` | Numeral annotations |
| Errata | `<errata>` | Corrections: original (copytext) and amended text |
| Drawing | `<drawing>` | Drawings with descriptive text, references, and symbols |
| Graph | `<graph>` | Graph annotations with nodes (text/person) and graph texts |
| Table | `<table>` | Table annotations with cells, headers, and references |
| Calculation | `<calculation>` | Mathematical calculations with type, method, data, and content |
| Physical Link | `<physical_link>` | Links between annotations on different pages |

## XML Structure

An annotated page XML file has the following top-level structure:

```xml
<annotated_page>
  <page filename="Ha2.001r" reader="Harvey" pagination="1">
    <marginalia>...</marginalia>
    <underline>...</underline>
    <mark>...</mark>
    <symbol>...</symbol>
    <numeral>...</numeral>
    <errata>...</errata>
    <drawing>...</drawing>
    <graph>...</graph>
    <table>...</table>
    <calculation>...</calculation>
    <physical_link>...</physical_link>
  </page>
</annotated_page>
```

## Marginalia Detail

Marginalia is the most complex annotation type, supporting multiple languages within a single note:

```xml
<marginalia hand="Harvey" method="pen" topic="Law">
  <language ident="la">
    <position place="right_margin" book_orientation="0">
      <marginalia_text>Ius Ciuile</marginalia_text>
      <person name="Justinian"/>
      <book title="Corpus Juris Civilis"/>
      <location name="Rome"/>
    </position>
  </language>
  <language ident="en">
    <position place="tail">
      <marginalia_text>Civil Law</marginalia_text>
    </position>
  </language>
</marginalia>
```

Each `<language>` element contains one or more `<position>` elements. Positions hold text content and reference entities (people, books, locations). Cross-references (`<X_ref>`) link to other texts and readers.

## Java Data Model Mapping

The XML elements map to the following Java classes in `rosa.archive.model.aor`:

| XML Element | Java Class | Key Fields |
|-------------|-----------|------------|
| `<annotated_page>` | `AnnotatedPage` | page, reader, pagination, annotations list |
| `<marginalia>` | `Marginalia` | hand, method, topic, languages list |
| `<language>` | `MarginaliaLanguage` | lang, positions list |
| `<position>` | `Position` | place, orientation, texts, people, books, locations |
| `<underline>` | `Underline` | method, type, language, text |
| `<mark>` | `Mark` | name, method, language, text |
| `<symbol>` | `Symbol` | name, language, text |
| `<numeral>` | `Numeral` | numeral value, text |
| `<errata>` | `Errata` | copytext, amendedtext |
| `<drawing>` | `Drawing` | texts, people, books, locations, symbols, translation |
| `<graph>` | `Graph` | nodes (GraphNode), graph_texts (GraphText), translation |
| `<table>` | `Table` | cells (TableCell), headers (TableHeader), people, books |
| `<calculation>` | `Calculation` | type, method, data, content |
| `<physical_link>` | `PhysicalLink` | annotation links |

All annotation types extend the `Annotation` sealed interface, enabling exhaustive pattern matching in processing code.
