package rosa.iiif.presentation.core.transform.impl;

import org.junit.Before;
import org.junit.Test;
import rosa.archive.core.ArchiveNameParser;
import rosa.archive.model.aor.Location;
import rosa.iiif.presentation.core.IIIFPresentationRequestFormatter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class AnnotationTransformerTest {

    private AnnotationTransformer transformer;

    @Before
    public void setup() {
        transformer = new AnnotationTransformer(
                new IIIFPresentationRequestFormatter("SCHEME", "HOST", "PREFIX", 80),
                new ArchiveNameParser()
        );
    }

//    @Test
//    public void illustrationsTest() {
//
//    }
//
    @Test
    public void locationToHtmlNoLocationTest() {
        String result = transformer.locationToHtml();
        assertNotNull("Result was NULL.", result);
        assertTrue("Result was not empty.", result.isEmpty());
    }

    @Test
    public void locationToHtmlTest() {

        List<Location[]> testLocations = testLocations();
        List<String> expected = expected();

        for (int i = 0; i < testLocations.size(); i++) {
            String result = transformer.locationToHtml(testLocations.get(i));

            assertNotNull("Result is NULL.", result);
            assertFalse("Result is empty.", result.isEmpty());
            assertEquals("Unexpected result found.", expected.get(i), result);
        }

    }

    private List<Location[]> testLocations() {
        List<Location[]> tests = new ArrayList<>();

        tests.add(new Location[] {Location.LEFT_MARGIN});
        tests.add(new Location[] {Location.LEFT_MARGIN, Location.RIGHT_MARGIN});
        tests.add(new Location[] {Location.LEFT_MARGIN, Location.INTEXT});
        tests.add(new Location[] {Location.HEAD, Location.TAIL, Location.INTEXT});

        return tests;
    }

    private List<String> expected() {
        return Arrays.asList(
                "<i class=\"aor-icon side-left \"></i>",
                "<i class=\"aor-icon side-left side-right \"></i>",
                "<i class=\"aor-icon side-left side-within \"><i class=\"inner\"></i></i>",
                "<i class=\"aor-icon side-top side-bottom side-within \"><i class=\"inner\"></i></i>"
        );
    }

}
