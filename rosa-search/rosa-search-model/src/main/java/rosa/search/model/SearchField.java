package rosa.search.model;

/**
 * A search field has a name and a set of types.
 */
public interface SearchField {
    String getFieldName();

    SearchFieldType[] getFieldTypes();
    
    /**
     * @return Whether or not to potentially include in search results as match context
     */
    boolean isContext();
    
    /**
     * @return Whether or not to include field value in search match.
     */
    boolean includeValue();
}
