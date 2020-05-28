package rosa.iiif.presentation.endpoint;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.FileWriter;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashSet;
import java.util.Set;

import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Guice;
import com.google.inject.Injector;

import rosa.archive.core.ArchiveCoreModule;
import rosa.archive.core.Store;
import rosa.iiif.image.core.IIIFRequestFormatter;
import rosa.iiif.presentation.core.IIIFPresentationRequestFormatter;
import rosa.iiif.presentation.core.PresentationUris;
import rosa.iiif.presentation.core.StaticResourceRequestFormatter;
import rosa.iiif.presentation.core.jhsearch.JHSearchService;
import rosa.iiif.presentation.model.PresentationRequest;
import rosa.iiif.presentation.model.PresentationRequestType;
import rosa.search.model.SearchOptions;

/**
 * Check that the IIIF Presentation API implementation is working as expected.
 */
public class ITIIIFPresentationServlet {
    private static Store store;
    private static PresentationUris pres_uris;

    @BeforeClass
    public static void setup() {
        Injector injector = Guice.createInjector(new ArchiveCoreModule(), new IIIFPresentationServletITModule());

        store = injector.getInstance(Store.class);
        pres_uris = new PresentationUris(injector.getInstance(IIIFPresentationRequestFormatter.class),
                injector.getInstance(IIIFRequestFormatter.class), injector.getInstance(StaticResourceRequestFormatter.class));
    }

    private void check_json_syntax(InputStream is) throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.enable(DeserializationFeature.FAIL_ON_READING_DUP_TREE_KEY);
        JsonNode root = objectMapper.readTree(is);
        
        // In case this is JSON-LD ensure that there are no duplicate @id statements
        check_json_ld_id_dups(root);
    }

    private void check_retrieve_json(String url) throws Exception {
        HttpURLConnection con = (HttpURLConnection) (new URL(url)).openConnection();

        con.connect();
        int code = con.getResponseCode();
        assertEquals(200, code);

        try (InputStream is = con.getInputStream()) {
            check_json_syntax(is);
        }
    }

    private void check_retrieve_search_info(String url) throws Exception {
        HttpURLConnection con = (HttpURLConnection) (new URL(url)).openConnection();

        con.connect();
        int code = con.getResponseCode();
        assertEquals(200, code);

        try (InputStream is = con.getInputStream()) {
            check_search_info_syntax(is);
        }
    }

    // Check a JSON-LD document to see if any of the @ids are duplicated.
    private void check_json_ld_id_dups(JsonNode root) {
    	try {
    	FileWriter w = new FileWriter("/home/msp/blah");
    	w.append(root.toString());
    	w.close();
    	} catch (Exception e) {
    		throw new RuntimeException(e);
    	}
    	
    	check_json_ld_id_dups(root, new HashSet<String>(), "");
    }
    
    private void check_json_ld_id_dups(JsonNode node, Set<String> ids, String parent_name) {    	
    	if (node.isObject()) {
    		node.fields().forEachRemaining(e -> {    			
    			if (e.getKey().equals("@id")) {
    				String id = e.getValue().asText();
    				
    				assertFalse("Duplicate @id: " + id, ids.contains(id));
    	
    				// Ignore missing image which may occur several times.
    				if (!id.endsWith("missing;image/annotation") && !id.contains("missing_image")) {
    					ids.add(id);
    				}
    				// System.err.println("@id " + id);
    			} else if (e.getKey().equals("service")) {
    				// There will be duplicate image services from thumbnail and canvas
    			} else if (e.getKey().equals("thumbnail") && parent_name.equals("sequences")) {
    			    // Do not check duplicates in thumbnail of sequence because it will be present in a canvas
    			} else {
    				JsonNode val = e.getValue();
    				
    				if (val.isObject()) {
    					check_json_ld_id_dups(val, ids, e.getKey());
    				} else if (val.isArray()) {
    					val.elements().forEachRemaining(c -> {
    						check_json_ld_id_dups(c, ids, e.getKey());
    					});
    				}
    			}
    		});
    	}
    }

    private void check_search_info_syntax(InputStream is) throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.enable(DeserializationFeature.FAIL_ON_READING_DUP_TREE_KEY);
        JsonNode base = objectMapper.readTree(is);

        check_for_json_array("fields", base);
        check_for_json_array("categories", base);
    }

    // Check that a JSON field is an array and is not empty (has at least one element)
    private void check_for_json_array(String fieldName, JsonNode baseObj) {
        assertTrue(baseObj.hasNonNull(fieldName));
        assertTrue(baseObj.findValue(fieldName).isArray());
        assertNotNull(baseObj.findValue(fieldName).get(0));
    }

    /**
     * Test that each book and collection can be retrieved successfully through
     * the IIIF Presentation API.
     * 
     * @throws Exception
     */
    @Test
    public void testRetrieveCollectionsAndManifests() throws Exception {
        for (String col : store.listBookCollections()) {
            check_retrieve_json(pres_uris.getCollectionURI(col));

            for (String book : store.listBooks(col)) {
                check_retrieve_json(pres_uris.getManifestURI(col, book));
            }
        }
    }
    
    /**
     * Ensure that search info can be retrieved for each book and collection.
     * Make sure that the 'fields' and 'categories' properties of the search
     * info are not empty.
     * 
     * @throws Exception
     */
    @Test
    public void testSearchInfo() throws Exception {
        for (String col : store.listBookCollections()) {
            check_retrieve_search_info(pres_uris.getCollectionURI(col) + JHSearchService.INFO_RESOURCE_PATH);
            
            for (String book : store.listBooks(col)) {
                check_retrieve_search_info(pres_uris.getManifestURI(col, book) + JHSearchService.INFO_RESOURCE_PATH);
            }
        }
    }

    /**
     * Test that each collection and book can be searched.
     * 
     * @throws Exception
     */
    @Test
    public void testSearchCollections() throws Exception {
        for (String col : store.listBookCollections()) {
            check_retrieve_json(pres_uris.getCollectionURI(col) + JHSearchService.RESOURCE_PATH + "?q="
                    + URLEncoder.encode("object_id:'moo'", "UTF-8"));
            
            for (String book : store.listBooks(col)) {
                check_retrieve_json(pres_uris.getManifestURI(col, book) + JHSearchService.RESOURCE_PATH + "?q="
                        + URLEncoder.encode("object_id:'moo'", "UTF-8"));
            }
        }
    }

    /**
     * Test that a known search returns results. Search for the word "one" in the TEXT field.
     * This _should_ be a search across all collections that returns at least one result
     *
     * @throws Exception .
     */
    @Ignore
    @Test
    public void testMoo() throws Exception {
        for (String col : store.listBookCollections()) {
            PresentationRequest req = new PresentationRequest(PresentationRequestType.COLLECTION, col);
            String search = pres_uris.getJHSearchURI(req,
                    "text:'one'", new SearchOptions(0, 30), null);
            HttpURLConnection con = (HttpURLConnection) (new URL(search)).openConnection();
            // System.out.println(" ### " + search);
            con.connect();
            int code = con.getResponseCode();
            assertEquals(200, code);

            try (InputStream is = con.getInputStream()) {
                ObjectMapper objectMapper = new ObjectMapper();
                objectMapper.enable(DeserializationFeature.FAIL_ON_READING_DUP_TREE_KEY);
                JsonNode base = objectMapper.readTree(is);

                JsonNode matches = base.findValue("matches");
                assertNotNull(matches);
                assertTrue(matches.isArray());
                assertTrue(matches.has(0));
            }
        }
    }

//    /**
////     * Check any object's "related" property. If it exists, see if the URI is resolvable.
////     *
////     * @throws Exception
////     */
//    @Test
//    public void testResolveRelatedUris() throws Exception {
//        for (String col : store.listBookCollections()) {
////            test_related_uris(pres_uris.getCollectionURI(col));
//            for (String book : store.listBooks(col)) {
//                test_related_uris(pres_uris.getManifestURI(col, book));
//            }
//        }
//    }
//
//    private void test_related_uris(String uri) throws Exception {
//        HttpURLConnection con = (HttpURLConnection) (new URL(uri)).openConnection();
//
//        con.connect();
//        int code = con.getResponseCode();
//        assertEquals(200, code);
//
//        try (InputStream is = con.getInputStream()) {
//            ObjectMapper objectMapper = new ObjectMapper();
//            objectMapper.enable(DeserializationFeature.FAIL_ON_READING_DUP_TREE_KEY);
//            JsonNode base = objectMapper.readTree(is);
//
//            if (base.has("related")) {
//                String resourceUri = base.get("related").get("@id").textValue();
//                assertNotNull("A 'related' resource was found that had no access URI", resourceUri);
//
//                resolve_resource_uri(resourceUri);
//            }
//        }
//    }
//
//    private void resolve_resource_uri(String uri) throws Exception {
//        HttpURLConnection con = (HttpURLConnection) (new URL(uri)).openConnection();
//        System.out.println(" Trying >> " + uri);
//        con.connect();
//        int code = con.getResponseCode();
//        assertEquals(200, code);
//
//    }
    
}
