package rosa.iiif.presentation.endpoint;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.BeforeClass;
import org.junit.Test;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Guice;
import com.google.inject.Injector;

import rosa.archive.core.ArchiveCoreModule;
import rosa.archive.core.Store;
import rosa.iiif.image.core.IIIFRequestFormatter;
import rosa.iiif.presentation.core.IIIFPresentationRequestFormatter;
import rosa.iiif.presentation.core.PresentationUris;
import rosa.iiif.presentation.core.jhsearch.JHSearchService;

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
        pres_uris = new PresentationUris(injector.getInstance(IIIFPresentationRequestFormatter.class), injector.getInstance(IIIFRequestFormatter.class));
    }

    private void check_json_syntax(InputStream is) throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.enable(DeserializationFeature.FAIL_ON_READING_DUP_TREE_KEY);
        objectMapper.readTree(is);
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

    private void check_retrieve_search_inro(String url) throws Exception {
        HttpURLConnection con = (HttpURLConnection) (new URL(url)).openConnection();

        con.connect();
        int code = con.getResponseCode();
        assertEquals(200, code);

        try (InputStream is = con.getInputStream()) {
            check_search_info_syntax(is);
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
            check_retrieve_search_inro(pres_uris.getCollectionURI(col) + JHSearchService.INFO_RESOURCE_PATH);

            for (String book : store.listBooks(col)) {
                check_retrieve_search_inro(pres_uris.getManifestURI(col, book) + JHSearchService.INFO_RESOURCE_PATH);
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
    
    
}
