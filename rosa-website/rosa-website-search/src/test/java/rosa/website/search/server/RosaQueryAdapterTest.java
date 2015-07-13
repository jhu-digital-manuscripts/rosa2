//package rosa.website.search.server;
//
//import org.junit.Test;
//import rosa.search.model.Query;
//import rosa.search.model.QueryOperation;
//import rosa.search.model.SearchFields;
//import rosa.website.search.client.RosaQueryAdapter;
//
//import static org.junit.Assert.assertEquals;
//
//public class RosaQueryAdapterTest {
//
//    private RosaQueryAdapter adapter = new RosaQueryAdapter();
//
//    @Test
//    public void testToToken() {
//        Query query = new Query(
//                QueryOperation.AND,
//                new Query(SearchFields.IMAGE_NAME, "001r"),
//                new Query(SearchFields.TRANSCRIPTION_TEXT, "Random text"),
//                new Query(
//                        QueryOperation.OR,
//                        new Query(SearchFields.BOOK_ID, "LudwigXV7"),
//                        new Query(SearchFields.BOOK_ID, "FolgersHa2")
//                )
//        );
//
//        String expected = "";
//
//        System.out.println(adapter.toToken(query));
//        assertEquals(expected, adapter.toToken(query));
//    }
//}
