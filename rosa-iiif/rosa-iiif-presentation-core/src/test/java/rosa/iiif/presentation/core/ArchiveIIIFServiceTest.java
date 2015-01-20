package rosa.iiif.presentation.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;

import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

import rosa.archive.core.BaseArchiveTest;
import rosa.iiif.presentation.core.transform.JsonldSerializer;
import rosa.iiif.presentation.core.transform.PresentationTransformer;
import rosa.iiif.presentation.model.PresentationRequest;
import rosa.iiif.presentation.model.PresentationRequestType;

/**
 * Evaluate service against test data from rosa-archive-core.
 */
public class ArchiveIIIFServiceTest extends BaseArchiveTest {
    private ArchiveIIIFService service;

    @Before
    public void setupArchiveStore() throws URISyntaxException, IOException {
        super.setupArchiveStore();
        
        JsonldSerializer serializer = new JsonldSerializer();

        String scheme = "http";
        String host = "serenity.dkc.jhu.edu";
        int port = 80;
        String pres_prefix = "/pres";
        String image_prefix = "/image";

        IIIFRequestFormatter requestFormatter = new IIIFRequestFormatter(scheme, host, pres_prefix, port);
        rosa.iiif.image.core.IIIFRequestFormatter imageFormatter = new rosa.iiif.image.core.IIIFRequestFormatter(
                scheme, host, port, image_prefix);
        ImageIdMapper imageIdMapper = new JhuFsiImageIdMapper(new HashMap<String, String>());

        PresentationTransformer transformer = new PresentationTransformer(requestFormatter, imageFormatter,
                imageIdMapper);

        service = new ArchiveIIIFService(store, serializer, transformer, 1000);
    }
    
    // TODO More extensive testing
    
    @Test
    public void testLudwigXV7ManifestRequest() throws IOException {
        String id = VALID_COLLECTION + "." + VALID_BOOK_LUDWIGXV7;
        PresentationRequest req = new PresentationRequest(id, null, PresentationRequestType.MANIFEST);
        
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        assertTrue(service.handle_request(req, os));
     
        String result = new String(os.toByteArray(), "UTF-8");
        
        JSONObject json = new JSONObject(result);
        
        assertEquals("http://iiif.io/api/presentation/2/context.json", json.get("@context"));
        assertTrue(json.has("sequences"));
        assertTrue(json.has("structures"));
    }
    
    @Test
    public void testFolgersHa2ManifestRequest() throws IOException {
        String id = VALID_COLLECTION + "." + VALID_BOOK_FOLGERSHA2;
        PresentationRequest req = new PresentationRequest(id, null, PresentationRequestType.MANIFEST);
        
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        assertTrue(service.handle_request(req, os));
     
        String result = new String(os.toByteArray(), "UTF-8");
        
        JSONObject json = new JSONObject(result);
        
        assertEquals("http://iiif.io/api/presentation/2/context.json", json.get("@context"));        
        assertTrue(json.has("sequences"));
        assertTrue(json.has("structures"));
    }
}
