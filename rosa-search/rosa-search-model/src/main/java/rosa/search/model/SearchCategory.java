package rosa.search.model;

/**
 * TODO Add to SearchField? isCategory?
 * 
 * An object may be assigned a value in various categories. A search category
 * has an internal field name and a human readable label.
 */
public interface SearchCategory {
    /**
     * @return Internal name of the category as a search field.
     */
    public String getFieldName();

    /**
     * @return Human readable name for the category.
     */
    public String getCategoryLabel();
}
