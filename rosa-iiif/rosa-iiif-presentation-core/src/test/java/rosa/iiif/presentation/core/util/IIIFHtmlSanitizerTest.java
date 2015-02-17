package rosa.iiif.presentation.core.util;

import static org.junit.Assert.assertEquals;

import org.junit.Ignore;
import org.junit.Test;

// TODO fix tests...
public class IIIFHtmlSanitizerTest {

    /**
     * Test the default settings.
     */
    @Ignore
    @Test
    public void defaultSanitizerTest() {
        String badHtml =
                "<p class=\"top\">" +
                "<span style=\"font-size: 24px;\">T</span>" +
                "his is some text." +
                "<a href=\"http://www.example.org/image\" target=\"_blank\">" +
                "<img src=\"http://example.org/image.jpg\" alt=\"Alt Text\" data-bad=\"bad data\"/>" +
                "</a>" +
                "<!-- Comment should be removed! -->" +
                "<strong>This should not be here, apparently.</strong>" +
                "</p>";

        IIIFHtmlSanitizer sanitizer = IIIFHtmlSanitizer.defaultSanitizer();
        String safeHtml = sanitizer.sanitize(badHtml);

        String expectedHtml =
                "<p>" +
                "<span>T</span>" +
                "his is some text." +
                "<a href=\"http://www.example.org/image\">" +
                "<img alt=\"Alt Text\" src=\"http://example.org/image.jpg\"/>" +
                "</a>" +
                "&lt;!-- Comment should be removed! --&gt;" +
                "&lt;strong&gt;This should not be here, apparently.&lt;/strong&gt;" +
                "</p>";

        assertEquals("Unexpected results found.", expectedHtml, safeHtml);
    }

    /**
     * Test one set of custom settings.
     */
    @Ignore
    @Test
    public void customSanitizerTest() {
        IIIFHtmlSanitizer sanitizer = IIIFHtmlSanitizer.newSanitizer()
                .addTags("p", "strong")
                .addAttributes("p", "class");

        String badHtml =
                "<p class=\"top\">" +
                "<span style=\"font-size: 24px;\">T</span>" +
                "his is some text." +
                "<a href=\"http://www.example.org/image\" target=\"_blank\">" +
                "<img src=\"http://example.org/image.jpg\" alt=\"Alt Text\" data-bad=\"bad data\"/>" +
                "</a>" +
                "<!-- Comment should be removed! -->" +
                "<strong>This should not be here, apparently.</strong>" +
                "</p>";
        String expectedHtml =
                "<p class=\"top\">" +
                "&lt;span style=&quot;font-size: 24px;&quot;&gt;T&lt;/span&gt;" +
                "his is some text." +
                "&lt;a href=&quot;http://www.example.org/image&quot; target=&quot;_blank&quot;&gt;" +
                "&lt;img src=&quot;http://example.org/image.jpg&quot; alt=&quot;Alt Text&quot; data-bad=&quot;bad data&quot;/&gt;" +
                "&lt;/a&gt;" +
                "&lt;!-- Comment should be removed! --&gt;" +
                "<strong>This should not be here, apparently.</strong>" +
                "</p>";

        assertEquals("Unexpected results found.", expectedHtml, sanitizer.sanitize(badHtml));
    }

}
