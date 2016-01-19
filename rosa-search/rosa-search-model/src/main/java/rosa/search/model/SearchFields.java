package rosa.search.model;

/**
 * Available search fields.
 */

// TODO Doc each field.
public enum SearchFields implements SearchField {
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
    NARRATIVE_SECTION_DESCRIPTION(SearchFieldType.ENGLISH),
    TYPE(SearchFieldType.STRING),

    ANNOTATION_AUTHOR(SearchFieldType.STRING),
    ANNOTATION_TARGET(SearchFieldType.STRING),
    ANNOTATION_TEXT(SearchFieldType.ENGLISH, SearchFieldType.FRENCH, SearchFieldType.ITALIAN, SearchFieldType.GREEK, SearchFieldType.LATIN),
    ANNOTATION_NOTE(SearchFieldType.ENGLISH),
    ANNOTATION_HTML(SearchFieldType.STRING),
    ANNOTATION_MOTIVATION(SearchFieldType.STRING),;
    
    private final SearchFieldType[] types;

    SearchFields(SearchFieldType... types) {
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
