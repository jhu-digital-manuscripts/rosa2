package rosa.archive.core.util;

import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;

/**
 * Adapt transcriptions from Rose collection
 */
public class RoseTranscriptionAdapter {

    /**
     * Adapt transcription XML to HTML for display.
     *
     * @param xml string contents
     * @return HTML representations, a String per column in each page
     */
    public String toHtml(String xml, String name) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {

            Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(
                    new InputSource(new StringReader(xml))
            );

            XMLUtil.write(doc, out, true);

        } catch (ParserConfigurationException | IOException | SAXException e) {
            return null;
        }

        StringBuilder result = new StringBuilder();

        String[] lines = out.toString().split("\n");
        for (String line : lines) {
            boolean needsNL = true;
            line = line.trim();

            if (skipLine(line)) {
                needsNL = false;
            }

            if (line.startsWith("<div")) {
                needsNL = false;
            }
            if (line.startsWith("<pb")) {
                line = line.replaceFirst("<pb\\s+n=\"", "<h3>").replaceFirst("\"\\s*/>", "</h3>");
                needsNL = false;
            }
            if (line.startsWith("<cb")) {
                line = line.replaceFirst("<cb\\s+n=\"", "<h5>Column ").replaceFirst("\"\\s*/>", "</h5>");
                needsNL = false;
            }
            if (line.contains("</l>")) {
                line = line.replaceAll("</l>", "</l><br/>");
                needsNL = false;
            }

            if (line.contains("<hi ")) {
                line = line.replaceAll("<hi\\s+rend", "<span class").replaceAll("</hi>", "</span>");
            }
            if (line.contains("<note")) {
                line = line.replaceAll("<note .*</note>", "");
            }
            if (line.contains("<expan")) {
                line = line.replaceAll("<expan>", "<span class=\"expan\">").replaceAll("</expan>", "</span>");
            }

            if (needsNL)
                line = line.concat("<br/>");

            result.append(line);
        }

        return result.toString();
    }

    private boolean skipLine(String line) {
        return line.startsWith("<figure") || line.startsWith("</figure>") ||
                line.startsWith("<lg") || line.startsWith("</lg") ||
                line.startsWith("<milestone") ||
                line.equals("<l>") || line.equals("</l>");
    }

}
