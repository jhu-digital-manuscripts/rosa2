package rosa.iiif.presentation.core.extres;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.URI;

import org.junit.BeforeClass;
import org.junit.Test;

public class PleaidasGazetteerTest {
    private static PleaidesGazetteer pg;
    
    @BeforeClass
    public static void setup() throws IOException {
        pg = new PleaidesGazetteer();
    }
    
    @Test
    public void testRomeLookup() throws IOException {
        assertEquals(URI.create("https://pleiades.stoa.org/places/423025"), pg.lookup("Rome"));
    }

    @Test
    public void testDecoration() throws IOException {
        String text = "The cow goes Moo in Rome and Athens.";
        String html = new HtmlDecorator().decorate(text, pg);

        assertTrue(html.contains("https://pleiades.stoa.org/places/423025"));
        assertTrue(html.contains("https://pleiades.stoa.org/places/579885"));
    }
    
    @Test
    public void testMultiWordDecorationFail1() throws IOException {
        String text = "The cow goes Moo in Rome Athens.";
        String html = new HtmlDecorator().decorate(text, pg);

        assertFalse(html.contains("http"));
    }
    
    @Test
    public void testMultiWordDecorationFail2() throws IOException {
        String text = "The cow goes Moo in RomeAthens.";
        String html = new HtmlDecorator().decorate(text, pg);

        assertFalse(html.contains("http"));
    }
}
