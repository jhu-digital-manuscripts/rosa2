package rosa.website.search.client.model;

/**
 * Represents a category by which a search can be restricted.
 * These categories map directly to {@link WebsiteSearchFields} which
 * are used in the search service.
 *
 * Enum names come from the old rosa1 UserFields used in its Lucene search widget.
 */
public enum SearchCategory {
    POETRY(WebsiteSearchFields.TRANSCRIPTION_TEXT),
    RUBRIC(WebsiteSearchFields.TRANSCRIPTION_RUBRIC),
    ILLUSTRATION_TITLE(WebsiteSearchFields.ILLUSTRATION_TITLE),
    LECOY(WebsiteSearchFields.TRANSCRIPTION_LECOY),
    NOTE(WebsiteSearchFields.TRANSCRIPTION_NOTE),
    ILLUSTRATION_CHAR(WebsiteSearchFields.ILLUSTRATION_CHAR),
    ILLUSTRATION_KEYWORDS(WebsiteSearchFields.ILLUSTRATION_KEYWORD),
    DESCRIPTION(WebsiteSearchFields.DESCRIPTION_TEXT),
    IMAGE(WebsiteSearchFields.IMAGE_NAME),
    NARRATIVE_SECTION(WebsiteSearchFields.NARRATIVE_SECTION_ID, WebsiteSearchFields.NARRATIVE_SECTION_DESCRIPTION),
    ALL(WebsiteSearchFields.TRANSCRIPTION_TEXT, WebsiteSearchFields.TRANSCRIPTION_RUBRIC, WebsiteSearchFields.ILLUSTRATION_TITLE, WebsiteSearchFields.TRANSCRIPTION_LECOY, WebsiteSearchFields.TRANSCRIPTION_NOTE, WebsiteSearchFields.ILLUSTRATION_CHAR,
            WebsiteSearchFields.ILLUSTRATION_KEYWORD, WebsiteSearchFields.DESCRIPTION_TEXT, WebsiteSearchFields.IMAGE_NAME, WebsiteSearchFields.NARRATIVE_SECTION_ID, WebsiteSearchFields.NARRATIVE_SECTION_DESCRIPTION);

    private WebsiteSearchFields[] fields;

    SearchCategory(WebsiteSearchFields... fields) {
        this.fields = fields;
    }

    /**
     * Get all related Lucene search fields.
     *
     * @return array of search fields
     */
    public WebsiteSearchFields[] getFields() {
        return fields;
    }

    public static SearchCategory category(String category) {
        for (SearchCategory cat : SearchCategory.values()) {
            if (cat.toString().equals(category)) {
                return cat;
            }
        }
        return null;
    }
}
