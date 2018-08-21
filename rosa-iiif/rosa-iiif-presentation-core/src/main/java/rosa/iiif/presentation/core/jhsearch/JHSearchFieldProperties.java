package rosa.iiif.presentation.core.jhsearch;

// TODO Load from properties file?

interface JHSearchFieldProperties {
    String MARGINALIA_LABEL = "Marginalia";
    String MARGINALIA_DESCRIPTION = "Notes written by a reader";
    String UNDERLINE_LABEL = "Underline";
    String UNDERLINE_DESCRIPTION = "Words or phrases in the printed text that have been underlined.";
    String EMPHASIS_LABEL = "Emphasis";
    String EMPHASIS_DESCRIPTION = "Words or phrases within the readers marginal notes that have been underlined or otherwise emphasized.";
    String ERRATA_LABEL = "Errata";
    String ERRATA_DESCRIPTION = "Corrections made by a reader to the printed text.";
    String MARK_LABEL = "Mark";
    String MARK_DESCRIPTION = "Pen marks made on a page that may not have consistent abstract meaning. Those marks not covered by 'Symbol'";
    String[] MARK_VALUES = {
            "apostrophe", "Apostrophe",
             "box", "Box",
             "bracket", "Bracket",
             "circumflex", "Circumflex",
             "colon", "Colon",
             "comma", "Comma",
             "dash", "Dash",
             "diacritic", "Diacritic",
             "dot", "Dot",
             "double_vertical_bar", "Double Vertical Bar",
             "equal_sign", "Equal Sign",
             "est_mark", "Est Mark",
             "hash", "Hash",
             "horizontal_bar", "Horizontal Bar",
             "page_break", "Page Break",
             "pen_trial", "Pen Trial",
             "plus_sign", "Plus Sign",
             "quotation_mark", "Quotation Mark",
             "scribble", "Scribble",
             "section_sign", "Section Sign",
             "semicolon", "Semicolon",
             "slash", "Slash",
             "straight_quotation_mark", "Straight Quotation Mark",
             "tick", "Tick",
             "tilde", "Tilde",
             "triple_dash", "Triple Dash",
             "vertical_bar", "Vertical Bar",
             "X_sign", "X Sign"
    };
    String SYMBOL_LABEL = "Symbol";
    String SYMBOL_DESCRIPTION = "Simple drawings that carry some abstract and consistent meaning.";
    String[] SYMBOL_VALUES = {
             "Asterisk", "Asterisk",
             "Bisected_circle", "Bisected Circle",
             "Crown", "Crown",
             "JC", "JC",
             "HT", "HT",
             "LL", "LL",
             "Mars", "Mars",
             "Mercury", "Mercury",
             "Moon", "Moon",
             "Opposite_planets", "Opposite Planets",
             "Saturn", "Saturn",
             "Square", "Square",
             "SS", "SS",
             "Sun", "Sun",
             "Venus", "Venus"
    };
    String NUMERAL_LABEL = "Numeral";
    String NUMERAL_DESCRIPTION = "Numbers written in the book.";
    String DRAWING_LABEL = "Drawing";
    String DRAWING_DESCRIPTION = "Drawings or diagrams.";
    String[] DRAWING_VALUES = {
            "face", "Face",
            "manicule", "Manicule",
            "florilegium", "Florilegium"
    };
    String CROSS_REFERENCE_LABEL = "Cross Reference";
    String CROSS_REFERENCE_DESCRIPTION = "Quotes from sources not explicitly identified by the reader.";
    String TRANSCRIPTION_LABEL = "Transcription";
    String TRANSCRIPTION_DESCRIPTION = "Search within transcriptions of manuscript texts.";
    String ILLUSTRATION_LABEL = "Illustrations";
    String ILLUSTRATION_DESCRIPTION = "Search within the descriptions of illustrations.";
    String DESCRIPTION_LABEL = "Description";
    String DESCRIPTION_DESCRIPTION = "Search within the metadata for the collection.";
    String LANGUAGE_LABEL ="Language";
    String LANGUAGE_DESCRIPTION = "";
    String[] LANGUAGE_VALUES = { 
    			"en", "English", 
    			"es", "Spanish", 
    			"it", "Italian", 
    			"el", "Greek", 
    			"la", "Latin", 
    			"fr", "French"
    };
    String MARG_LANGUAGE_LABEL = "Marginalia language";
    String MARG_LANGUAGE_DESCRIPTION = "Marginalia text in a single language.";
    String BOOK_LABEL = "Book";
    String BOOK_DESCRIPTION = "Titles of books that are referenced in text or annotations.";
    String PEOPLE_LABEL = "People";
    String PEOPLE_DESCRIPTION = "Search for names of people within metadata and transcriptions.";
    String PLACE_LABEL = "Place";
    String PLACE_DESCRIPTION = "Search for places and locations within metadata and transcriptions.";
    String METHOD_LABEL = "Method";
    String METHOD_DESCRIPTION = "Implement used to create mark or underline.";
    String[] METHOD_VALUES = { 
    		"pen", "Pen", "chalk", "Chalk"
    };

    String TITLE_LABEL = "Title";
    String TITLE_DESCRIPTION = "Search titles of items.";
    String REPO_LABEL = "Repository";
    String REPO_DESCRIPTION = "Search for names of repositories in which manuscripts are currently held";
    String TEXT_LABEL = "Text";
    String TEXT_DESCRIPTION = "Search the text of an item. This can include transcriptions and translations.";
    String CHAR_NAME_LABEL = "Character names";
    String CHAR_NAME_DESCRIPTION = "Search names of characters in the Roman de la Rose.";

    String HAND_LABEL = "Hand";
    String HAND_DESCRIPTION = "The hand in which an annotation was written";
    String CALC_LABEL = "Calculation";
    String CALC_DESCRIPTION = "Search through annotations used as calculations ";
    String GRAPH_LABEL = "Graph";
    String GRAPH_DESCRIPTION = "Search through graphs";
    String TABLE_LABEL = "Table";
    String TABLE_DESCRIPTION = "Search through table annotations";
    String ANNOTATOR_LABEL = "Annotator";
    String ANNOTATOR_DESCRIPTION = "Author of annotations";
}
