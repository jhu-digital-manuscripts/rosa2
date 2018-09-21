package rosa.iiif.presentation.core.jhsearch;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import rosa.archive.core.BaseSearchTest;
import rosa.archive.core.Store;
import rosa.archive.core.StoreImpl;
import rosa.archive.model.BookCollection;
import rosa.iiif.image.core.IIIFRequestFormatter;
import rosa.iiif.presentation.core.IIIFPresentationRequestFormatter;
import rosa.iiif.presentation.core.PresentationUris;
import rosa.iiif.presentation.core.StaticResourceRequestFormatter;
import rosa.iiif.presentation.model.IIIFNames;
import rosa.iiif.presentation.model.PresentationRequest;
import rosa.iiif.presentation.model.PresentationRequestType;
import rosa.search.model.Query;
import rosa.search.model.QueryOperation;
import rosa.search.model.QueryTerm;
import rosa.search.model.SearchCategory;
import rosa.search.model.SearchMatch;
import rosa.search.model.SearchOptions;
import rosa.search.model.SearchResult;
import rosa.search.model.SortOrder;

public class LuceneJHSearchServiceTest extends BaseSearchTest {
    private static LuceneJHSearchService service;

    @ClassRule
    public static TemporaryFolder tmpfolder = new TemporaryFolder();

    @BeforeClass
    public static void setup() throws Exception {
        String scheme = "http";
        String host = "serenity.dkc.jhu.edu";
        int port = 80;
        String pres_prefix = "/pres";
        String image_prefix = "/image";

        IIIFPresentationRequestFormatter presFormatter = new IIIFPresentationRequestFormatter(scheme, host, pres_prefix, port);
        IIIFRequestFormatter imageFormatter = new IIIFRequestFormatter(scheme, host, port, image_prefix);
        StaticResourceRequestFormatter staticFormatter = new StaticResourceRequestFormatter(scheme, host, pres_prefix, port);
        
        service = new LuceneJHSearchService(tmpfolder.newFolder().toPath(), new PresentationUris(presFormatter, imageFormatter, staticFormatter));
        service.update(store, VALID_COLLECTION);
        
        assertTrue(service.has_content());
    }
    
    @AfterClass
    public static void cleanup() {
        if (service != null) {
            service.shutdown();
        }
    }

    @Test
    public void testRequiredResultsFields() throws Exception {
        Query query = new Query(JHSearchField.TEXT, "Moo");
        SearchResult result = service.search(query, new SearchOptions(0, 20, SortOrder.RELEVANCE));

        assertNotNull(result);
        assertEquals(SortOrder.RELEVANCE, result.getSortOrder());
        assertEquals(20, result.getMaxMatches());
        assertEquals(0, result.getOffset());
    }

    @Test
    public void testSearchSymbolSun() throws Exception {

        Query query = new Query(JHSearchField.SYMBOL, "Sun");
        SearchResult result = service.search(query, null);

        assertNotNull("Search result was NULL", result);
        assertEquals("Unexpected number of results found.", 37, result.getTotal());
    }

    @Test
    public void testSearchSymbolMars() throws Exception {

        Query query = new Query(JHSearchField.SYMBOL, "Mars");
        SearchResult result = service.search(query, null);

        assertNotNull("Search result was NULL", result);
        assertEquals("Unexpected number of results found.", 2, result.getTotal());
    }

    @Test
    public void testSearchSymbolAndMarginalia() throws Exception {
        Query query = new Query(QueryOperation.AND, new Query(JHSearchField.SYMBOL, "Mars"),
                new Query(JHSearchField.MARGINALIA, "Homer"));
        SearchResult result = service.search(query, null);

        assertNotNull("Search result was NULL", result);
        assertEquals("Unexpected number of results found.", 1, result.getTotal());
    }
    
    @Test
    public void testStemLatin() throws Exception {
        Query query = new Query(JHSearchField.MARGINALIA, "maximus");
        SearchOptions opts = new SearchOptions();
        opts.setSortOrder(SortOrder.INDEX);
        SearchResult result = service.search(query, opts);

        assertNotNull("Search result was NULL", result);
        assertEquals("Unexpected number of results found.", 17, result.getTotal());
    }

    @Test
    public void testCaseLatin() throws Exception {
    	{
	        Query query = new Query(JHSearchField.MARGINALIA, "Quod");
	        SearchResult result = service.search(query, null);

	        assertNotNull("Search result was NULL", result);
	        assertEquals("Unexpected number of results found.", 5, result.getTotal());
    	}
    	
    	{
	        Query query = new Query(JHSearchField.MARGINALIA, "quod");
	        SearchResult result = service.search(query, null);
	
	        assertNotNull("Search result was NULL", result);
	        assertEquals("Unexpected number of results found.", 5, result.getTotal());
    	}
    }
    
    
    @Test
    public void testSearchMarginaliaTranslationPhrase() throws Exception {
        Query query = new Query(JHSearchField.MARGINALIA, "\"if you wish, you may command the citizens.\"");
        SearchResult result = service.search(query, null);

        assertNotNull("Search result was NULL.", result);
        assertEquals("Unexpected number of results found.", 1, result.getTotal());
        assertTrue("Unexpected result ID found.", result.getMatches()[0].getId().contains("36v"));
    }

    @Test
    public void testStripTranscibersMarks() {
    	assertEquals("supra, infra",   JHSearchLuceneMapper.stripTranscribersMarks("s[upr]a, i[nfr]a"));
    }
    
    @Test
    public void testSearchMarginaliaIgnoringTranscriberMarks() throws Exception {
        Query query = new Query(JHSearchField.MARGINALIA, "supra");
        SearchOptions opts = new SearchOptions();
        opts.setSortOrder(SortOrder.INDEX);
        
        SearchResult result = service.search(query, opts);

        assertNotNull("Search result was NULL.", result);

        assertEquals(20, result.getTotal());
        
        
        
        for (SearchMatch m: result.getMatches()) {
        	m.getContext().forEach(text -> {
        		assertFalse(text.contains("["));
        		assertFalse(text.contains("]"));
        	});
        }
        
        assertTrue("Unexpected context found.", result.getMatches()[0].getContext().contains("<B>supra</B>"));
    }

    @Test
    public void testSearchMarginaliaAnchorText() throws Exception {
        Query query = new Query(JHSearchField.MARGINALIA, "Giudeo");
        SearchResult result = service.search(query, null);

        assertNotNull("Search result was NULL.", result);
        assertEquals("Unexpected number of results found.", 1, result.getTotal());
        assertTrue("Unexpected result ID found.", result.getMatches()[0].getId().contains("7r"));
    }
    
    @Test
    public void testSearchLanguage() throws Exception {
        Query query = new Query(JHSearchField.LANGUAGE, "it");
        SearchResult result = service.search(query, null);

        assertNotNull("Search result was NULL.", result);
        assertEquals("Unexpected number of results found.", 76, result.getTotal());
    }
    
    @Test
    public void testSearchLanguageInMarginalia() throws Exception {
        Query query = new Query(JHSearchField.LANGUAGE, "la");
        SearchResult result = service.search(query, null);

        assertNotNull("Search result was NULL.", result);
        assertEquals("Unexpected number of results found.", 74, result.getTotal());
    }

    @Test
    public void test() throws Exception {
        Query query = new Query(JHSearchField.MARGINALIA_LANGUAGE, "it");
        SearchResult result = service.search(query, null);

        assertNotNull("Search results was NULL.", result);
        assertEquals("Unexpected number of results found.", 56, result.getTotal());
    }
    
    @Test
    public void testSearchMethod() throws Exception {
        Query query = new Query(JHSearchField.METHOD, "chalk");
        SearchResult result = service.search(query, null);

        assertNotNull("Search result was NULL.", result);
        assertEquals("Unexpected number of results found.", 27, result.getTotal());
    }    
    
    @Test
    public void testSearchBook() throws Exception {
        Query query = new Query(JHSearchField.BOOK, "\"Theatrum vitae humane\"");
        SearchResult result = service.search(query, null);

        assertNotNull("Search result was NULL.", result);
        assertEquals("Unexpected number of results found.", 1, result.getTotal());
        assertTrue("Unexpected result ID found.", result.getMatches()[0].getId().contains("3v"));
    }
    
    @Test
    public void testSearchPlaces() throws Exception {
        Query query = new Query(JHSearchField.PLACE, "Athens");
        SearchResult result = service.search(query, null);

        assertNotNull("Search result was NULL.", result);
        assertEquals("Unexpected number of results found.", 1, result.getTotal());
        assertTrue("Unexpected result ID found.", result.getMatches()[0].getId().contains("1r"));
    }
    
    @Test
    public void testSearchPeople() throws Exception {
        Query query = new Query(JHSearchField.PEOPLE, "Theodor Zwinger");
        SearchResult result = service.search(query, null);

        assertNotNull("Search result was NULL.", result);
        assertEquals("Unexpected number of results found.", 2, result.getTotal());
    }
    
    @Test
    public void testSearchMarginaliaXRef() throws Exception {
        Query query = new Query(JHSearchField.CROSS_REFERENCE, "Erasmus");
        SearchResult result = service.search(query, null);

        assertNotNull("Search result was NULL.", result);
        assertEquals("Unexpected number of results found.", 1, result.getTotal());
        assertTrue("Unexpected result ID found.", result.getMatches()[0].getId().contains("1v"));
    }
    
    @Test
    public void testSearchMarginaliaEmphasis() throws Exception {
        Query query = new Query(JHSearchField.EMPHASIS, "Bodini");
        SearchResult result = service.search(query, null);

        assertNotNull("Search result was NULL.", result);
        assertEquals("Unexpected number of results found.", 1, result.getTotal());
        assertTrue("Unexpected result ID found.", result.getMatches()[0].getId().contains("1r"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBlankQuery() throws Exception {
        service.search(new Query(), null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNoSearchTermQuery() throws Exception {
        service.search(new Query(QueryOperation.AND), null);
    }

    @Test
    public void testHandleRequestSymbol() throws Exception {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        PresentationRequest req = new PresentationRequest(PresentationRequestType.MANIFEST, "valid", "FolgersHa2");

        String query = "symbol:'Sun'";

        service.handle_request(req, query, 0, os);

        String result_json = os.toString("UTF-8");

        assertTrue(result_json.contains("\"total\":37"));
        assertTrue(result_json.contains("\"context\":"));
        assertTrue(result_json.contains("\"manifest\":"));
        assertTrue(result_json.contains("\"object\":"));
        assertFalse(result_json.contains("\"categories\":"));
    }
    
    // Searches within a book for a manifest, so only one result.
    @Test
    public void testHandleBrowseRepository() throws Exception {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        PresentationRequest req = new PresentationRequest(PresentationRequestType.MANIFEST, "valid", "FolgersHa2");

        String query = "object_type:'sc:Manifest'";

        service.handle_request(req, query, 0, 20, "index", "facet_repository:''", os);

        String result_json = os.toString("UTF-8");

        assertTrue(result_json.contains("\"total\":1"));
        assertTrue(result_json.contains("\"context\":"));
        assertTrue(result_json.contains("\"manifest\":"));
        assertTrue(result_json.contains("\"object\":"));
        assertTrue(result_json.contains("\"categories\":"));
    }
    
    
    @Test
    public void testHandleInfoRequest() throws Exception {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        PresentationRequest req = new PresentationRequest(PresentationRequestType.MANIFEST, "valid", "FolgersHa2");
        
        service.handle_info_request(req, os);

        String result_json = os.toString("UTF-8");

        assertTrue(result_json.contains("\"fields\":"));
        assertTrue(result_json.contains("\"categories\":"));
        assertTrue(result_json.contains("\"default-fields\":"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testUnknownField() throws Exception {
        service.search(new Query("unknown", "moo"), null);
    }

    /**
     * Search for a person's name that appears once in the Folgers book. Then search for
     * an alternate spelling of the name. Both should return a single result of the same
     * page.
     *
     * The result Strings will NOT be exactly the same. While the object/manifest will be
     * the same, the context will be different.
     *
     * TODO re-enable when there is correct handling of name variants in AOR
     *
     * @throws Exception
     */
    @Test
    @Ignore
    public void testPersonSpellingVariant() throws Exception {
        PresentationRequest req = new PresentationRequest(PresentationRequestType.MANIFEST, "valid", "FolgersHa2");        

        // Check this name: Baldo degli Ubaldi,Baldus,Baldus de Ubaldis,,
        // Alternate spelling
        {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            String query = "marginalia:'\"Baldus de Ubaldis\"'";

            service.handle_request(req, query, 0, out);

            String result = out.toString();
            
            assertNotNull(result);
            assertFalse("Result was empty.", result.isEmpty());
            assertTrue("Unexpected number of matches returned.", result.contains("\"total\":1"));
            assertTrue("Unexpected page label found.", result.contains("\"label\":\"25v\""));
            assertTrue("Unexpected page ID found.", result.contains(
                    "\"@id\":\"http://serenity.dkc.jhu.edu/pres/valid.FolgersHa2/canvas/25v\""
            ));
        }

        // Original spelling
        {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            String query = "marginalia:'\"Baldo degli Ubaldi\"'";

            service.handle_request(req, query, 0, out);

            String result = out.toString();
            assertNotNull(result);
            assertFalse("Result was empty.", result.isEmpty());
            assertTrue("Unexpected number of matches returned.", result.contains("\"total\":1"));
            assertTrue("Unexpected page label found.", result.contains("\"label\":\"25v\""));
            assertTrue("Unexpected page ID found.", result.contains(
                    "\"@id\":\"http://serenity.dkc.jhu.edu/pres/valid.FolgersHa2/canvas/25v\""
            ));
        }
    }

    /**
     * Search for a person's name that appears once in the Folgers book. Then search for
     * an alternate spelling of the name. The reference sheets containing spelling
     * variants is missing. While the alternate spelling search will not fail, it will
     * return zero results.
     *
     * @throws Exception
     */
    @Test
    @Ignore
    public void testSpellingVariantWithNullReference() throws Exception {
        BookCollection mockCollection = loadValidCollection();
        mockCollection.setBooksRef(null);
        mockCollection.setPeopleRef(null);
        mockCollection.setLocationsRef(null);

        // The Store will return a BookCollection that is missing all spelling reference sheets,
        // Everything else will be the same as the real Store.
        Store mockStore = mock(StoreImpl.class);
        when(mockStore.loadBookCollection(anyString(), anyListOf(String.class))).thenReturn(mockCollection);
        when(mockStore.loadBook(any(BookCollection.class), eq("FolgersHa2"), anyListOf(String.class)))
                .thenReturn(loadValidFolgersHa2());
        when(mockStore.loadBook(any(BookCollection.class), eq("LudwigXV7"), anyListOf(String.class)))
                .thenReturn(loadValidLudwigXV7());

        // Clear search index and re-index with mocked data
        service.clear();
        service.update(mockStore, mockCollection.getId());

        PresentationRequest req = new PresentationRequest(PresentationRequestType.MANIFEST, "valid", "FolgersHa2");

        // Check this name: Baldo degli Ubaldi,Baldus,Baldus de Ubaldis,,
        // Alternate spelling
        {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            String query = "marginalia:'\"Baldus de Ubaldis\"'";

            service.handle_request(req, query, 0, out);

            String result = out.toString();
            assertNotNull(result);
            assertFalse("Result was empty.", result.isEmpty());
            assertTrue("Unexpected number of matches returned.", result.contains("\"total\":0"));
        }

        // Original spelling
        {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            String query = "marginalia:'\"Baldo degli Ubaldi\"'";

            service.handle_request(req, query, 0, out);

            String result = out.toString();
            assertNotNull(result);
            assertFalse("Result was empty.", result.isEmpty());
            assertTrue("Unexpected number of matches returned.", result.contains("\"total\":1"));
            assertTrue("Unexpected page label found.", result.contains("\"label\":\"25v\""));
            assertTrue("Unexpected page ID found.", result.contains(
                    "\"@id\":\"http://serenity.dkc.jhu.edu/pres/valid.FolgersHa2/canvas/25v\""
            ));
        }

    }
    
    
    /**
     * Ensure that a specific location can be browsed. Only books in that location are 
     * returned and that correct category information is returned for that book.
     * 
     * @throws Exception
     */
    @Test
    public void testBrowseSpecificLocation() throws Exception {
        Query query = new Query(JHSearchField.OBJECT_TYPE, IIIFNames.SC_MANIFEST);
        
        SearchOptions opts = new SearchOptions();
        opts.setCategories(Arrays.asList(new QueryTerm(JHSearchCategory.LOCATION.getFieldName(),
                "Los Angeles")));
        
        SearchResult result = service.search(query, opts);

        assertNotNull("Search result was NULL", result);
        assertEquals("Unexpected number of results found.", 1, result.getTotal());
        
        
        assertEquals(1, result.getMatches().length);
        assertEquals("http://serenity.dkc.jhu.edu/pres/valid/LudwigXV7/manifest",
                result.getMatches()[0].getId());
        
        assertNotNull(result.getCategories());

        assertEquals(10, result.getCategories().size());
        
        result.getCategories().forEach(cat -> {
            assertEquals(1, cat.getValues().length);
            
            boolean foundfield = false;
            for (SearchCategory sc: JHSearchCategory.values()) {
                if (sc.getFieldName().equals(cat.getFieldName())) {
                    foundfield = true;
                    break;
                }
            }
            
            assertTrue(foundfield);
            assertNotNull(cat.getValues()[0].getValue());
            assertEquals(1, cat.getValues()[0].getCount());
        });
    }
    
    /**
     * Ensure that all the books with a location category value can be browsed.
     * 
     * @throws Exception
     */
    @Test
    public void testBrowseAnyLocation() throws Exception {
        Query query = new Query(JHSearchField.OBJECT_TYPE, IIIFNames.SC_MANIFEST);
        
        SearchOptions opts = new SearchOptions();
        opts.setCategories(Arrays.asList(new QueryTerm(JHSearchCategory.LOCATION.getFieldName(), "")));
        
        SearchResult result = service.search(query, opts);

        assertNotNull("Search result was NULL", result);
        assertEquals("Unexpected number of results found.", 2, result.getTotal());
        assertEquals(2, result.getMatches().length);        
        assertNotNull(result.getCategories());
        
        assertEquals(10, result.getCategories().size());
        result.getCategories().forEach(cat -> {
            if (cat.getFieldName().equals("facet_author")) {
                assertEquals(3, cat.getValues().length);
            } else if (cat.getFieldName().equals("facet_origin") || cat.getFieldName().equals("facet_type")) {
                assertEquals(1, cat.getValues().length);
            } else {
                assertEquals(2, cat.getValues().length);
            }
            
            boolean foundfield = false;
            for (SearchCategory sc: JHSearchCategory.values()) {
                if (sc.getFieldName().equals(cat.getFieldName())) {
                    foundfield = true;
                    break;
                }
            }
            
            assertTrue(foundfield);
            assertNotNull(cat.getValues()[0].getValue());
            assertEquals(1, cat.getValues()[0].getCount());
        });
    }
    
    /**
     * Ensure that all the books with an author category value can be browsed.
     * 
     * @throws Exception
     */
    @Test
    public void testBrowseAnyAuthor() throws Exception {
        Query query = new Query(JHSearchField.OBJECT_TYPE, IIIFNames.SC_MANIFEST);
        
        SearchOptions opts = new SearchOptions();
        opts.setCategories(Arrays.asList(new QueryTerm(JHSearchCategory.AUTHOR.getFieldName(), "")));
        
        SearchResult result = service.search(query, opts);

        assertNotNull("Search result was NULL", result);
        assertEquals("Unexpected number of results found.", 2, result.getTotal());
        assertEquals(2, result.getMatches().length);
        
        Set<String> expected_manifests = new HashSet<>();
        expected_manifests.add("http://serenity.dkc.jhu.edu/pres/valid/FolgersHa2/manifest");
        expected_manifests.add("http://serenity.dkc.jhu.edu/pres/valid/LudwigXV7/manifest");
        
        Set<String> manifests = new HashSet<>();
        
        for (SearchMatch m: result.getMatches()) {
            manifests.add(m.getId());
        }
        
        assertEquals(expected_manifests, manifests);
        
        assertNotNull(result.getCategories());
        assertEquals(10, result.getCategories().size());
        
        result.getCategories().forEach(cat -> {
            String name = cat.getFieldName();
            // Folgers Ha2 has two author facet values
            if (cat.getFieldName().equals(JHSearchCategory.AUTHOR.getFieldName())) {
                assertEquals(3, cat.getValues().length);
            } else if (name.equals(JHSearchCategory.ORIGIN.getFieldName()) || name.equals(JHSearchCategory.TYPE.getFieldName())) {
                assertEquals(1, cat.getValues().length);
            } else {
                assertEquals(2, cat.getValues().length);
            }
            
            boolean foundfield = false;
            for (SearchCategory sc: JHSearchCategory.values()) {
                if (sc.getFieldName().equals(cat.getFieldName())) {
                    foundfield = true;
                    break;
                }
            }
            
            assertTrue(foundfield);
            assertNotNull(cat.getValues()[0].getValue());
            assertEquals(1, cat.getValues()[0].getCount());
        });
    }
}
