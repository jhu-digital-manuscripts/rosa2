package rosa.iiif.presentation.core.transform;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;

import rosa.archive.core.ArchiveNameParser;
import rosa.archive.core.BaseArchiveTest;
import rosa.iiif.presentation.core.IIIFPresentationRequestFormatter;
import rosa.iiif.presentation.core.ImageIdMapper;
import rosa.iiif.presentation.core.JhuFSIImageIdMapper;
import rosa.iiif.presentation.core.transform.impl.AnnotationListTransformer;
import rosa.iiif.presentation.core.transform.impl.AnnotationTransformer;
import rosa.iiif.presentation.core.transform.impl.CanvasTransformer;
import rosa.iiif.presentation.core.transform.impl.CollectionTransformer;
import rosa.iiif.presentation.core.transform.impl.LayerTransformer;
import rosa.iiif.presentation.core.transform.impl.ManifestTransformer;
import rosa.iiif.presentation.core.transform.impl.PresentationTransformerImpl;
import rosa.iiif.presentation.core.transform.impl.RangeTransformer;
import rosa.iiif.presentation.core.transform.impl.SequenceTransformer;
import rosa.iiif.presentation.core.transform.impl.TransformerSet;
import rosa.iiif.presentation.model.AnnotationList;
import rosa.iiif.presentation.model.Canvas;
import rosa.iiif.presentation.model.Collection;
import rosa.iiif.presentation.model.IIIFNames;
import rosa.iiif.presentation.model.Layer;
import rosa.iiif.presentation.model.Manifest;
import rosa.iiif.presentation.model.Reference;
import rosa.iiif.presentation.model.Sequence;
import rosa.iiif.presentation.model.TextValue;
import rosa.iiif.presentation.model.annotation.Annotation;

public class PresentationTransformerTest extends BaseArchiveTest {
    private static final String ENDPOINT_SCHEME = "http";
    private static final String ENDPOINT_HOST = "example.org";
    private static final String ENDPOINT_PREFIX = "/iiif";
    private static final int ENDPOINT_PORT = -1;


    private PresentationTransformerImpl presentationTransformer;

    /**
     * Set up a new PresentationTransformer with each test.
     */
    @Before
    public void setup() {
        Map<String, String> idMap = new HashMap<>();
        idMap.put(VALID_COLLECTION, "valid");

        IIIFPresentationRequestFormatter presentationReqFormatter =
                new IIIFPresentationRequestFormatter(ENDPOINT_SCHEME, ENDPOINT_HOST, ENDPOINT_PREFIX, ENDPOINT_PORT);
        rosa.iiif.image.core.IIIFRequestFormatter imageReqFormatter =
                new rosa.iiif.image.core.IIIFRequestFormatter(ENDPOINT_SCHEME, ENDPOINT_HOST, ENDPOINT_PORT, ENDPOINT_PREFIX);
        ImageIdMapper idMapper = new JhuFSIImageIdMapper(idMap);

        CanvasTransformer canvasTransformer = new CanvasTransformer(presentationReqFormatter, imageReqFormatter, idMapper);
        CollectionTransformer collectionTransformer = new CollectionTransformer(presentationReqFormatter, simpleStore, imageReqFormatter, idMapper);
        SequenceTransformer sequenceTransformer = new SequenceTransformer(presentationReqFormatter, canvasTransformer);
        AnnotationTransformer annotationTransformer = new AnnotationTransformer(presentationReqFormatter, new ArchiveNameParser());

        Set<Transformer<?>> transformers = new HashSet<>();
        transformers.add(new AnnotationListTransformer(presentationReqFormatter, annotationTransformer));
        transformers.add(canvasTransformer);
        transformers.add(sequenceTransformer);
        transformers.add(new ManifestTransformer(presentationReqFormatter, sequenceTransformer, new RangeTransformer(presentationReqFormatter)));
        transformers.add(new RangeTransformer(presentationReqFormatter));
        transformers.add(new LayerTransformer(presentationReqFormatter));

        TransformerSet transformerSet = new TransformerSet(transformers);

        presentationTransformer = new PresentationTransformerImpl(presentationReqFormatter, transformerSet, collectionTransformer);
    }

    @Test
    public void collectionTest() throws IOException {
        Collection col = presentationTransformer.collection(loadValidCollection());

        assertNotNull(col);

        assertNotNull(col.getDescription("en"));
        assertFalse(col.getDescription("en").isEmpty());

        assertNotNull(col.getRights());
        assertTrue(col.getRights().hasOneLogo());
        assertNotNull(col.getRights().getFirstLogo());
        assertFalse(col.getRights().getFirstLogo().isEmpty());
    }

    /**
     * Generate sequence from LudwigXV7 and check validity.
     * @throws IOException if collection and/or book not found
     */
    @Test
    public void sequenceLudwigXV7Test() throws IOException {
        checkSequence(presentationTransformer.sequence(loadValidCollection(), loadValidLudwigXV7(), "reading-order"));
    }

    /**
     * Generate canvas from LudwigXV7 and check validity.
     * @throws IOException if collection or book is not found
     */
    @Test
    public void canvasLudwigXV7Test() throws IOException {
        checkACanvas(presentationTransformer.canvas(loadValidCollection(), loadValidLudwigXV7(), "1v"));
    }

    /**
     * Generate canvases from FolgersHa2 and check validity. Ensure that
     * references to annotation lists are OK.
     * @throws IOException
     */
    @Test
    public void canvasFolgersHa2Test() throws IOException {
        Canvas c1 = presentationTransformer.canvas(loadValidCollection(), loadValidFolgersHa2(), "1r");
        Canvas c2 = presentationTransformer.canvas(loadValidCollection(), loadValidFolgersHa2(), "11r");

        checkACanvas(c1);
        checkACanvas(c2);

        assertNotNull(c1.getOtherContent());
        assertNotNull(c2.getOtherContent());
        assertEquals(1, c1.getOtherContent().size());
        assertEquals(1, c2.getOtherContent().size());

        Reference r1 = c1.getOtherContent().get(0);
        Reference r2 = c2.getOtherContent().get(0);

        assertNotNull(r1);
        assertNotNull(r2);

        assertNotEquals(r1.getReference(), r2.getReference());
    }

    /**
     * Generate a manifest for LudwigXV7 and check validity.
     * @throws IOException if collection or book is not found
     */
    @Test
    public void manifestLudwigXV7Test() throws IOException {
        Manifest manifest = presentationTransformer.manifest(loadValidCollection(), loadValidLudwigXV7());
        checkId(manifest.getId());
        assertNotNull("Metadata for manifest is missing.", manifest.getMetadata());
        assertFalse("Metadata for manifest is empty.", manifest.getMetadata().isEmpty());
        checkTextValue(manifest.getLabel());

        assertNotNull("Default sequence is missing.", manifest.getDefaultSequence());
        assertNotNull("", manifest.getOtherSequences());
        assertTrue("Unexpected other sequences found.", manifest.getOtherSequences().isEmpty());
        checkSequence(manifest.getDefaultSequence());
    }

    /**
     * Generate annotation list from LudwigXV7 and check validity.
     * @throws IOException if collection or book is not found
     */
    @Test
    public void annotationListLudwigXV7Test() throws IOException {
        checkAnnotationList(presentationTransformer.annotationList(
                loadValidCollection(), loadValidLudwigXV7(), "2r.all"));
    }

    /**
     * Generate annotation lists from FolgersHa2 and check validity.
     * @throws IOException if collection or book is not found.
     */
    @Test
    public void annotationListFolgersHa2Test() throws IOException {
        checkAnnotationList(presentationTransformer.annotationList(
                loadValidCollection(), loadValidFolgersHa2(), "1r.all"));
        checkAnnotationList(presentationTransformer.annotationList(
                loadValidCollection(), loadValidFolgersHa2(), "1r.symbol"));
        checkAnnotationList(presentationTransformer.annotationList(
                loadValidCollection(), loadValidFolgersHa2(), "1r.marginalia"));
        checkAnnotationList(presentationTransformer.annotationList(
                loadValidCollection(), loadValidFolgersHa2(), "1r.underline"));

        // Check for name-matching problem
        AnnotationList l1 = presentationTransformer.annotationList(loadValidCollection(), loadValidFolgersHa2(), "1r.all");
        AnnotationList l2 = presentationTransformer.annotationList(loadValidCollection(), loadValidFolgersHa2(), "11r.all");

        assertNotNull("Annotation list missing for 1r.all", l1);
        assertNotNull("Annotation list missing for 11r.all", l2);
        assertNotEquals("Annotation lists for 1r.all and 11r.all should NOT be equal.", l1, l2);
        assertNotEquals("Annotation lists for 1r.all and 11r.all should have different sizes", l1.size(), l2.size());

        assertEquals("Unexpected number of annotations in 1r.all", 9, l1.size());
        assertEquals("Unexpected number of annotations in 11r.all", 12, l2.size());

        {
            AnnotationList ll = presentationTransformer.annotationList(loadValidCollection(), loadValidFolgersHa2(), "front matter 1r.all");
            assertNotNull("Failed to create AnnotationList for front matter 1r", ll);
            assertTrue("Unexpected annotations found in annotation list for 'front matter 1r'", ll.getAnnotations().isEmpty());
        }

        {
            AnnotationList ll = presentationTransformer.annotationList(
                    loadValidCollection(), loadValidFolgersHa2(), "1r.all");
            assertNotNull("Failed to create AnnotationList for '1r'", ll);

            int marg_count = 0;
            for (int i = 0; i < ll.size(); i++) {
                Annotation a = ll.getAnnotations().get(i);
                if (a.getId().contains("marginalia")) {
                    marg_count++;
                }
            }
            assertEquals("Unexpected number of marginalia found.", 8, marg_count);

        }
    }

    /**
     * Generate layers from FolgersHa2 and check validity.
     * @throws IOException if collection or book is not found
     */
    @Test
    public void layerFolgersHa2Test() throws IOException {
        checkLayer(presentationTransformer.layer(loadValidCollection(), loadValidFolgersHa2(), "all"));
        checkLayer(presentationTransformer.layer(loadValidCollection(), loadValidFolgersHa2(), "underline"));
        checkLayer(presentationTransformer.layer(loadValidCollection(), loadValidFolgersHa2(), "marginalia"));
    }

    private void checkSequence(Sequence seq) {
        checkId(seq.getId());
        assertNotNull("List of canvases is missing from sequence.", seq.getCanvases());
        assertFalse("List of canvases is empty.", seq.getCanvases().isEmpty());

        checkTextValue(seq.getLabel());

        assertTrue("Start canvas is less than 0.", seq.getStartCanvas() >= 0);
        checkACanvas(seq.getCanvases().get(seq.getStartCanvas()));
    }

    private void checkACanvas(Canvas canvas) {
        checkId(canvas.getId());

        checkTextValue(canvas.getLabel());
        assertEquals("Unexpected object type found.", IIIFNames.SC_CANVAS, canvas.getType());

        assertTrue("Canvas must have a width > 0", canvas.getWidth() > 0);
        assertTrue("Canvas must have a height > 0", canvas.getHeight() > 0);

        assertNotNull("List of images on canvas is missing.", canvas.getImages());
        assertFalse("List of images is empty.", canvas.getImages().isEmpty());

        if (canvas.getOtherContent() != null) {
            for (Reference ref : canvas.getOtherContent()) {
                checkReference(ref);
            }
        }
    }

    private void checkAnnotationList(AnnotationList list) {
        assertNotNull("Annotation list does not exist.", list);

        checkId(list.getId());
        checkTextValue(list.getLabel());
        assertEquals("Unexpected object type found.", IIIFNames.SC_ANNOTATION_LIST, list.getType());
        assertNotNull("'within' property missing.", list.getWithin());
        assertFalse("'within' property empty.", list.getWithin() == null && list.getWithin().size() == 0);

        for (Annotation ann : list) {
            checkAnnotation(ann);
        }
    }

    private void checkLayer(Layer layer) {
        assertNotNull("Layer does not exist.", layer);

        checkId(layer.getId());
        checkTextValue(layer.getLabel());
        assertNotNull("Layer 'otherContents' is missing.", layer.getOtherContent());
        assertFalse("Layer 'otherContent' is empty.", layer.getOtherContent().isEmpty());

        for (String content : layer.getOtherContent()) {
            checkId(content);
        }
    }

    private void checkAnnotation(Annotation annotation) {
        checkId(annotation.getId());
        assertEquals("Unexpected object type found.", IIIFNames.OA_ANNOTATION, annotation.getType());
        assertNotNull("No default source found.", annotation.getDefaultSource());
        assertNotNull("No default target found.", annotation.getDefaultTarget());
    }

    private void checkReference(Reference ref) {
        checkTextValue(ref.getLabel());
        assertNotNull("Reference type is missing.", ref.getType());
        assertFalse("Reference type is empty.", ref.getType().isEmpty());
        assertNotNull("Reference uri is missing.", ref.getReference());
        assertFalse("Reference uri is empty.", ref.getReference().isEmpty());
    }

    private void checkTextValue(TextValue value) {
        assertNotNull("Text value is missing.", value);
        assertFalse("Text is empty.", value.getValue().isEmpty());
    }

//    @Test
//    public void locationOnCanvasTest() {
//        for (int i = 0; i < 10; i++) {
//            BookImage c = new BookImage();
//            c.setId("Canvas" + i);
//            c.setWidth(1000);
//            c.setHeight(1500);
//
//            checkTarget(annotationListTransformer.locationOnCanvas(c, Location.RIGHT_MARGIN), false);
//            checkTarget(annotationListTransformer.locationOnCanvas(c, Location.FULL_PAGE), true);
//
//            AnnotationTarget t = annotationListTransformer.locationOnCanvas(c, Location.HEAD);
//            checkTarget(t, false);
//
//            String selectorContent = t.getSelector().content();
//
//            String[] parts = selectorContent.split(",");
//            assertEquals(4, parts.length);
//            assertEquals("0", parts[0]);
//            assertEquals("0", parts[1]);
//            assertEquals("1000", parts[2]);
//            // x, y, width are always known. Height depends on the guessing factor
//        }
//    }

//    private void checkTarget(AnnotationTarget target, boolean isFullPage) {
//        assertNotNull(target);
//        if (isFullPage) {
//            assertFalse(target.isSpecificResource());
//            assertNull(target.getSelector());
//        } else {
//            assertTrue(target.isSpecificResource());
//
//            Selector s = target.getSelector();
//            assertNotNull(s);
//            assertTrue(s instanceof FragmentSelector);
//        }
//    }

    /**
     * Make sure this ID is present, and starts with a well constructed IIIF formatted
     * URL prefix
     *
     * @param id ID of a IIIF presentation object
     */
    private void checkId(String id) {
        assertNotNull("ID is missing.", id);
        assertTrue("ID has bad format.",
                id.startsWith(ENDPOINT_SCHEME + "://" + ENDPOINT_HOST + ENDPOINT_PREFIX + "/"));
    }
}
