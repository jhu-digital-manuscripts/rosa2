package rosa.archive.core.util;

import org.apache.commons.io.IOUtils;
import org.junit.Test;
import rosa.archive.core.BaseArchiveTest;
import rosa.archive.model.Book;
import rosa.archive.model.Transcription;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Map.Entry;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class TranscriptionSplitterTest extends BaseArchiveTest {

    /**
     * Load valid book LudwigXV7 from the test archive and split the transcription
     * XML. For each page that has a transcription, there should be an associated
     * XML fragment in the resulting Map.
     *
     * @throws IOException .
     */
    @Test
    public void splitTranscriptionLudwigXV7() throws IOException {
        String transcription = loadLudwigTranscription();
        Map<String, String> map = TranscriptionSplitter.split(transcription);

        assertNotNull("Results map is empty.", map);
        assertEquals("Unexpected number of pages.", 172, map.size());

        for (Entry<String, String> entry : map.entrySet()) {
            assertNotNull("NULL key", entry.getKey());
            assertFalse("Empty key", entry.getKey().isEmpty());
            // Check key, should be one to three digits followed by 'r' or 'v'
            assertTrue("Unexpected key format", entry.getKey().matches("^(\\d{1,3})(r|v)$"));
            assertNotNull("NULL value", entry.getValue());
            assertFalse("Empty value", entry.getValue().isEmpty());
        }
    }

    private String loadLudwigTranscription() throws IOException {
        Book book = loadValidLudwigXV7();
        Transcription transcription = book.getTranscription();

        assertNotNull("Transcription missing.", transcription);
        assertFalse("Transcription string missing.", transcription.getXML().isEmpty());

        return transcription.getXML();
    }

    @Test
    public void testDouce() throws IOException {
        try (InputStream in = getClass().getClassLoader().getResourceAsStream("Douce195.transcription.xml")) {
            String xml = IOUtils.toString(in, "UTF-8");
            assertNotNull("No XML found.", xml);

            Map<String, String> xmlMap = TranscriptionSplitter.split(xml);
            assertNotNull("XML failed to split.", xmlMap);
            assertEquals("Unexpected number of pages found.", 312, xmlMap.size());
        }
    }

//    @Test
//    public void test() throws Exception {
////        try (InputStream in = getClass().getClassLoader().getResourceAsStream("Douce195.transcription.xml")) {
//        try (InputStream in = getClass().getClassLoader().getResourceAsStream("archive/valid/LudwigXV7/LudwigXV7.transcription.xml")) {
//
//            SAXParser parser = SAXParserFactory.newInstance().newSAXParser();
//            SAXSplitter handler = new SAXSplitter();
//
//            parser.parse(in, handler);
//
//            Map<String, String> map = handler.getPageMap();
//
////            for (int i = 1; i < 157; i++) {
////
////                String recto = i + "r";
////                String verso = i + "v";
////
////                if (i < 10) {
////                    recto = "00" + recto;
////                    verso = "00" + verso;
////                } else if (i < 100) {
////                    recto = "0" + recto;
////                    verso = "0" + verso;
////                }
////
////                if (map.containsKey(recto)) {
////                    System.out.println(recto);
////                } else {
////                    System.out.println("**" + recto);
////                }
////
////                if (map.containsKey(verso)) {
////                    System.out.println(verso);
////                } else {
////                    System.out.println("**" + verso);
////                }
////            }
//            System.out.println(map.get("013r"));
//
//        }
//    }
}
