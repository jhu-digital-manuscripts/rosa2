package rosa.website.search.client;

import org.junit.Test;
import rosa.search.model.Query;
import rosa.search.model.QueryTerm;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class RosaQueryUtilTest {
    private final String token =
            "ALL;1234;POETRY;qwer-;rewq;RUBRIC;asdf;ALL;lkhj;BOOK;Marne3,AssembleeNationale1230,CodGall80;25";

    private RosaQueryUtil adapter = new RosaQueryUtil ();

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
        Query query = adapter.toQuery(token);

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

}
