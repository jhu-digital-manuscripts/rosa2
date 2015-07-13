package rosa.website.search.client;

import rosa.search.model.SearchFields;

/**
 * Represents a category by which a search can be restricted.
 * These categories map directly to {@link rosa.search.model.SearchFields} which
 * are used in the search service.
 *
 * Enum names come from the old rosa1 UserFields used in its Lucene search widget.
 */
public enum SearchCategory {
    ALL(SearchLabels.INSTANCE.categoryAllLabel(), SearchFields.values()),
    POETRY(SearchLabels.INSTANCE.categoryLinesOfVerseLabel(), SearchFields.TRANSCRIPTION_TEXT),
    RUBRIC(SearchLabels.INSTANCE.categoryRubricLabel(), SearchFields.TRANSCRIPTION_RUBRIC),
    ILLUSTRATION_TITLE(SearchLabels.INSTANCE.categoryIllustrationTitleLabel(), SearchFields.ILLUSTRATION_TITLE),
    LECOY(SearchLabels.INSTANCE.categoryLecoyLabel(), SearchFields.TRANSCRIPTION_LECOY),
    NOTE(SearchLabels.INSTANCE.categoryCriticalNoteLabel(), SearchFields.TRANSCRIPTION_NOTE),
    ILLUSTRATION_CHAR(SearchLabels.INSTANCE.categoryCharacterDepictedLabel(), SearchFields.ILLUSTRATION_CHAR),
    ILLUSTRATION_KEYWORDS(SearchLabels.INSTANCE.categoryIllustrationKeywordsLabel(), SearchFields.ILLUSTRATION_KEYWORD),
    DESCRIPTION(SearchLabels.INSTANCE.categoryBookDescriptionLabel(), SearchFields.DESCRIPTION_TEXT),
    IMAGE(SearchLabels.INSTANCE.categoryFolioNumberLabel(), SearchFields.IMAGE_NAME),
    NARRATIVE_SECTION(SearchLabels.INSTANCE.categoryNarrativeSectionsLabel(),
            SearchFields.NARRATIVE_SECTION_ID, SearchFields.NARRATIVE_SECTION_DESCRIPTION);

    private String display;
    private SearchFields[] fields;

    SearchCategory(String display, SearchFields... fields) {
        this.display = display;
        this.fields = fields;
    }

    /**
     * @return display string
     */
    public String getDisplay() {
        return display;
    }

    /**
     * Get all related Lucene search fields.
     *
     * @return array of search fields
     */
    public SearchFields[] getFields() {
        return fields;
    }
}
