package rosa.search.model;

import org.junit.Test;

import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;

/**
 * Check equals and hashcode across all classes in the model.
 */
public class ModelEqualsTest {
    @Test
    public void testQuery() {
        Query prefab1 = new Query("field1", "this");
        Query prefab2 = new Query("field2", "that");

        EqualsVerifier.forClass(Query.class).allFieldsShouldBeUsed()
                .withPrefabValues(Query.class, prefab1, prefab2)
                .suppress(Warning.NONFINAL_FIELDS, Warning.STRICT_INHERITANCE)
                .verify();
    }

    @Test
    public void testQueryTerm() {
        EqualsVerifier.forClass(QueryTerm.class).allFieldsShouldBeUsed()
                .suppress(Warning.NONFINAL_FIELDS, Warning.STRICT_INHERITANCE)
                .verify();
    }

    @Test
    public void testSearchMatch() {
        EqualsVerifier.forClass(SearchMatch.class).allFieldsShouldBeUsed()
                .suppress(Warning.NONFINAL_FIELDS, Warning.STRICT_INHERITANCE)
                .verify();
    }

    @Test
    public void testSearchOptions() {
        EqualsVerifier.forClass(SearchOptions.class).allFieldsShouldBeUsed()
                .suppress(Warning.NONFINAL_FIELDS, Warning.STRICT_INHERITANCE)
                .verify();
    }

    @Test
    public void testSearchResult() {
        EqualsVerifier.forClass(SearchResult.class).allFieldsShouldBeUsed()
                .suppress(Warning.NONFINAL_FIELDS, Warning.STRICT_INHERITANCE)
                .verify();
    }
}
