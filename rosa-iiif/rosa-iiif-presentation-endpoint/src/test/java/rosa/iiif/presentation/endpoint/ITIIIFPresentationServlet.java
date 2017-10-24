package rosa.iiif.presentation.endpoint;

import static org.junit.Assert.assertEquals;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import org.junit.BeforeClass;
import org.junit.Test;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Guice;
import com.google.inject.Injector;

import rosa.archive.core.ArchiveCoreModule;
import rosa.archive.core.Store;
import rosa.iiif.presentation.core.IIIFPresentationRequestFormatter;
import rosa.iiif.presentation.core.PresentationUris;


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
        pres_uris = new PresentationUris(injector.getInstance(IIIFPresentationRequestFormatter.class));
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
    
    @Test
    public void testRetrieveCollectionsAndManifests() throws Exception {
        for (String col: store.listBookCollections()) {
            System.out.println("Checking: " + pres_uris.getCollectionURI(col));
            
            check_retrieve_json(pres_uris.getCollectionURI(col));
            
            for (String book: store.listBooks(col)) {
                System.out.println("Checking: " + pres_uris.getManifestURI(col, book));
                
                check_retrieve_json(pres_uris.getManifestURI(col, book));
            }
        }
    }
}
