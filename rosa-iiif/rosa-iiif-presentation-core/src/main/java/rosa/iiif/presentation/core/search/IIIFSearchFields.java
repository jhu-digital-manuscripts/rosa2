package rosa.iiif.presentation.core.search;

import rosa.search.model.SearchField;
import rosa.search.model.SearchFieldType;

/**
 * Each search document represents a text annotation associated an image.
 * 
 * ID is unique identifier for document.
 * COLLECTION is identifier for collection.
 * BOOK is identifier for book.
 * IMAGE is identifier for image.
 * TYPE is identifier for type of the annotation
 * LABEL is text describing the annotation
 * TARGET_
 */

public enum IIIFSearchFields implements SearchField {
    ID(SearchFieldType.STRING),
    COLLECTION(SearchFieldType.STRING),
    BOOK(SearchFieldType.STRING),
    IMAGE(SearchFieldType.STRING),
    TYPE(SearchFieldType.STRING),
    LABEL(SearchFieldType.STRING),
    TARGET_LABEL(SearchFieldType.STRING),
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

    @Override
    public boolean isContext() {
        return true;
    }

    @Override
    public boolean includeValue() {
        return false;
    }
}
