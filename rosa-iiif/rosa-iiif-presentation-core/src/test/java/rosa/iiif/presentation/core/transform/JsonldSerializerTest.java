package rosa.iiif.presentation.core.transform;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;

import rosa.iiif.presentation.model.Canvas;
import rosa.iiif.presentation.model.Collection;
import rosa.iiif.presentation.model.HtmlValue;
import rosa.iiif.presentation.model.IIIFImageService;
import rosa.iiif.presentation.model.IIIFNames;
import rosa.iiif.presentation.model.Manifest;
import rosa.iiif.presentation.model.PresentationBase;
import rosa.iiif.presentation.model.Reference;
import rosa.iiif.presentation.model.Sequence;
import rosa.iiif.presentation.model.Service;
import rosa.iiif.presentation.model.TextValue;
import rosa.iiif.presentation.model.ViewingDirection;
import rosa.iiif.presentation.model.ViewingHint;
import rosa.iiif.presentation.model.annotation.Annotation;
import rosa.iiif.presentation.model.annotation.AnnotationSource;
import rosa.iiif.presentation.model.annotation.AnnotationTarget;

public class JsonldSerializerTest {
    private static final String IIIF_CONTEXT_URL = "http://iiif.io/api/presentation/2/context.json";

    private JsonldSerializer serializer;

    @Before
    public void setup() {
        serializer = new JsonldSerializer();
    }

    @Test
    public void goodOutputWithCollection() throws Exception {
        Collection collection = createCollectionOfManifests();

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        serializer.write(collection, out);

        String json = out.toString();
        assertNotNull(json);
        assertFalse(json.isEmpty());

//        System.out.println(json);
    }

    @Test
    public void goodOutputWithCompleteManifest() throws Exception {
        Manifest manifest = createManifest();

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        serializer.write(manifest, out);

        String json = out.toString();
        assertNotNull("JSON-LD string does not exist.", json);
        assertFalse("JSON-LD string is empty.", json.isEmpty());

        checkForIIIFContextAppearances(json, 1);
    }

    @Test
    public void goodOutputWithOnlySequence() throws Exception {
        Sequence sequence = createSequence(1);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        serializer.write(sequence, out);

        String json = out.toString();
        assertNotNull(json);
        assertFalse(json.isEmpty());

        checkForIIIFContextAppearances(json, 1);
    }

    @Test
    public void goodOutputWithOnlyCanvas() throws Exception {
        Canvas canvas = createCanvasesWithOneImage().get(0);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        serializer.write(canvas, out);

        String json = out.toString();
        assertNotNull(json);
        assertFalse(json.isEmpty());

        checkForIIIFContextAppearances(json, 1);
    }

    private void checkForIIIFContextAppearances(String toTest, int expectedAppearances) {
        int index = toTest.indexOf(IIIF_CONTEXT_URL);
        int count = 0;
        while (index != -1) {
            index = toTest.indexOf(IIIF_CONTEXT_URL, index + 1);
            count++;
        }
        assertEquals("IIIF context appears more than once in the JSON-LD object!",
                expectedAppearances, count);
    }

    private Collection createCollectionOfManifests() {
        Collection collection = new Collection();

        collection.setId("CollectionId");
        collection.setType("sc:Collection");
        collection.setLabel("Collection label", "en");
        collection.getManifests().add(createReference(IIIFNames.SC_MANIFEST));
        collection.getManifests().add(createReference(IIIFNames.SC_MANIFEST));

        return collection;
    }

    private Reference createReference(String type) {
        Reference ref = new Reference();
        
        ref.setReference(UUID.randomUUID().toString());
        ref.setLabel(new TextValue("moo", "cow"));
        ref.setType(type);
        
        return ref;
    }

    private Manifest createManifest() {
        Manifest manifest = new Manifest();

        manifest.setViewingDirection(ViewingDirection.LEFT_TO_RIGHT);
        setBaseData(manifest);
        for (int i = 0; i < 5; i++) {
            manifest.getSequences().add(createSequence(i));
        }

        return manifest;
    }

    private Sequence createSequence(int index) {
        Sequence sequence = new Sequence();
        sequence.setId("SequenceId");
        sequence.setLabel("Sequence " + index, "en");
        sequence.setCanvases(createCanvasesWithOneImage());
        sequence.setStartCanvas(2);

        return sequence;
    }

    private List<Canvas> createCanvasesWithOneImage() {
        List<Canvas> canvases = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            Canvas c = new Canvas();
            c.setId("CanvasId" + i);
            c.setLabel("Canvas_" + i, "en");

            Annotation image = new Annotation();
            image.setId("Annotation" + i);
            image.setWidth(250);
            image.setHeight(200);
            image.setType("oa:Annotation");

            image.setDefaultSource(new AnnotationSource(
                    "IMAGE_URL", "dcterms:Image", "image/jpeg"
            ));
            image.setDefaultTarget(new AnnotationTarget("CanvasId" + i));

            c.getImages().add(image);
            canvases.add(c);
        }
        return canvases;
    }

    private <T extends PresentationBase> void setBaseData(T obj) {
        obj.setId("ObjectId");
        obj.setType("TYPE");
        obj.setViewingHint(ViewingHint.PAGED);
        obj.setLabel("Label", "en");
        obj.setDescription("This is a description. It can have minimal HTML markup.", "en");
        obj.setThumbnailUrl("THUMBNAIL_URL");
        obj.setThumbnailService(createIiifService());
        obj.setAttribution(new HtmlValue("This is the attribution."));
        obj.setLicense("URL_TO_LICENSE");
        obj.setLogo("URL_TO_LICENSE_LOGO");
        obj.setSeeAlso("Do not see also.");
        obj.setService(null);
        obj.setRelatedUri("URL_TO_RELATED_RESOURCE");
        obj.setRelatedFormat("text/plain");
        obj.setWithin("ParentId");
        obj.setMetadata(createMetadata());
    }

    private Map<String, HtmlValue> createMetadata() {
        Map<String, HtmlValue> map = new HashMap<>();

        map.put("key", new HtmlValue("value"));
        map.put("another_key", new HtmlValue("Another value"));

        return map;
    }

    private Service createIiifService() {
        return new IIIFImageService(
                "CONTEXT_URL", "serviceID", "Profile", 250, 200, -1, -1, new int[] {}
        );
    }

}
