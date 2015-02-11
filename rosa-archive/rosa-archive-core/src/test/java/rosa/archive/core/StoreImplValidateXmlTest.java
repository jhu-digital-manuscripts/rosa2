package rosa.archive.core;

import org.junit.Test;
import rosa.archive.model.Book;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class StoreImplValidateXmlTest extends BaseArchiveTest {

    @Test
    public void validateXmlTest() throws IOException {
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        assertTrue("No AoR transcriptions found.", hasXml(VALID_BOOK_FOLGERSHA2));

        store.validateXml(VALID_COLLECTION, VALID_BOOK_FOLGERSHA2, errors, warnings);
        assertTrue("Unexpected errors found.", errors.isEmpty());
        assertTrue("Unexpected warnings found.", warnings.isEmpty());
    }

    @Test
    public void validateXmlWithNoXmlFiles() throws IOException {
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        assertFalse("AoR transcriptions found.", hasXml(VALID_BOOK_LUDWIGXV7));

        store.validateXml(VALID_COLLECTION, VALID_BOOK_LUDWIGXV7, errors, warnings);
        assertTrue("Unexpected errors found.", errors.isEmpty());
        assertTrue("Unexpected warnings found.", warnings.isEmpty());
    }

    private boolean hasXml(String book) throws IOException {
        Book b = loadBook(VALID_COLLECTION, book);
        assertNotNull("Book not found in test archive.", b);

        boolean hasXml = false;
        for (String name : b.getContent()) {
            if (name.endsWith(ArchiveConstants.XML_EXT) && name.contains(ArchiveConstants.AOR_ANNOTATION)) {
                hasXml = true;
            }
        }

        return hasXml;
    }

}
