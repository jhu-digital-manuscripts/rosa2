package rosa.iiif.presentation.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

import rosa.archive.core.ArchiveNameParser;
import rosa.archive.core.BaseArchiveTest;
import rosa.iiif.presentation.core.transform.impl.AnnotationListTransformer;
import rosa.iiif.presentation.core.transform.impl.CanvasTransformer;
import rosa.iiif.presentation.core.transform.impl.CollectionTransformer;
import rosa.iiif.presentation.core.transform.impl.JsonldSerializer;
import rosa.iiif.presentation.core.transform.impl.ManifestTransformer;
import rosa.iiif.presentation.core.transform.PresentationTransformer;
import rosa.iiif.presentation.core.transform.impl.RangeTransformer;
import rosa.iiif.presentation.core.transform.impl.SequenceTransformer;
import rosa.iiif.presentation.core.transform.Transformer;
import rosa.iiif.presentation.core.transform.impl.TransformerSet;
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
        ArchiveNameParser parser = new ArchiveNameParser();

        CollectionTransformer collectionTransformer = new CollectionTransformer(requestFormatter, parser);
        CanvasTransformer canvasTransformer = new CanvasTransformer(requestFormatter, imageFormatter, parser, imageIdMapper);
        SequenceTransformer sequenceTransformer = new SequenceTransformer(requestFormatter, parser, canvasTransformer);

        Set<Transformer<?>> transformers = new HashSet<>();
        transformers.add(new AnnotationListTransformer(requestFormatter, parser));
        transformers.add(canvasTransformer);
        transformers.add(sequenceTransformer);
        transformers.add(new ManifestTransformer(requestFormatter, parser, sequenceTransformer));
        transformers.add(new RangeTransformer(requestFormatter));

        TransformerSet transformerSet = new TransformerSet(transformers);

        PresentationTransformer transformer = new PresentationTransformer(requestFormatter, parser, transformerSet,
                collectionTransformer);
        AnnotationListTransformer annoListTransformer = new AnnotationListTransformer(requestFormatter, parser);

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
//        assertTrue(json.has("structures"));
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
//        assertTrue(json.has("structures"));
    }
}
