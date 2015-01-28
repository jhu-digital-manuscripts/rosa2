package rosa.iiif.presentation.core.transform;

import org.junit.Before;
import org.junit.Test;
import rosa.archive.core.ArchiveNameParser;
import rosa.archive.core.BaseArchiveTest;
import rosa.iiif.presentation.core.IIIFRequestFormatter;
import rosa.iiif.presentation.core.ImageIdMapper;
import rosa.iiif.presentation.core.JhuFsiImageIdMapper;
import rosa.iiif.presentation.core.transform.impl.AnnotationListTransformer;
import rosa.iiif.presentation.core.transform.impl.CanvasTransformer;
import rosa.iiif.presentation.core.transform.impl.CollectionTransformer;
import rosa.iiif.presentation.core.transform.impl.ManifestTransformer;
import rosa.iiif.presentation.core.transform.impl.PresentationTransformerImpl;
import rosa.iiif.presentation.core.transform.impl.RangeTransformer;
import rosa.iiif.presentation.core.transform.impl.SequenceTransformer;
import rosa.iiif.presentation.core.transform.impl.TransformerSet;
import rosa.iiif.presentation.model.AnnotationList;
import rosa.iiif.presentation.model.Canvas;
import rosa.iiif.presentation.model.IIIFNames;
import rosa.iiif.presentation.model.Manifest;
import rosa.iiif.presentation.model.Reference;
import rosa.iiif.presentation.model.Sequence;
import rosa.iiif.presentation.model.TextValue;
import rosa.iiif.presentation.model.annotation.Annotation;
import rosa.iiif.presentation.model.annotation.AnnotationTarget;
import rosa.iiif.presentation.model.selector.FragmentSelector;
import rosa.iiif.presentation.model.selector.Selector;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class PresentationTransformerTest extends BaseArchiveTest {
    private static final String ENDPOINT_SCHEME = "http";
    private static final String ENDPOINT_HOST = "example.org";
    private static final String ENDPOINT_PREFIX = "/iiif";
    private static final int ENDPOINT_PORT = -1;


    private PresentationTransformerImpl presentationTransformer;

    @Before
    public void setup() {
        Map<String, String> idMap = new HashMap<>();
        idMap.put(VALID_COLLECTION, "valid");

        IIIFRequestFormatter presentationReqFormatter =
                new IIIFRequestFormatter(ENDPOINT_SCHEME, ENDPOINT_HOST, ENDPOINT_PREFIX, ENDPOINT_PORT);
        rosa.iiif.image.core.IIIFRequestFormatter imageReqFormatter =
                new rosa.iiif.image.core.IIIFRequestFormatter(ENDPOINT_SCHEME, ENDPOINT_HOST, ENDPOINT_PORT, ENDPOINT_PREFIX);
        ImageIdMapper idMapper = new JhuFsiImageIdMapper(idMap);
        ArchiveNameParser parser = new ArchiveNameParser();

        CanvasTransformer canvasTransformer = new CanvasTransformer(presentationReqFormatter, imageReqFormatter, parser, idMapper);
        CollectionTransformer collectionTransformer = new CollectionTransformer(presentationReqFormatter, parser);
        SequenceTransformer sequenceTransformer = new SequenceTransformer(presentationReqFormatter, parser, canvasTransformer);

        Set<Transformer<?>> transformers = new HashSet<>();
        transformers.add(new AnnotationListTransformer(presentationReqFormatter, parser));
        transformers.add(canvasTransformer);
        transformers.add(sequenceTransformer);
        transformers.add(new ManifestTransformer(presentationReqFormatter, parser, sequenceTransformer));
        transformers.add(new RangeTransformer(presentationReqFormatter));

        TransformerSet transformerSet = new TransformerSet(transformers);

        presentationTransformer = new PresentationTransformerImpl(presentationReqFormatter, parser, transformerSet, collectionTransformer);
    }

    @Test
    public void sequenceLudwigXV7Test() throws IOException {
        checkSequence(presentationTransformer.sequence(loadValidCollection(), loadValidLudwigXV7(), "reading-order"));
    }

    @Test
    public void canvasLudwigXV7Test() throws IOException {
        checkACanvas(presentationTransformer.canvas(loadValidCollection(), loadValidLudwigXV7(), "001v"));
    }

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

    @Test
    public void annotationListLudwigXV7Test() throws IOException {
        checkAnnotationList(presentationTransformer.annotationList(
                loadValidCollection(), loadValidLudwigXV7(), "002r.all"));
    }

    @Test
    public void annotationListFolgersHa2Test() throws IOException {
        checkAnnotationList(presentationTransformer.annotationList(
                loadValidCollection(), loadValidFolgersHa2(), "001r.all"));
        checkAnnotationList(presentationTransformer.annotationList(
                loadValidCollection(), loadValidFolgersHa2(), "001r.symbol"));
        checkAnnotationList(presentationTransformer.annotationList(
                loadValidCollection(), loadValidFolgersHa2(), "001r.marginalia"));
        checkAnnotationList(presentationTransformer.annotationList(
                loadValidCollection(), loadValidFolgersHa2(), "001r.underline"));
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

        for (Annotation ann : list) {
            checkAnnotation(ann);
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

    private void checkTarget(AnnotationTarget target, boolean isFullPage) {
        assertNotNull(target);
        if (isFullPage) {
            assertFalse(target.isSpecificResource());
            assertNull(target.getSelector());
        } else {
            assertTrue(target.isSpecificResource());

            Selector s = target.getSelector();
            assertNotNull(s);
            assertTrue(s instanceof FragmentSelector);
        }
    }

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
