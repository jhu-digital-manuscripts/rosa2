package rosa.archive.core;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;

import com.google.inject.Inject;

import rosa.archive.core.GuiceJUnitRunner.GuiceModules;
import rosa.archive.core.check.BookChecker;
import rosa.archive.core.check.BookCollectionChecker;
import rosa.archive.core.serialize.SerializerSet;
import rosa.archive.model.aor.AnnotatedPage;

/**
 * Test a part of the method {@link Store#renameTranscriptions(String, String, boolean, List)}.
 * Specifically, test the part that renames appropriate attributes/values of the 'internal ref'
 * tag and 'target' child tag.
 *
 * &lt;internal_ref text="..."&gt;
 *   &lt;target filename="..." book_id="..." text="..." /&gt;
 *   ...
 * &lt;/internal_ref&gt;
 *
 * For each target of the internal_ref tags, the filename and book_id should
 * be transformed appropriately from original Git name to its archive name.
 *
 * Test data stored in:
 *      src/test/resources/data/
 *          |
 *          | --- Buchanan_MariaScotorumRegina/
 *          ` --- Domenichi/
 *
 * Buchanan_MariaScotorumRegina directory will contain some transcriptions files. They have
 * only 2 internal references, each point back to the same book.
 *
 * Domenichi data has been tweaked slightly from real data in Git to have some references
 * point to the Buchanan test data. There are 45 internal ref targets in this set, two
 * of which point to the Buchanan_MariaScotorum book.
 *   - Ha2.040r.xml
 *   - Ha2.049r.xml
 *
 * Unlike the previous test {@link StoreImplRenameTranscriptionsTest},
 * which tests on data that has already undergone these transformations, this test
 * must operate on a non-collection set of data, a small sub-set of the data from
 * the Git repository (https://github.com/livesandletters/aor.git). By necessity,
 * this data is not in the same state as the archive collections, so the BaseArchiveTest
 * cannot be used to load data.
 *
 * Parts of the code rely on the Store's serializers, so a Store must first be setup,
 * it just won't have the same functionality as a Store on an archive.
 */
@RunWith(GuiceJUnitRunner.class)
@GuiceModules({ ArchiveCoreModule.class })
public class StoreImplRenameReferencesTest {
    private static final String VALID_COLLECTION = "collection";
    private static final String BOOK_DOMENICHI = "Domenichi";
    private static final String BOOK_BUCHANAN = "Buchanan_MariaScotorumRegina";

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Inject
    protected SerializerSet serializers;

    @Inject
    protected BookChecker bookChecker;

    @Inject
    protected BookCollectionChecker collectionChecker;

    protected ByteStreamGroup base;
    protected Path basePath;
    protected StoreImpl store;

    /**
     * Copy files over to a temp directory, setup Store
     */
    @Before
    public void setup() throws IOException {
        Path tmp = tempFolder.newFolder().toPath();

        ResourceUtil.copyResource(getClass(), "/data", tmp);

        basePath = tmp.resolve("data");
        base = new FSByteStreamGroup(basePath);
        store = new StoreImpl(serializers, bookChecker, collectionChecker, base, true);
    }

    /**
     * Rename the AoR transcriptions in Domenichi. Basically, the prefix 'Folgers'
     * should be added to all files, as specified in the Domenichi file map.
     *
     * @throws Exception
     */
    @Test
    public void renameDomenichiTranscriptionsTest() throws Exception {
        List<String> errors = new ArrayList<>();

        store.renameTranscriptions(VALID_COLLECTION, BOOK_DOMENICHI, false, errors);
        assertTrue("Errors were found while renaming.", errors.isEmpty());

        // Make sure all transcriptions have been renamed
        List<String> filesInBook = base.getByteStreamGroup(VALID_COLLECTION)
                .getByteStreamGroup(BOOK_DOMENICHI)
                .listByteStreamNames();
        assertTrue(filesInBook.parallelStream()
                .filter(file -> file.endsWith(".xml"))
                .allMatch(file -> file.endsWith(".xml") && file.startsWith("FolgersHa2.aor.")));

        // Ensure that 'filename' attribute in all transcriptions have been changed
        assertTrue(getPages(BOOK_DOMENICHI).parallelStream().map(AnnotatedPage::getPage)
                .allMatch(page -> page.endsWith(".tif") && page.startsWith("FolgersHa2")));
    }

    /**
     * Rename transcriptions in book: Buchanan_MariaScotorumRegina. There are
     * TWO internal references that must also be renamed. Final name for this
     * book is 'PrincetonRB16th11'
     *
     * @throws Exception
     */
    @Test
    public void renameBuchananTranscriptionsTest() throws Exception {
        List<String> errors = new ArrayList<>();

        store.renameTranscriptions(VALID_COLLECTION, BOOK_BUCHANAN, false, errors);
        assertTrue("Errors were found while renaming.", errors.isEmpty());

        // Make sure all transcriptions have been renamed
        List<String> filesInBook = base.getByteStreamGroup(VALID_COLLECTION)
                .getByteStreamGroup(BOOK_BUCHANAN)
                .listByteStreamNames();
        assertTrue(filesInBook.parallelStream()
                .filter(file -> file.endsWith(".xml"))
                .allMatch(file -> file.endsWith(".xml") && file.startsWith("PrincetonRB16th11.aor.")));

        // Ensure that 'filename' attribute in all transcriptions have been changed
        assertTrue(getPages(BOOK_BUCHANAN).parallelStream().map(AnnotatedPage::getPage)
                .allMatch(page -> page.endsWith(".tif") && page.startsWith("PrincetonRB16th11")));
    }

    /**
     * Read all AoR transcriptions for a given book and return a list of
     * AnnotatedPage objects.
     *
     * @param book name of book
     * @return list of AnnotatedPage objects
     * @throws IOException
     */
    private List<AnnotatedPage> getPages(String book) throws IOException {
        assertTrue("Book not found.", base.getByteStreamGroup(VALID_COLLECTION).hasByteStreamGroup(book));

        List<AnnotatedPage> list = new ArrayList<>();

        ByteStreamGroup bookStreams = base.getByteStreamGroup(VALID_COLLECTION).getByteStreamGroup(book);
        List<String> filesInBook = bookStreams.listByteStreamNames();

        List<String> errors = new ArrayList<>();
        for (String file : filesInBook) {
            if (!file.endsWith(".xml")) {
                continue;
            }

            errors.clear();
            try (InputStream in = bookStreams.getByteStream(file)) {
                AnnotatedPage page = serializers.getSerializer(AnnotatedPage.class).read(in, errors);

                assertNotNull("Page found NULL (" + book + ":" + file + ")", page);
                assertTrue("Errors found while reading page. (" + book + ":" + file + ")", errors.isEmpty());

                page.setId(file);
                list.add(page);
            }
        }


        return list;
    }

    // For debugging
//    private void printTarget(ReferenceTarget target) {
//        System.out.println(
//                "<target book_id=\""
//                        + target.getBookId()
//                        + "\" filename=\""
//                        + target.getFilename()
//                        + "\" text=\""
//                        + target.getText()
//                        + "\" />"
//        );
//    }

}
