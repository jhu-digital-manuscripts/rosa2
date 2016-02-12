package rosa.website.search.client.model;


import rosa.search.model.SearchField;
import rosa.search.model.SearchFieldType;

/**
 * Available search fields.
 */

// TODO Doc each field.
public enum WebsiteSearchFields implements SearchField {
    ID(SearchFieldType.STRING),
    COLLECTION_ID(SearchFieldType.STRING),
    BOOK_ID(SearchFieldType.STRING),
    IMAGE_NAME(SearchFieldType.IMAGE_NAME),
    TRANSCRIPTION_TEXT(SearchFieldType.OLD_FRENCH),
    TRANSCRIPTION_RUBRIC(SearchFieldType.OLD_FRENCH),
    TRANSCRIPTION_LECOY(SearchFieldType.STRING),
    TRANSCRIPTION_NOTE(SearchFieldType.ENGLISH),
    DESCRIPTION_TEXT(SearchFieldType.ENGLISH, SearchFieldType.FRENCH),
    ILLUSTRATION_TITLE(SearchFieldType.ENGLISH),
    ILLUSTRATION_CHAR(SearchFieldType.OLD_FRENCH),
    ILLUSTRATION_KEYWORD(SearchFieldType.ENGLISH),
    NARRATIVE_SECTION_ID(SearchFieldType.STRING),
    NARRATIVE_SECTION_DESCRIPTION(SearchFieldType.ENGLISH);


    private final SearchFieldType[] types;

    WebsiteSearchFields(SearchFieldType... types) {
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
