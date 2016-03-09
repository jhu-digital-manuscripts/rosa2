package rosa.search.model;

/**
 * A search field has a name and a set of types. A search field may be indexed
 * using any of these types.
 */
public interface SearchField {
    String getFieldName();

    SearchFieldType[] getFieldTypes();

    /**
     * @return Whether or not to potentially include in search results as match
     *         context
     */
    boolean isContext();

    /**
     * @return Whether or not to include field value in search match.
     */
    boolean includeValue();
}
