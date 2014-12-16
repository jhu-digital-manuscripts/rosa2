package rosa.iiif.presentation.model.util;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class HtmlValueTest {

    @Test
    public void invalidAttributesAreRemovedTest() {
        String badHtml = "<a href=\"http://example.org/\" target=\"_blank\" data-bad=\"bad stuff\">Text To Display</a>";

        HtmlValue value = new HtmlValue(badHtml, "en");
        assertNotEquals(badHtml, value.getValue());
        assertEquals("<a href=\"http://example.org/\" rel=\"no-follow\">Text To Display</a>", value.getValue());
    }

    @Test
    public void invalidTagsAreRemovedTest() {
        String badHtml =
                "<p>"
                + "<span style=\"font-size:24px;\">T</span>"
                + "his is some text."
                + "<img src=\"http://example.org/\" alt=\"Alt text\"/>"
                + "<script src=\"http://example.org/bad-script.js\"/>"
                + "</p>";

        HtmlValue value = new HtmlValue(badHtml, "en");
        assertNotEquals(badHtml, value.getValue());

        String goodHtml = "<p>" +
                "<span>T</span>" +
                "his is some text." +
                "<img src=\"http://example.org/\" alt=\"Alt text\">" +
                "</p>";
        assertEquals(goodHtml, value.getValue());
    }

    @Test
    public void commentsAreRemovedTest() {
        String badHtml =
                "<p>"
                + "<span style=\"font-size:24px;\">T</span>"
                + "his is some text."
                + "<!-- Comment goes here --> "
                + "<img src=\"http://example.org/\" alt=\"Alt text\"/>"
                + "<script src=\"http://example.org/bad-script.js\"/>"
                + "</p>";

        HtmlValue value = new HtmlValue(badHtml, "en");
        assertNotEquals(badHtml, value.getValue());

        String goodHtml = "<p>" +
                "<span>T</span>" +
                "his is some text." +
                " <img src=\"http://example.org/\" alt=\"Alt text\">" +
                "</p>";
        assertEquals(goodHtml, value.getValue());
    }

}
