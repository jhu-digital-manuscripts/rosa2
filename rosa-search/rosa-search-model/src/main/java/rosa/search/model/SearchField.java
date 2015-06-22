package rosa.search.model;

/**
 * A search field has a name and a set of types.
 */
public interface SearchField {
    String getFieldName();

    SearchFieldType[] getFieldTypes();
}
