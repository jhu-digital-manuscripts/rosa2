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
import rosa.archive.model.aor.AnnotatedPage;
import rosa.archive.model.aor.Annotation;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(GuiceJUnitRunner.class)
@GuiceModules({ ArchiveCoreModule.class })
public class HamletTest {
    private static String COLLECTION = "collection";
    private static String BOOK_HAMLET = "Hamlet";

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Inject
    protected SerializerSet serializers;

    @Inject
    protected BookChecker bookChecker;

    @Inject
    protected BookCollectionChecker collectionChecker;

    protected ByteStreamGroup base;
    protected StoreImpl store;

    /**
     * Copy files over to a temp directory, setup Store
     */
    @Before
    public void setup() throws IOException {
        Path tmp = tempFolder.newFolder().toPath();

        ResourceUtil.copyResource(getClass(), "/data", tmp);

        Path basePath = tmp.resolve("data");
        base = new FSByteStreamGroup(basePath);
        store = new StoreImpl(serializers, bookChecker, collectionChecker, base);
    }

    @Test
    public void testLoadHamlet() throws Exception {
        List<AnnotatedPage> pages = getPages(BOOK_HAMLET);

        assertNotNull(pages);
        assertEquals("2 page were expected.", 2, pages.size());

        List<Annotation> annos = pages.get(1).getAnnotations();
        assertEquals("15 annotations expected on sample page.", 15, annos.size());
    }

    private List<AnnotatedPage> getPages(String book) throws IOException {
        assertTrue("Book not found.", base.getByteStreamGroup(COLLECTION).hasByteStreamGroup(book));

        List<AnnotatedPage> list = new ArrayList<>();

        ByteStreamGroup bookStreams = base.getByteStreamGroup(COLLECTION).getByteStreamGroup(book);
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

}
