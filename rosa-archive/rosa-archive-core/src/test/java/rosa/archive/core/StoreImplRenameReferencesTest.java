package rosa.archive.core;

import com.google.inject.Inject;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import rosa.archive.core.GuiceJUnitRunner.GuiceModules;
import rosa.archive.core.check.BookChecker;
import rosa.archive.core.check.BookCollectionChecker;
import rosa.archive.core.serialize.SerializerSet;
import rosa.archive.model.FileMap;
import rosa.archive.model.aor.AnnotatedPage;
import rosa.archive.model.aor.InternalReference;
import rosa.archive.model.aor.Marginalia;
import rosa.archive.model.aor.MarginaliaLanguage;
import rosa.archive.model.aor.Position;
import rosa.archive.model.aor.ReferenceTarget;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

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
    private static final String DIRECTORY_MAP = "dir-map.csv";

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
        store = new StoreImpl(serializers, bookChecker, collectionChecker, base);
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
        for (String file : filesInBook) {
            if (file.endsWith(".xml")) {
                assertTrue("Unexpected prefix for transcription. (" + file + ")", file.startsWith("FolgersHa2.aor."));
            }
        }

        // Load file maps
        Map<String, FileMap> filemaps = loadFileMaps();

        int folgers_count = 0;
        int princeton_count = 0;
        // Make sure all <internal_ref>s have been changed appropriately
        List<AnnotatedPage> pages = getPages(BOOK_DOMENICHI);
        for (AnnotatedPage page : pages) {
            assertNotNull("NULL page.", page);

            for (Marginalia marg : page.getMarginalia()) {
                for (MarginaliaLanguage lang : marg.getLanguages()) {
                    for (Position pos : lang.getPositions()) {
                        for (InternalReference ref : pos.getInternalRefs()) {
                            for (ReferenceTarget target : ref.getTargets()) {
//                                printTarget(target);
                                assertNotNull("Target book id is NULL.", target.getBookId());
                                assertFalse("Target book id is empty.", target.getBookId().isEmpty());
                                assertNotNull("Target filename is NULL.", target.getFilename());
                                assertFalse("Target filename is empty.", target.getFilename().isEmpty());

                                assertTrue("Unexpected Book ID found. (" + target.getBookId() + ")",
                                        filemaps.get(DIRECTORY_MAP).getMap().containsValue(target.getBookId()));
                                assertTrue("Unexpected filename found. (" + target.getFilename() + ")",
                                        target.getFilename().startsWith("FolgersHa2.aor.") ||
                                                target.getFilename().startsWith("PrincetonRB16th11.aor.") &&
                                                target.getFilename().endsWith(".xml"));

                                if (target.getBookId().startsWith("Folgers")) {
                                    folgers_count++;
                                } else if (target.getBookId().startsWith("Princeton")) {
                                    princeton_count++;
                                } else {
                                    fail("Unexpected book id found. (" + target.getBookId() + ")");
                                }
                            }
                        }
                    }
                }
            }
        }

        assertEquals("Unexpected number of Folgers references", 43, folgers_count);
        assertEquals("Unexpected number of PrincetonRB16th11 references", 2, princeton_count);
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
        for (String file : filesInBook) {
            if (file.endsWith(".xml")) {
                assertTrue("Unexpected prefix for transcription. (" + file + ")",
                        file.startsWith("PrincetonRB16th11.aor."));
            }
        }

        // Load file maps
        Map<String, FileMap> filemaps = loadFileMaps();

        int folgers_count = 0;
        int princeton_count = 0;
        // Make sure all <internal_ref>s have been changed appropriately
        List<AnnotatedPage> pages = getPages(BOOK_BUCHANAN);
        for (AnnotatedPage page : pages) {
            assertNotNull("NULL page.", page);

            for (Marginalia marg : page.getMarginalia()) {
                for (MarginaliaLanguage lang : marg.getLanguages()) {
                    for (Position pos : lang.getPositions()) {
                        for (InternalReference ref : pos.getInternalRefs()) {
                            for (ReferenceTarget target : ref.getTargets()) {
//                                printTarget(target);
                                assertNotNull("Target book id is NULL.", target.getBookId());
                                assertFalse("Target book id is empty.", target.getBookId().isEmpty());
                                assertNotNull("Target filename is NULL.", target.getFilename());
                                assertFalse("Target filename is empty.", target.getFilename().isEmpty());

                                assertTrue("Unexpected Book ID found. (" + target.getBookId() + ")",
                                        filemaps.get(DIRECTORY_MAP).getMap().containsValue(target.getBookId()));
                                assertTrue("Unexpected filename found. (" + target.getFilename() + ")",
                                        target.getFilename().startsWith("PrincetonRB16th11.aor.") &&
                                                target.getFilename().endsWith(".xml"));

                                if (target.getBookId().startsWith("Folgers")) {
                                    folgers_count++;
                                } else if (target.getBookId().startsWith("Princeton")) {
                                    princeton_count++;
                                } else {
                                    fail("Unexpected book id found. (" + target.getBookId() + ")");
                                }
                            }
                        }
                    }
                }
            }
        }

        assertEquals("Unexpected number of Folgers references", 0, folgers_count);
        assertEquals("Unexpected number of PrincetonRB16th11 references", 2, princeton_count);
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

    private Map<String, FileMap> loadFileMaps() throws IOException {
        Map<String, FileMap> map = new HashMap<>();

        List<String> errors = new ArrayList<>();

        ByteStreamGroup coll_streams = base.getByteStreamGroup(VALID_COLLECTION);
        try (InputStream in_dom = coll_streams.getByteStreamGroup(BOOK_DOMENICHI).getByteStream("filemap.csv");
             InputStream in_buc = coll_streams.getByteStreamGroup(BOOK_BUCHANAN).getByteStream("filemap.csv");
             InputStream in_dir_map = getClass().getClassLoader().getResourceAsStream("rosa/archive/dir-map.csv")) {

            map.put(BOOK_DOMENICHI, serializers.getSerializer(FileMap.class).read(in_dom, errors));
            map.put(BOOK_BUCHANAN, serializers.getSerializer(FileMap.class).read(in_buc, errors));
            map.put(DIRECTORY_MAP, serializers.getSerializer(FileMap.class).read(in_dir_map, errors));

        }

        assertTrue("Unexpected errors found while loading file maps.", errors.isEmpty());

        return map;
    }

    // For debugging
    private void printTarget(ReferenceTarget target) {
        System.out.println(
                "<target book_id=\""
                        + target.getBookId()
                        + "\" filename=\""
                        + target.getFilename()
                        + "\" text=\""
                        + target.getText()
                        + "\" />"
        );
    }

}
