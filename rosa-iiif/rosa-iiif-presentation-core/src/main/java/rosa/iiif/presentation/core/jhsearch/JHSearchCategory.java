package rosa.iiif.presentation.core.jhsearch;

import rosa.search.model.SearchCategory;

public enum JHSearchCategory implements SearchCategory{
    AUTHOR("Author"), COMMON_NAME("Name"), NUM_PAGES("Number pages"), LOCATION("Location"), REPOSITORY("Repository"), DATE("Date");

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
