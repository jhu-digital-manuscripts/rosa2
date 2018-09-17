package rosa.iiif.presentation.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.json.JSONObject;
import org.junit.BeforeClass;
import org.junit.Test;

import rosa.archive.core.ArchiveNameParser;
import rosa.archive.core.BaseSearchTest;
import rosa.iiif.presentation.core.transform.PresentationTransformer;
import rosa.iiif.presentation.core.transform.impl.JsonldSerializer;
import rosa.iiif.presentation.core.transform.impl.PresentationTransformerImpl;
import rosa.iiif.presentation.model.PresentationRequest;
import rosa.iiif.presentation.model.PresentationRequestType;

/**
 * Evaluate service against test data from rosa-archive-core.
 */
public class ArchiveIIIFPresentationServiceTest extends BaseSearchTest {
    private static ArchiveIIIFPresentationService service;

    @BeforeClass
    public static void setup() throws Exception {
        JsonldSerializer serializer = new JsonldSerializer();

        String scheme = "http";
        String host = "serenity.dkc.jhu.edu";
        int port = 80;
        String pres_prefix = "/pres";
        String image_prefix = "/image";

        IIIFPresentationRequestFormatter requestFormatter = new IIIFPresentationRequestFormatter(scheme, host, pres_prefix, port);

        rosa.iiif.image.core.IIIFRequestFormatter imageFormatter = new rosa.iiif.image.core.IIIFRequestFormatter(
                scheme, host, port, image_prefix);
        ArchiveNameParser nameParser = new ArchiveNameParser();
        
        IIIFPresentationCache cache = new IIIFPresentationCache(store, 10);
        PresentationUris pres_uris = new PresentationUris(requestFormatter, imageFormatter);

        PresentationTransformer transformer = new PresentationTransformerImpl(cache, pres_uris, nameParser);

        service = new ArchiveIIIFPresentationService(cache, serializer, transformer);
    }
    
    // TODO More extensive testing
    
    @Test
    public void testLudwigXV7ManifestRequest() throws IOException {
        PresentationRequest req = new PresentationRequest(PresentationRequestType.MANIFEST, VALID_COLLECTION, VALID_BOOK_LUDWIGXV7);
        
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        assertTrue(service.handle_request(req, os));
     
        String result = new String(os.toByteArray(), "UTF-8");
        
        JSONObject json = new JSONObject(result);
        
        assertEquals("http://iiif.io/api/presentation/2/context.json", json.get("@context"));
        assertTrue(json.has("sequences"));
        assertTrue(result.contains("otherContent"));
//        assertTrue(json.has("structures"));
    }
    
    
    @Test
    public void testUnknownCollectionRequest() throws IOException {
        PresentationRequest req = new PresentationRequest(PresentationRequestType.MANIFEST, "foo", VALID_BOOK_LUDWIGXV7);
        
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        assertFalse(service.handle_request(req, os));
    }
    
    @Test
    public void testUnknownManifestRequest() throws IOException {
        PresentationRequest req = new PresentationRequest(PresentationRequestType.MANIFEST, VALID_COLLECTION, "foo");
        
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        assertFalse(service.handle_request(req, os));
    }
    
    @Test
    public void testFolgersHa2ManifestRequest() throws IOException {
        PresentationRequest req = new PresentationRequest(PresentationRequestType.MANIFEST, VALID_COLLECTION, VALID_BOOK_FOLGERSHA2);
        
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        assertTrue(service.handle_request(req, os));
     
        String result = new String(os.toByteArray(), "UTF-8");
        
        JSONObject json = new JSONObject(result);
        
        assertEquals("http://iiif.io/api/presentation/2/context.json", json.get("@context"));        
        assertTrue(json.has("sequences"));
//        assertTrue(json.has("structures"));
    }
}
