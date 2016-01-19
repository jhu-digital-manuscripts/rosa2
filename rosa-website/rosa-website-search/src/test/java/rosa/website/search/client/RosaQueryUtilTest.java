package rosa.website.search.client;

import org.junit.Test;
import rosa.search.model.Query;
import rosa.search.model.QueryOperation;
import rosa.search.model.QueryTerm;
import rosa.website.search.client.model.SearchCategory;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class RosaQueryUtilTest {
    private final String token =
            "ALL;1234;POETRY;qwer-;rewq;RUBRIC;asdf;ALL;lkhj;BOOK;Marne3,AssembleeNationale1230,CodGall80;25";

    private RosaQueryUtil adapter = new RosaQueryUtil ();

    /**
     * Test query fragment with no BOOK restriction. Previous bug was found where in this case,
     * the offset value would be appended to the final query fragment.
     */
    @Test
    public void simpleQueryPartsTest() {
        List<QueryTerm> parts = adapter.queryParts("ALL;LudwigXV7;0");
        assertNotNull(parts);
        assertEquals("Unexpected number of query fragments found.", 1, parts.size());
        assertFalse("Offset value should not appear in query fragment.", parts.get(0).getValue().endsWith(";0"));

        parts = adapter.queryParts("ALL;LudwigXV7;ALL;FolgersHa2;0");
        assertNotNull(parts);
        assertEquals("Unexpected number of query fragments found.", 2, parts.size());
        assertFalse("Offset value should not appear in final query fragment.", parts.get(1).getValue().endsWith(";0"));
    }

    @Test
    public void queryPartsTest() {
        List<QueryTerm> parts = adapter.queryParts(token);
        assertNotNull(parts);
        assertEquals("Unexpected number of query fragments found.", 4, parts.size());

        parts = adapter.queryParts("POETRY;qwer-;rewq-;uiop-;poiu;ALL;nm,.");
        assertNotNull(parts);
        assertEquals("Unexpected number of query fragments found.", 2, parts.size());
    }

    @Test
    public void toQueryTest() {
        Query query = adapter.toQuery(token, "rosecollection");

        assertNotNull(query);
        assertTrue("Top level should have children", query.children() != null);
        for (Query child : query.children()) {
            if (child.children() != null) {
                for (Query third : child.children()) {
                    assertTrue("Third level should not have children nodes.", third.children() == null);
                }
            }
        }
    }

    @Test
    public void simpleToQueryTest() {
        Query query = adapter.toQuery("ALL;LudwigXV7;0", "rosecollection");

        assertNotNull(query);
        assertTrue("Top level query should be an operation.", query.isOperation());
        assertEquals("Top level query should be AND operation.", QueryOperation.AND, query.getOperation());
        assertEquals("Unexpected number of child queries.", 2, query.children().length);

        Query child = query.children()[0];

        assertTrue("First child query should be an operation.", child.isOperation());
        assertEquals("First child query should be OR operation.", QueryOperation.OR, child.getOperation());
        assertEquals("Unexpected number of child queries.", SearchCategory.ALL.getFields().length, child.children().length);

        child = query.children()[1];
        assertTrue("Second child should be term.", child.isTerm());
        assertEquals("Second child should be collection ID", "COLLECTION_ID", child.getTerm().getField());
        assertEquals("Second child should be for 'rosecollection'", "rosecollection", child.getTerm().getValue());
    }

    @Test
    public void bookListTest() {
        String[] list = adapter.bookRestrictionList(token);

        assertNotNull(list);
        assertEquals("Unexpected number of books found in list.", 3, list.length);
    }

    @Test
    public void offsetTest() {
        int offset = adapter.offset(token);
        assertEquals("Unexpected offset number found.", 25, offset);
    }

    @Test
    public void newOffsetTest() {
        String nt = adapter.changeOffset(token, 50);

        assertNotNull(nt);
        assertNotEquals("Original and new values should not be the same.", token, nt);
        assertTrue("New token should end with new offset value.", nt.endsWith(";" + String.valueOf(50)));

        nt = adapter.changeOffset("ALL;LudwigXV7;0", 20);

        assertNotNull(nt);
        assertNotEquals("Original and new values should not be the same.", "ALL;LudwigXV7;0", nt);
        assertTrue("New token should end with new offset value.", nt.endsWith(";" + String.valueOf(20)));
    }

}
