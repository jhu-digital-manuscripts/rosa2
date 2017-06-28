package rosa.archive.core;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import rosa.archive.core.GuiceJUnitRunner.GuiceModules;
import rosa.archive.core.check.BookChecker;
import rosa.archive.core.check.BookCollectionChecker;
import rosa.archive.core.serialize.AORAnnotatedPageSerializer;
import rosa.archive.core.serialize.BookDescriptionSerializer;
import rosa.archive.core.serialize.BookMetadataSerializer;
import rosa.archive.core.serialize.BookReferenceSheetSerializer;
import rosa.archive.core.serialize.BookStructureSerializer;
import rosa.archive.core.serialize.CharacterNamesSerializer;
import rosa.archive.core.serialize.CropInfoSerializer;
import rosa.archive.core.serialize.FileMapSerializer;
import rosa.archive.core.serialize.IllustrationTaggingSerializer;
import rosa.archive.core.serialize.IllustrationTitlesSerializer;
import rosa.archive.core.serialize.ImageListSerializer;
import rosa.archive.core.serialize.MultilangMetadataSerializer;
import rosa.archive.core.serialize.NarrativeSectionsSerializer;
import rosa.archive.core.serialize.NarrativeTaggingSerializer;
import rosa.archive.core.serialize.PermissionSerializer;
import rosa.archive.core.serialize.ReferenceSheetSerializer;
import rosa.archive.core.serialize.SHA1ChecksumSerializer;
import rosa.archive.core.serialize.SerializerSet;
import rosa.archive.core.serialize.TranscriptionXmlSerializer;
import rosa.archive.model.Book;
import rosa.archive.model.BookCollection;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Like the base class for general archive tests, but more useful for searching.
 * Since searching will not change the archive state, this provides a static Store
 * that can be indexed once per test CLASS instead of per test METHOD for test
 * performance gains.
 *
 * {@link BaseArchiveTest}
 */
@RunWith(GuiceJUnitRunner.class)
@GuiceModules({ ArchiveCoreModule.class })
public abstract class BaseSearchTest {

    protected final static String VALID_COLLECTION = "valid";
    protected final static String VALID_BOOK_FOLGERSHA2 = "FolgersHa2";
    protected final static String VALID_BOOK_LUDWIGXV7 = "LudwigXV7";

    @ClassRule
    public static TemporaryFolder tempFolder = new TemporaryFolder();

    protected static ByteStreamGroup base;
    protected static Path basePath;
    protected static StoreImpl store;
    protected static SimpleStore simpleStore;

    @BeforeClass
    public static void setupArchiveStore() throws Exception {
        Path tmp = tempFolder.newFolder().toPath();

        ResourceUtil.copyResource(BaseSearchTest.class, "/archive", tmp);

        basePath = tmp.resolve("archive");
        base = new FSByteStreamGroup(basePath);

        SerializerSet serializers = new SerializerSet(new HashSet<>(Arrays.asList(
                new BookMetadataSerializer(),
                new BookStructureSerializer(),
                new CharacterNamesSerializer(),
                new SHA1ChecksumSerializer(),
                new CropInfoSerializer(),
                new IllustrationTaggingSerializer(),
                new IllustrationTitlesSerializer(),
                new ImageListSerializer(),
                new NarrativeSectionsSerializer(),
                new NarrativeTaggingSerializer(),
                new TranscriptionXmlSerializer(),
                new PermissionSerializer(),
                new MultilangMetadataSerializer(),
                new AORAnnotatedPageSerializer(),
                new ReferenceSheetSerializer(),
                new BookReferenceSheetSerializer(),
                new FileMapSerializer(),
                new BookDescriptionSerializer()
        )));

        BookChecker bookChecker = new BookChecker(serializers);
        BookCollectionChecker bookCollectionChecker = new BookCollectionChecker(serializers);

        store = new StoreImpl(serializers, bookChecker, bookCollectionChecker, base);
        simpleStore = new SimpleCachingStore(store, 1000);
    }

    protected BookCollection loadCollection(String name) throws IOException {
        List<String> errors = new ArrayList<>();

        BookCollection result = store.loadBookCollection(name, errors);

        assertNotNull(result);
        assertEquals(0, errors.size());

        return result;
    }

    protected Book loadBook(String collection, String book) throws IOException {
        List<String> errors = new ArrayList<>();

        Book result = store.loadBook(loadCollection(collection), book, errors);

        assertNotNull(result);
        assertEquals(0, errors.size());

        return result;
    }

    protected BookCollection loadValidCollection() throws IOException {
        return loadCollection(VALID_COLLECTION);
    }

    protected Book loadValidCollectionBook(String name) throws IOException {
        return loadBook(VALID_COLLECTION, name);
    }

    protected Book loadValidFolgersHa2() throws IOException {
        return loadBook(VALID_COLLECTION, VALID_BOOK_FOLGERSHA2);
    }

    protected Book loadValidLudwigXV7() throws IOException {
        return loadBook(VALID_COLLECTION, VALID_BOOK_LUDWIGXV7);
    }

    public Path getCollectionPath(String collection) {
        return basePath.resolve(collection);
    }

    public Path getBookPath(String collection, String book) {
        return basePath.resolve(collection).resolve(book);
    }

    protected void removeBookFile(String collection, String book, String name) throws IOException {
        Files.deleteIfExists(basePath.resolve(collection).resolve(book).resolve(name));
    }

    protected void removeCollectionFile(String collection, String name) throws IOException {
        Files.deleteIfExists(basePath.resolve(collection).resolve(name));
    }

}
