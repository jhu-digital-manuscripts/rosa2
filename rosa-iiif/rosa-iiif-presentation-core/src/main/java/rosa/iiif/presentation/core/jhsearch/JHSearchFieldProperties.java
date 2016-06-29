package rosa.iiif.presentation.core.jhsearch;

// TODO Load from properties file?

interface JHSearchFieldProperties {
    static final String MARGINALIA_LABEL = "Marginalia";
    static final String MARGINALIA_DESCRIPTION = "Notes written by a reader";
    static final String UNDERLINE_LABEL = "Underline";
    static final String UNDERLINE_DESCRIPTION = "Words or phrases in the printed text that have been underlined.";
    static final String EMPHASIS_LABEL = "Emphasis";
    static final String EMPHASIS_DESCRIPTION = "Words or phrases within the readers marginal notes that have been underlined or otherwise emphasized.";
    static final String ERRATA_LABEL = "Errata";
    static final String ERRATA_DESCRIPTION = "Corrections made by a reader to the printed text.";
    static final String MARK_LABEL = "Mark";
    static final String MARK_DESCRIPTION = "Pen marks made on a page that may not have consistent abstract meaning. Those marks not covered by 'Symbol'";
    static final String[] MARK_VALUES = {
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
             "sign", "Sign"
    };
    static final String SYMBOL_LABEL = "Symbol";
    static final String SYMBOL_DESCRIPTION = "Simple drawings that carry some abstract and consistent meaning.";
    static final String[] SYMBOL_VALUES = {
             "Asterisk", "Asterisk",
             "Bisectedcircle", "Bisectedcircle",
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
    static final String NUMERAL_LABEL = "Numeral";
    static final String NUMERAL_DESCRIPTION = "Numbers written in the book.";
    static final String DRAWING_LABEL = "Drawing";
    static final String DRAWING_DESCRIPTION = "Drawings or diagrams.";
    static final String CROSS_REFERENCE_LABEL = "Cross Reference";
    static final String CROSS_REFERENCE_DESCRIPTION = "";
    static final String TRANSCRIPTION_LABEL = "Transcription";
    static final String TRANSCRIPTION_DESCRIPTION = "";
    static final String ILLUSTRATION_LABEL = "Transcription";
    static final String ILLUSTRATION_DESCRIPTION = "Description of illustrations";
    static final String DESCRIPTION_LABEL = "Description";
    static final String DESCRIPTION_DESCRIPTION = "";
    static final String LANGUAGE_LABEL ="Language";
    static final String LANGUAGE_DESCRIPTION = "";
    static final String[] LANGUAGE_VALUES = { 
    			"en", "English", 
    			"es", "Spanish", 
    			"it", "Italian", 
    			"el", "Greek", 
    			"la", "Latin", 
    			"fr", "French"
    };
    static final String BOOK_LABEL = "Book";
    static final String BOOK_DESCRIPTION = "Books mentioned in marginalia.";
    static final String PEOPLE_LABEL = "People";
    static final String PEOPLE_DESCRIPTION = "People mentioned in marginalia.";
    static final String PLACE_LABEL = "Place";
    static final String PLACE_DESCRIPTION = "Places mentioned in marginalia.";    
    static final String METHOD_LABEL = "Method";
    static final String METHOD_DESCRIPTION = "Implement used to create mark or underline.";
    static final String[] METHOD_VALUES = { 
    		"pen", "Pen", "chalk", "Chalk"
    };
}
