package rosa.iiif.presentation.core.jhsearch;

import rosa.search.model.SearchField;
import rosa.search.model.SearchFieldType;

/**
 * Each search document represents a IIIF Presentation object.
 */

public enum JHSearchFields implements SearchField {
    OBJECT_ID(false, false, SearchFieldType.STRING),
    OBJECT_TYPE(false, true, SearchFieldType.STRING),
    OBJECT_LABEL(false, true, SearchFieldType.STRING),
    COLLECTION_ID(false, false, SearchFieldType.STRING),    
    MANIFEST_ID(false, true, SearchFieldType.STRING),
    MANIFEST_LABEL(false, true, SearchFieldType.STRING),
    MARGINALIA(true, false, SearchFieldType.ENGLISH, SearchFieldType.FRENCH, SearchFieldType.ITALIAN, SearchFieldType.GREEK, SearchFieldType.SPANISH, SearchFieldType.LATIN),
    UNDERLINE(true, false, SearchFieldType.ENGLISH, SearchFieldType.FRENCH, SearchFieldType.ITALIAN, SearchFieldType.GREEK, SearchFieldType.SPANISH, SearchFieldType.LATIN),
    ERRATA(true, false,SearchFieldType.ENGLISH, SearchFieldType.FRENCH, SearchFieldType.ITALIAN, SearchFieldType.GREEK, SearchFieldType.SPANISH, SearchFieldType.LATIN),
    MARK(true, false,SearchFieldType.STRING,SearchFieldType.ENGLISH, SearchFieldType.FRENCH, SearchFieldType.ITALIAN, SearchFieldType.GREEK, SearchFieldType.SPANISH, SearchFieldType.LATIN),
    SYMBOL(true, false,SearchFieldType.STRING),
    NUMERAL(true,false,SearchFieldType.STRING),
    DRAWING(true,false,SearchFieldType.STRING),
    IMAGE_NAME(true, false,SearchFieldType.IMAGE_NAME);

    private final SearchFieldType[] types;
    private final boolean context;
    private final boolean include;
    private final String field;
    
    JHSearchFields(boolean context, boolean include, SearchFieldType... types) {
        this.types = types;
        this.context = context;
        this.include = include;
        this.field = name().toLowerCase();
    }

    @Override
    public SearchFieldType[] getFieldTypes() {
        return types;
    }

    @Override
    public String getFieldName() {
        return field;
    }
    
    @Override
    public boolean isContext() {
        return context;
    }

    @Override
    public boolean includeValue() {
        return include;
    }
}
