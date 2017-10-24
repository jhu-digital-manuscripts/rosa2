package rosa.archive.core.util;

import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Adapt transcriptions from Rose collection
 */
public class RoseTranscriptionAdapter {
    private static final Logger logger = Logger.getLogger("RoseTranscriptionAdapter");

    /**
     * Adapt transcription XML to HTML for display.
     *
     * @param xml string contents
     * @param name name of the xml fragment
     * @return HTML representations, a String per column in each page
     */
    public String toHtml(String xml, String name) {
        try {
            SAXParser parser = SAXParserFactory.newInstance().newSAXParser();
            RoseTranscriptionAdapterHandler handler = new RoseTranscriptionAdapterHandler();

            parser.parse(
                    new ByteArrayInputStream(xml.getBytes("UTF-8")),
                    handler
            );

            return handler.getAdaptedString();

        } catch (ParserConfigurationException | SAXException | IOException e) {
            logger.log(Level.WARNING, "Failed to adapt Rose transcription from TEI to HTML. ", e);
            return "";
        }
    }

}
