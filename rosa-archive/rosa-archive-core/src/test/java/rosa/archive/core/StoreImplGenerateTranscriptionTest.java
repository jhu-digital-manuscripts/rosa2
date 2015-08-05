package rosa.archive.core;

import org.junit.Before;
import org.junit.Test;
import rosa.archive.core.serialize.TranscriptionXmlSerializer;
import rosa.archive.model.Transcription;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class StoreImplGenerateTranscriptionTest extends BaseArchiveTest {
    private TranscriptionXmlSerializer transcriptionXmlSerializer;

    private List<String> errors;
    private List<String> warnings;

    @Before
    public void setup() {
        errors = new ArrayList<>();
        warnings = new ArrayList<>();
        transcriptionXmlSerializer = new TranscriptionXmlSerializer();
    }

    /**
     * Test the TEI transcription generate on the book 'LudwigXV7'. This book
     * contains transcription .txt files on a per page basis. The call to the
     * store should generate a single XML file that aggregates all of these
     * transcription text files in valid TEI XML.
     *
     * This relies on the code in the {@link Store#generateTEITranscriptions(String, String, List, List)}
     * method. This in turn relies on the {@link rosa.archive.core.util.TranscriptionConverter}
     * class and the {@link rosa.archive.core.util.XMLWriter} class.
     *
     * @throws Exception .
     */
    @Test
    public void generateTranscriptionLudwigTest() throws Exception {
        ByteStreamGroup bookGroup = base.getByteStreamGroup(VALID_COLLECTION)
                .getByteStreamGroup(VALID_BOOK_LUDWIGXV7);
        Transcription original = getTranscription(bookGroup);
        original.setXML(original.getXML().replaceAll("\\s+", " "));
        original.setXML(original.getXML().replaceAll("> <", "><"));

        store.generateTEITranscriptions(VALID_COLLECTION, VALID_BOOK_LUDWIGXV7, errors, warnings);
        Transcription result = getTranscription(bookGroup);
        result.setXML(result.getXML().replaceAll("\\s+", " "));
        result.setXML(result.getXML().replaceAll("> <", "><"));

        System.out.println("Original: " + original.getXML().length());
        System.out.println("Result:   " + result.getXML().length());

        final int start = 400;
        final int end = start + 200;
        for (int i = start; i < end; i++) {
            System.out.print(original.getXML().charAt(i));
        }
        System.out.println();
        for (int i = start; i < end; i++) {
//            if (i >= 23224) {
//                System.out.print(result.getXML().charAt(i + 5));
//            } else {
                System.out.print(result.getXML().charAt(i));
//            }
        }

        System.out.println("\n");
        for (int i = 0; i < original.getXML().length(); i++) {
            char o = original.getXML().charAt(i);
            char r = result.getXML().charAt(i);

            if (i == 23224) {
                // known difference
                continue;
            }
            if (i > 23224) {
                r = result.getXML().charAt(i + 5);
            }

            if (o != r) {
                System.out.println(i);
                break;
            }
        }

    }

    private Transcription getTranscription(ByteStreamGroup bookGroup) throws IOException {
        Transcription trans = null;
        try (InputStream in = bookGroup.getByteStream(bookGroup.name() + ".transcription.xml")) {
            trans = transcriptionXmlSerializer.read(in, errors);
            assertTrue("Errors encountered while reading original transcription.", errors.isEmpty());
            assertNotNull(trans);
        }

        return trans;
    }

}
