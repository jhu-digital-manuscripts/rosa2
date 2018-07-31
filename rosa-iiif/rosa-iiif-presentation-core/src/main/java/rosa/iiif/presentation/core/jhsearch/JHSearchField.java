package rosa.iiif.presentation.core.jhsearch;

import rosa.search.model.SearchField;
import rosa.search.model.SearchFieldType;

/**
 * Each search document represents a IIIF Presentation object.
 */

public enum JHSearchField implements SearchField, JHSearchFieldProperties {
    OBJECT_ID(false, false, SearchFieldType.STRING),
    OBJECT_TYPE(false, true, SearchFieldType.STRING),
    OBJECT_LABEL(false, true, SearchFieldType.STRING),
    COLLECTION_ID(false, false, SearchFieldType.STRING),    
    MANIFEST_ID(false, true, SearchFieldType.STRING),
    MANIFEST_LABEL(false, true, SearchFieldType.STRING),
    IMAGE_NAME(true, false, SearchFieldType.IMAGE_NAME),
    MARGINALIA(true, false, true, MARGINALIA_LABEL, MARGINALIA_DESCRIPTION, SearchFieldType.ENGLISH, SearchFieldType.FRENCH, SearchFieldType.ITALIAN, SearchFieldType.GREEK, SearchFieldType.SPANISH, SearchFieldType.LATIN, SearchFieldType.GERMAN),
    UNDERLINE(true, false, true, UNDERLINE_LABEL, UNDERLINE_DESCRIPTION, SearchFieldType.ENGLISH, SearchFieldType.FRENCH, SearchFieldType.ITALIAN, SearchFieldType.GREEK, SearchFieldType.SPANISH, SearchFieldType.LATIN, SearchFieldType.GERMAN),
    EMPHASIS(true, false, true, EMPHASIS_LABEL, EMPHASIS_DESCRIPTION, SearchFieldType.ENGLISH, SearchFieldType.FRENCH, SearchFieldType.ITALIAN, SearchFieldType.GREEK, SearchFieldType.SPANISH, SearchFieldType.LATIN, SearchFieldType.GERMAN),
    ERRATA(true, false,true, ERRATA_LABEL, ERRATA_DESCRIPTION, SearchFieldType.ENGLISH, SearchFieldType.FRENCH, SearchFieldType.ITALIAN, SearchFieldType.GREEK, SearchFieldType.SPANISH, SearchFieldType.LATIN, SearchFieldType.GERMAN),
    MARK(true, false, true, MARK_LABEL, MARK_DESCRIPTION, MARK_VALUES, SearchFieldType.STRING, SearchFieldType.ENGLISH, SearchFieldType.FRENCH, SearchFieldType.ITALIAN, SearchFieldType.GREEK, SearchFieldType.SPANISH, SearchFieldType.LATIN, SearchFieldType.GERMAN),
    SYMBOL(true, false,true, SYMBOL_LABEL, SYMBOL_DESCRIPTION, SYMBOL_VALUES, SearchFieldType.STRING, SearchFieldType.ENGLISH, SearchFieldType.FRENCH, SearchFieldType.ITALIAN, SearchFieldType.GREEK, SearchFieldType.SPANISH, SearchFieldType.LATIN, SearchFieldType.GERMAN),
    NUMERAL(true,false,true, NUMERAL_LABEL, NUMERAL_DESCRIPTION, SearchFieldType.STRING, SearchFieldType.ENGLISH, SearchFieldType.FRENCH, SearchFieldType.ITALIAN, SearchFieldType.GREEK, SearchFieldType.SPANISH, SearchFieldType.LATIN, SearchFieldType.GERMAN),
    DRAWING(true,false,true, DRAWING_LABEL, DRAWING_DESCRIPTION, DRAWING_VALUES, SearchFieldType.STRING, SearchFieldType.ENGLISH, SearchFieldType.FRENCH, SearchFieldType.ITALIAN, SearchFieldType.GREEK, SearchFieldType.SPANISH, SearchFieldType.LATIN, SearchFieldType.GERMAN),
    CROSS_REFERENCE(true, false,true, CROSS_REFERENCE_LABEL, CROSS_REFERENCE_DESCRIPTION, SearchFieldType.ENGLISH, SearchFieldType.FRENCH, SearchFieldType.ITALIAN, SearchFieldType.GREEK, SearchFieldType.SPANISH, SearchFieldType.LATIN, SearchFieldType.GERMAN),
    TRANSCRIPTION(true, false, true, TRANSCRIPTION_LABEL, TRANSCRIPTION_DESCRIPTION, SearchFieldType.OLD_FRENCH, SearchFieldType.ENGLISH),
    ILLUSTRATION(true, false, true, ILLUSTRATION_LABEL, ILLUSTRATION_DESCRIPTION, SearchFieldType.ENGLISH),
    LANGUAGE(false, false, true, LANGUAGE_LABEL, LANGUAGE_DESCRIPTION, LANGUAGE_VALUES, SearchFieldType.STRING),
    MARGINALIA_LANGUAGE(false, false, true, MARG_LANGUAGE_LABEL, MARG_LANGUAGE_DESCRIPTION, LANGUAGE_VALUES, SearchFieldType.STRING),
    BOOK(true, false,true, BOOK_LABEL, BOOK_DESCRIPTION, SearchFieldType.ENGLISH),
    METHOD(false, false, true, METHOD_LABEL, METHOD_DESCRIPTION, METHOD_VALUES, SearchFieldType.STRING),

    // Set of fields that will be shared among all collections
    TITLE(true, false, true, TITLE_LABEL, TITLE_DESCRIPTION, SearchFieldType.ENGLISH, SearchFieldType.OLD_FRENCH, SearchFieldType.FRENCH, SearchFieldType.LATIN, SearchFieldType.GREEK, SearchFieldType.ITALIAN, SearchFieldType.SPANISH),
    PEOPLE(true, false, true, PEOPLE_LABEL, PEOPLE_DESCRIPTION, SearchFieldType.ENGLISH),
    PLACE(true, false, true, PLACE_LABEL, PLACE_DESCRIPTION, SearchFieldType.ENGLISH),
    // Include 'author'/'creator' ?
    REPO(true, false, true, REPO_LABEL, REPO_DESCRIPTION, SearchFieldType.ENGLISH),
    DESCRIPTION(true, false, true, DESCRIPTION_LABEL, DESCRIPTION_DESCRIPTION, SearchFieldType.ENGLISH),
    TEXT(true, false, true, TEXT_LABEL, TEXT_DESCRIPTION, SearchFieldType.ENGLISH, SearchFieldType.FRENCH, SearchFieldType.OLD_FRENCH, SearchFieldType.ITALIAN, SearchFieldType.GREEK, SearchFieldType.SPANISH, SearchFieldType.LATIN),
    CHAR_NAME(true, false, true, CHAR_NAME_LABEL, CHAR_NAME_DESCRIPTION, SearchFieldType.ENGLISH, SearchFieldType.OLD_FRENCH, SearchFieldType.FRENCH)
    ;
    
     // TODO Move some of this to SearchField?
    
    private final SearchFieldType[] types;
    private final boolean context;
    private final boolean include;
    private final boolean expose;
    private final String field;
    private final String label;
    private final String description;
    private final String[] value_label_pairs;
    
    /**
     * 
     * @param context - Context in search result
     * @param include - Include value in search result
     * @param expose - Advertise to user
     * @param label - Label of field for user
     * @param description - Description of field for user
     * @param value_label_pairs (value, label) pairs for suggested values or null
     * @param types .
     */
    JHSearchField(boolean context, boolean include, boolean expose, String label, String description, String[] value_label_pairs, SearchFieldType... types) {
        this.types = types;
        this.context = context;
        this.include = include;
        this.expose = expose;
        this.label = label;
        this.description = description;
        this.value_label_pairs = value_label_pairs;
        this.field = name().toLowerCase();
    }
    
    JHSearchField(boolean context, boolean include, boolean expose, String label, String description, SearchFieldType... types) {
        this(context, include, expose, label, description, null, types);
    }
    
    JHSearchField(boolean context, boolean include, SearchFieldType... types) {
        this(context, include, false, null, null, null, types);
    }

    @Override
    public SearchFieldType[] getFieldTypes() {
        return types;
    }

    @Override
    public String getFieldName() {
        return field;
    }
    
    public String getLabel() {
        return label;
    }
    
    public String getDescription() {
        return description;
    }
    
    @Override
    public boolean isContext() {
        return context;
    }

    @Override
    public boolean includeValue() {
        return include;
    }
    
    public String[] getValueLabelPairs() {
        return value_label_pairs;
    }
    
    public boolean isExposed() {
        return expose;
    }
}
