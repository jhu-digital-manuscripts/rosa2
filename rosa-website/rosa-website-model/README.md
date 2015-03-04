data model (separate module)

  * Use archive data model directly? or create separate website data model?
    * Convert data to CSVs that can be referenced

  * rosa.archive.model.Collection CSV
    * (all) ID, name, origin, material, number of folios, height, width, leaves per gathering, lines per column, number of illustrations,
        date start, date end, columns per folio, texts, folios with 1 illustration, folios with more than 1 illustrations
    * (Rose) Country, Locality, Institution, Shelfmark, Olim, de la Rose?, Langlois?, Notes
    * (Rose) Name, Date, Folios, illustrations, columns, lines/col, size, structure, >1 ill
    * (Pizan) Title, Date, MS, Editions, Translations (english), Translations (french), Translations (other), Digitized mss/editions
  * Narrative Sections CSV (same as CSV in archive)
    * ID, Description, Lecoy
  * Illustration Titles CSV (adapted from CSV in archive)
    * Position, Title, Frequency
  * Character Names CSV (same as CSV in archive)
    * Name, French, English