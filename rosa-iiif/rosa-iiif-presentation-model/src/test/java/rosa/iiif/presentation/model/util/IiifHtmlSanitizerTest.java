package rosa.iiif.presentation.model.util;

import org.junit.Test;

public class IiifHtmlSanitizerTest {

    @Test
    public void test() {
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

        IiifHtmlSanitizer sanitizer = IiifHtmlSanitizer.defaultSanitizer();
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
//        System.out.println(safeHtml);
//        System.out.println(expectedHtml);
    }

}
