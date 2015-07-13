package rosa.website.search.client;

import org.junit.Test;

public class RosaQueryUtilTest {
    private final String token =
            "ALL;1234;POETRY;qwer-;rewq;RUBRIC;asdf;ALL;lkhj;BOOK;Marne3,AssembleeNationale1230,CodGall80;0";

    private RosaQueryUtil adapter = new RosaQueryUtil ();

    @Test
    public void queryPartsTest() {
        System.out.println(adapter.queryParts(token));
    }

    @Test
    public void toQueryTest() {
        System.out.println(adapter.toQuery(token));
    }

}
