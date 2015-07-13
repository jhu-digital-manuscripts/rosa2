package rosa.website.search.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.i18n.client.Constants;

public interface SearchLabels extends Constants {
    SearchLabels INSTANCE = GWT.create(SearchLabels.class);

    String categoryAllLabel();
    String categoryLinesOfVerseLabel();
    String categoryRubricLabel();
    String categoryIllustrationTitleLabel();
    String categoryLecoyLabel();
    String categoryCriticalNoteLabel();
    String categoryCharacterDepictedLabel();
    String categoryIllustrationKeywordsLabel();
    String categoryBookDescriptionLabel();
    String categoryFolioNumberLabel();
    String categoryNarrativeSectionsLabel();
    
}
