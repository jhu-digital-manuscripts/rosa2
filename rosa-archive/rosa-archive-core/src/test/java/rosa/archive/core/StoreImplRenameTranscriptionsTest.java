package rosa.archive.core;

import org.junit.Before;
import org.junit.Test;
import rosa.archive.model.ArchiveItemType;
import rosa.archive.model.BookCollection;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class StoreImplRenameTranscriptionsTest extends BaseArchiveTest {
    private static final ArchiveNameParser parser = new ArchiveNameParser();

    private List<String> errors;

    /**
     * Set up. Create fresh list to hold errors.
     */
    @Before
    public void setup() {
        errors = new ArrayList<>();
    }

    /**
     * Rename all AoR transcriptions according to a file map.
     *
     * @throws IOException .
     */
    @Test
    public void renameTranscriptionsNormalTest() throws IOException {
        generateFileMap(VALID_COLLECTION, VALID_BOOK_FOLGERSHA2, "BOOK_ID", true, true, 10, 10, 1);

        store.renameTranscriptions(VALID_COLLECTION, VALID_BOOK_FOLGERSHA2, false, errors);
        assertTrue("Unexpected errors found.", errors.isEmpty());

        List<String> names = base.getByteStreamGroup(VALID_COLLECTION).getByteStreamGroup(VALID_BOOK_FOLGERSHA2)
                .listByteStreamNames();
        checkPages("BOOK_ID", names);

        assertTrue(names.contains("BOOK_ID.aor.frontmatter.flyleaf.001v.xml"));
        assertTrue(names.contains("BOOK_ID.aor.binding.frontcover.xml"));
        assertTrue(names.contains("BOOK_ID.aor.032v.xml"));
        checkWithBookChecker(VALID_COLLECTION, VALID_BOOK_FOLGERSHA2);
    }

    /**
     * Does not rename any items due to duplicate values in the file map.
     *
     * @throws IOException .
     */
    @Test
    public void dontRenameWithBadFileMap() throws IOException {
        generateFileMap(VALID_COLLECTION, VALID_BOOK_FOLGERSHA2, "BOOK_ID", true, true, 10, 10, 5);

        store.renameTranscriptions(VALID_COLLECTION, VALID_BOOK_FOLGERSHA2, false, errors);
        assertFalse("Errors should occur, but were not encountered.", errors.isEmpty());
        checkWithBookChecker(VALID_COLLECTION, VALID_BOOK_FOLGERSHA2);
    }

    /**
     * Here, we want to rename all files that we can. One file in the middle will contain an XML
     * syntax error, making our tools unable to parse it. Make sure that all other files are
     * renamed.
     *
     * @throws Exception .
     */
    @Test
    public void renameAllButUnparsableFileTest() throws Exception {
        final String BAD_FILE = "FolgersHa2.aor.022r.xml";
        generateFileMap(VALID_COLLECTION, VALID_BOOK_FOLGERSHA2, "Moo_ID", false, false, 5, 5, 1);

        // Append some stuff to the file to make it invalid XML
        Path badPath = getBookPath(VALID_COLLECTION, VALID_BOOK_FOLGERSHA2).resolve(BAD_FILE);
        Files.write(badPath, "Bad moo".getBytes("UTF-8"), StandardOpenOption.APPEND);

        store.renameTranscriptions(VALID_COLLECTION, VALID_BOOK_FOLGERSHA2, false, errors);
        assertEquals(1, errors.size());
        assertEquals("Failed to read XML transcription. FolgersHa2.aor.022r.xml", errors.get(0));

        List<String> names = base.getByteStreamGroup(VALID_COLLECTION).getByteStreamGroup(VALID_BOOK_FOLGERSHA2)
                .listByteStreamNames().stream()
                .filter(n -> n.endsWith(".xml") && !n.contains("description"))
                .collect(Collectors.toList());

        assertEquals(
                "Expecting all files to be renamed except one.",
                names.size() - 1,
                names.stream().filter(name -> name.startsWith("Moo_ID")).count()
        );

        names.stream().filter(name -> name.startsWith("Moo_ID")).forEach(name -> {
            Path p = getBookPath(VALID_COLLECTION, VALID_BOOK_FOLGERSHA2).resolve(name);
            // Make sure the renamed XML file has content
            try {
                List<String> lines = Files.readAllLines(p, Charset.forName("UTF-8"));
                assertNotNull(lines);
                assertFalse(lines.isEmpty());
            } catch (Exception e) {
                fail("Failed to read back transcription file. " + e.getMessage());
            }
        });
    }

    private boolean checkWithBookChecker(String collection, String book) throws IOException {
        BookCollection col = store.loadBookCollection(collection, errors);
        return store.check(col, store.loadBook(col, book, errors), false, errors, null);
    }

    private void checkPages(String expectedId, List<String> names) {
        for (String name : names) {
            if (parser.getArchiveItemType(name) == ArchiveItemType.TRANSCRIPTION_AOR) {
                assertTrue("Unexpected ID found in image name.", name.startsWith(expectedId));
            }
        }
    }

    private void generateFileMap(String collection, String book, String id, boolean hasFront, boolean hasBack,
                                 int front, int end, int misc) throws IOException {
        ByteStreamGroup bookStreams = base.getByteStreamGroup(VALID_COLLECTION).
                getByteStreamGroup(VALID_BOOK_FOLGERSHA2);
        if (!bookStreams.hasByteStream("filemap.csv")) {
            // Create a file map, because the test data does not include it
            store.generateFileMap(collection, book, id, hasFront, hasBack, front, end, misc, errors);

            assertTrue("Unexpected errors found.", errors.isEmpty());
            assertTrue("Failed to create file map.", bookStreams.hasByteStream("filemap.csv"));
        }
    }

}
