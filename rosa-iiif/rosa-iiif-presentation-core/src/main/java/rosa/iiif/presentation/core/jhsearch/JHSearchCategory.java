package rosa.iiif.presentation.core.jhsearch;

import rosa.search.model.SearchCategory;

public enum JHSearchCategory implements SearchCategory{
    AUTHOR("Author"),
    COMMON_NAME("Common Name"),
    NUM_PAGES("Number pages"),
    LOCATION("Current Location"),
    REPOSITORY("Repository"),
    DATE("Date"),
    ORIGIN("Origin"),
    TYPE("Type"),
    NUM_ILLUS("Number of Illustrations"),
    TRANSCRIPTION("Transcription");

    private String label;
    
    
    JHSearchCategory(String label) {
        this.label = label;
    }
    
    @Override
    public String getFieldName() {
        return "facet_" + name().toLowerCase();
    }

    @Override
    public String getCategoryLabel() {
        return label;
    }
}
