package rosa.archive.core.util;

import org.xml.sax.SAXException;
import rosa.archive.model.Transcription;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utility class that takes transcription XML and splits it by page.
 */
public class TranscriptionSplitter {
    private static final Logger log = Logger.getLogger(TranscriptionSplitter.class.toString());

    /**
     * Split up transcription data for a book into fragments according to page and
     * column.
     *
     * @param transcription transcription object from a book
     * @return transcription text split per page
     * 
     * @see #split(String)
     */
    public static Map<String, String> split(Transcription transcription) {
        if (transcription == null
                || transcription.getXML() == null || transcription.getXML().isEmpty()) {
            return Collections.emptyMap();
        }

        return split(transcription.getXML());
    }

    /**
     * Split up the transcription XML into fragments according to page and column. All
     * XML fragments are kept as Strings.
     *
     * The map produced by this method will contain entries that relate page number/folio
     * [short name?](ex: 135r) to the XML fragment representing the transcription on
     * that page. The XML fragment SHOULD start with a &lt;cb&gt; tag, indicating the
     * first column on that page. Any other columns will also be marked with the same
     * tag.
     *
     * Note that the transcription may be recorded in couplets and new columns
     * might split couplets. This results in a &lt;cb&gt; tag marking a new column
     * inside a &lt;lg&gt; couplet tag. Anyone parsing these XML fragments will have
     * to take this into consideration.
     *
     * @param xml original transcription XML containing all transcriptions
     * @return map of page TO transcription XML fragment
     */
    public static Map<String, String> split(String xml) {

        try {
            SAXParser parser = SAXParserFactory.newInstance().newSAXParser();
            SAXSplitter handler = new SAXSplitter();

            parser.parse(
                    new ByteArrayInputStream(xml.getBytes("UTF-8")),
                    handler
            );

            return handler.getPageMap();

        } catch (ParserConfigurationException | SAXException | IOException e) {
            log.log(Level.SEVERE, "Failed to parse transcription XML.", e);
            return Collections.emptyMap();
        }
    }

}
