package rosa.iiif.presentation.core.transform;

import org.junit.Before;
import org.junit.Test;
import rosa.archive.core.BaseArchiveTest;
import rosa.archive.model.aor.Location;
import rosa.iiif.presentation.core.IIIFRequestFormatter;
import rosa.iiif.presentation.core.JhuFsiImageIdMapper;
import rosa.iiif.presentation.model.AnnotationList;
import rosa.iiif.presentation.model.Canvas;
import rosa.iiif.presentation.model.IIIFNames;
import rosa.iiif.presentation.model.Layer;
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
import java.util.Map;

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

    private PresentationTransformer transformer;

    /**
     * Set up a new PresentationTransformer with each test.
     */
    @Before
    public void setup() {
        Map<String, String> idMap = new HashMap<>();
        idMap.put(VALID_COLLECTION, "valid");

        transformer = new PresentationTransformer(
                new IIIFRequestFormatter(ENDPOINT_SCHEME, ENDPOINT_HOST, ENDPOINT_PREFIX, ENDPOINT_PORT),
                new rosa.iiif.image.core.IIIFRequestFormatter(
                        ENDPOINT_SCHEME, ENDPOINT_HOST, ENDPOINT_PORT, ENDPOINT_PREFIX),
                new JhuFsiImageIdMapper(idMap)
        );
    }

    /**
     * Generate sequence from LudwigXV7 and check validity.
     * @throws IOException if collection and/or book not found
     */
    @Test
    public void sequenceLudwigXV7Test() throws IOException {
        checkSequence(transformer.sequence(loadValidCollection(), loadValidLudwigXV7(), "reading-order"));
    }

    /**
     * Generate canvas from LudwigXV7 and check validity.
     * @throws IOException if collection or book is not found
     */
    @Test
    public void canvasLudwigXV7Test() throws IOException {
        checkACanvas(transformer.canvas(loadValidCollection(), loadValidLudwigXV7(), "001v"));
    }

    /**
     * Generate a manifest for LudwigXV7 and check validity.
     * @throws IOException if collection or book is not found
     */
    @Test
    public void manifestLudwigXV7Test() throws IOException {
        Manifest manifest = transformer.manifest(loadValidCollection(), loadValidLudwigXV7());
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
        checkAnnotationList(transformer.annotationList(loadValidCollection(), loadValidLudwigXV7(), "002r", "all"));
    }

    /**
     * Generate annotation lists from FolgersHa2 and check validity.
     * @throws IOException if collection or book is not found.
     */
    @Test
    public void annotationListFolgersHa2Test() throws IOException {
        checkAnnotationList(transformer.annotationList(loadValidCollection(), loadValidFolgersHa2(), "001r", "all"));
        checkAnnotationList(transformer.annotationList(loadValidCollection(), loadValidFolgersHa2(), "001r", "symbol"));
        checkAnnotationList(transformer.annotationList(loadValidCollection(), loadValidFolgersHa2(), "001r", "marginalia"));
        checkAnnotationList(transformer.annotationList(loadValidCollection(), loadValidFolgersHa2(), "001r", "underline"));
    }

    /**
     * Generate layers from FolgersHa2 and check validity.
     * @throws IOException if collection or book is not found
     */
    @Test
    public void layerFolgersHa2Test() throws IOException {
        checkLayer(transformer.layer(loadValidCollection(), loadValidFolgersHa2(), "all"));
        checkLayer(transformer.layer(loadValidCollection(), loadValidFolgersHa2(), "underline"));
        checkLayer(transformer.layer(loadValidCollection(), loadValidFolgersHa2(), "marginalia"));
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
        assertFalse("'within' property empty.", list.getWithin().isEmpty());

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

    @Test
    public void locationOnCanvasTest() {
        for (int i = 0; i < 10; i++) {
            Canvas c = new Canvas();
            c.setId("Canvas" + i);
            c.setWidth(1000);
            c.setHeight(1500);

            checkTarget(transformer.locationOnCanvas(c, Location.RIGHT_MARGIN), false);
            checkTarget(transformer.locationOnCanvas(c, Location.FULL_PAGE), true);

            AnnotationTarget t = transformer.locationOnCanvas(c, Location.HEAD);
            checkTarget(t, false);

            String selectorContent = t.getSelector().content();

            String[] parts = selectorContent.split(",");
            assertEquals(4, parts.length);
            assertEquals("0", parts[0]);
            assertEquals("0", parts[1]);
            assertEquals("1000", parts[2]);
            // x, y, width are always known. Height depends on the guessing factor
        }
    }

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
