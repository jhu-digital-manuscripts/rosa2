package rosa.website.search.client.model;


import rosa.search.model.SearchField;
import rosa.search.model.SearchFieldType;

/**
 * Available search fields.
 */

// TODO Doc each field.
public enum WebsiteSearchFields implements SearchField {
    ID(false, SearchFieldType.STRING),
    COLLECTION_ID(false, SearchFieldType.STRING),
    BOOK_ID(false, SearchFieldType.STRING),
    IMAGE_NAME(true, SearchFieldType.IMAGE_NAME),
    TRANSCRIPTION_TEXT(true, SearchFieldType.OLD_FRENCH),
    TRANSCRIPTION_RUBRIC(true, SearchFieldType.OLD_FRENCH),
    TRANSCRIPTION_LECOY(true, SearchFieldType.STRING),
    TRANSCRIPTION_NOTE(true, SearchFieldType.ENGLISH),
    DESCRIPTION_TEXT(true, SearchFieldType.ENGLISH, SearchFieldType.FRENCH),
    ILLUSTRATION_TITLE(true, SearchFieldType.ENGLISH),
    ILLUSTRATION_CHAR(true, SearchFieldType.OLD_FRENCH),
    ILLUSTRATION_KEYWORD(true, SearchFieldType.ENGLISH),
    NARRATIVE_SECTION_ID(false, SearchFieldType.STRING),
    NARRATIVE_SECTION_DESCRIPTION(true, SearchFieldType.ENGLISH);

    private final SearchFieldType[] types;
    private final boolean is_context;

    WebsiteSearchFields(boolean is_context, SearchFieldType... types) {
        this.types = types;
        this.is_context = is_context;
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
        return is_context;
    }

    @Override
    public boolean includeValue() {
        return false;
    }
}
