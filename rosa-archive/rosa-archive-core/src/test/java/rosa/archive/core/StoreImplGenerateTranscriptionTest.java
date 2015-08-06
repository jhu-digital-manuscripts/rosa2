package rosa.archive.core;

import org.junit.Before;
import org.junit.Test;
import rosa.archive.core.serialize.TranscriptionXmlSerializer;
import rosa.archive.model.Transcription;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class StoreImplGenerateTranscriptionTest extends BaseArchiveTest {

    private List<String> errors;
    private List<String> warnings;

    @Before
    public void setup() {
        errors = new ArrayList<>();
        warnings = new ArrayList<>();
    }

    /**
     * Test the TEI transcription generation on book 'FolgersHa2'. This book
     * contains no text transcription files, nor TEI transcription files.
     * No transcription files should be created because of this. The call to
     * the store should return before creating any new files with no warnings
     * or errors.
     *
     * @throws Exception .
     */
    @Test
    public void dontGenerateTranscriptionFolgersHa2() throws Exception {
        Path bookPath = getBookPath(VALID_COLLECTION, VALID_BOOK_FOLGERSHA2);
        Path transcriptionPath = bookPath.resolve("FolgersHa2.transcription.xml");

        assertFalse("No transcription file should exist.", Files.exists(transcriptionPath));

        store.generateTEITranscriptions(VALID_COLLECTION, VALID_BOOK_FOLGERSHA2, errors, warnings);

        assertFalse("No transcription file should have been written.", Files.exists(transcriptionPath));
        assertTrue("Errors found.", errors.isEmpty());
        assertTrue("Warnings found.", warnings.isEmpty());
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
        TranscriptionXmlSerializer serializer = new TranscriptionXmlSerializer();

        Path bookPath = getBookPath(VALID_COLLECTION, VALID_BOOK_LUDWIGXV7);
        Path transcriptionPath = bookPath.resolve("LudwigXV7.transcription.xml");

        store.generateTEITranscriptions(VALID_COLLECTION, VALID_BOOK_LUDWIGXV7, errors, warnings);

        assertTrue("Errors were found.", errors.isEmpty());
//        assertTrue("Warnings were found.", warnings.isEmpty());

        assertTrue("Transcription XML file missing.", Files.exists(transcriptionPath));
        try (InputStream in = Files.newInputStream(transcriptionPath)) {
            Transcription trans = serializer.read(in, errors);

            assertNotNull("Transcription XML unreadable.", trans);
            assertNotNull("Text not readable.", trans.getXML());
            assertFalse("Text was blank.", trans.getXML().isEmpty());
        }

        // TODO check for valid TEI

    }

}
