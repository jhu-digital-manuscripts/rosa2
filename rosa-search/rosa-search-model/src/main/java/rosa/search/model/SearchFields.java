package rosa.search.model;

/**
 * Available search fields.
 */

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

    // AOR related search fields
    AOR_ALL(SearchFieldType.STRING),
    AOR_READER(SearchFieldType.STRING),
    AOR_PAGINATION(SearchFieldType.STRING),
    AOR_SIGNATURE(SearchFieldType.STRING),
    AOR_MARGINALIA_ALL(SearchFieldType.STRING),
    AOR_MARGINALIA_BOOKS(SearchFieldType.STRING),
    AOR_MARGINALIA_PEOPLE(SearchFieldType.STRING),
    AOR_MARGINALIA_LOCATIONS(SearchFieldType.STRING),
    AOR_MARGINALIA_TRANSCRIPTIONS(SearchFieldType.STRING),
    AOR_MARGINALIA_TRANSLATIONS(SearchFieldType.ENGLISH),
    AOR_MARKS(SearchFieldType.STRING),
    AOR_SYMBOLS(SearchFieldType.STRING),
    AOR_UNDERLINES(SearchFieldType.STRING),
    AOR_ERRATA(SearchFieldType.STRING),
    AOR_DRAWINGS(SearchFieldType.STRING),
    AOR_NUMERALS(SearchFieldType.STRING);

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
