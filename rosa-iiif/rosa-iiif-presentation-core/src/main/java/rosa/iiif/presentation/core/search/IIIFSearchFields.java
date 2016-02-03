package rosa.iiif.presentation.core.search;

import rosa.search.model.SearchField;
import rosa.search.model.SearchFieldType;

/**
 * Available search fields.
 */

// TODO Doc each field.
public enum IIIFSearchFields implements SearchField {
    ID(SearchFieldType.STRING),
    COLLECTION(SearchFieldType.STRING),
    BOOK(SearchFieldType.STRING),
    IMAGE(SearchFieldType.STRING),
    TYPE(SearchFieldType.STRING),
    TEXT(SearchFieldType.ENGLISH, SearchFieldType.FRENCH, SearchFieldType.ITALIAN, SearchFieldType.GREEK, SearchFieldType.SPANISH, SearchFieldType.LATIN);
    
    private final SearchFieldType[] types;

    
    IIIFSearchFields(SearchFieldType... types) {
        this.types = types;
    }

    @Override
    public SearchFieldType[] getFieldTypes() {
        return types;
    }

    @Override
    public String getFieldName() {
        return name();
    }
}
