package rosa.iiif.presentation.core.extres;

import static org.junit.Assert.*;

import java.io.IOException;
import java.net.URI;

import org.junit.Before;
import org.junit.Test;

public class PleaidasGazetteerTest {
    private PleaidasGazetteer pg;
    
    @Before
    public void setup() throws IOException {
        pg = new PleaidasGazetteer();
    }
    
    @Test
    public void testRomeLookup() throws IOException {
        assertEquals(URI.create("https://pleiades.stoa.org/places/423025"), pg.lookup("Rome"));
    }

    @Test
    public void testDecoration() throws IOException {
        PleaidasGazetteer pg = new PleaidasGazetteer();

        String text = "The cow goes Moo in Rome and athens.";

        String html = new HtmlDecorator().decorate(text, pg);

        System.err.println(html);
    }
}
