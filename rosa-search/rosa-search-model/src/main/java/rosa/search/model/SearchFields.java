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

    // AOR related search fields TODO handle other languages better
    AOR_ALL(SearchFieldType.ENGLISH),  // other langs
    AOR_READER(SearchFieldType.STRING),
    AOR_PAGINATION(SearchFieldType.STRING),
    AOR_SIGNATURE(SearchFieldType.STRING),
    AOR_MARGINALIA_ALL(SearchFieldType.ENGLISH),  // other langs
    AOR_MARGINALIA_BOOKS(SearchFieldType.ENGLISH),
    AOR_MARGINALIA_PEOPLE(SearchFieldType.ENGLISH),
    AOR_MARGINALIA_LOCATIONS(SearchFieldType.ENGLISH),
    AOR_MARGINALIA_TRANSCRIPTIONS(SearchFieldType.ENGLISH),  // other langs
    AOR_MARGINALIA_TRANSLATIONS(SearchFieldType.ENGLISH),
    AOR_MARGINALIA_INTERNAL_REFS(SearchFieldType.STRING),
    AOR_MARKS(SearchFieldType.ENGLISH),
    AOR_SYMBOLS(SearchFieldType.ENGLISH),
    AOR_UNDERLINES(SearchFieldType.ENGLISH),  // other langs
    AOR_ERRATA(SearchFieldType.ENGLISH),  // other langs
    AOR_DRAWINGS(SearchFieldType.ENGLISH),
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
