package rosa.iiif.presentation.core;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class StaticResourceRequestFormatterTest {
    private static final String EXPECTED_URL_START = "scheme://host/prefix/";
    private StaticResourceRequestFormatter formatter;

    @Before
    public void setup() {
        this.formatter = new StaticResourceRequestFormatter(
                "scheme",
                "host",
                "/prefix",
                -1
        );
    }

    @Test
    public void uriForCollectionCsvTest() {
        String expected = EXPECTED_URL_START +  "rose/character_names.csv";
        String result = formatter.format("rose", null, "character_names.csv");
        assertEquals(expected, result);
    }

    @Test
    public void uriForBookDescriptionTest() {
        String expected = EXPECTED_URL_START + "rose/Walters143/Walters143.description_en.xml";
        String result = formatter.format("rose", "Walters143", "Walters143.description_en.xml");
        assertEquals(expected, result);
    }

}
