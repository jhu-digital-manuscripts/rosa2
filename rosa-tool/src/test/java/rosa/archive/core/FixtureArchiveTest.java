package rosa.archive.core;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import rosa.archive.model.Book;
import rosa.archive.model.BookCollection;
import rosa.archive.model.BookImage;
import rosa.archive.model.BookMetadata;
import rosa.archive.model.CropData;
import rosa.archive.model.NarrativeScene;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Loads the fixture archive in src/test/resources/archive and checks what each kind of file
 * contributes to the model. Together with the generator tests this gives every fixture file
 * an assertion, so a file that no test would miss does not belong in the fixture.
 */
class FixtureArchiveTest {

    private static FileSystemArchiveStore store;

    @BeforeAll
    static void openStore() throws Exception {
        store = new FileSystemArchiveStore(
                Path.of(FixtureArchiveTest.class.getClassLoader().getResource("archive").toURI()));
    }

    @Test
    void holdsTheExpectedCollectionsAndBooksAndLoadsCleanly() throws Exception {
        assertEquals(List.of("aor", "dlmm", "pizan", "rose"), store.listCollections().stream().sorted().toList());
        assertEquals(List.of("PrincetonU101"), store.listBooks("aor"));
        assertEquals(List.of(), store.listBooks("dlmm"));
        assertEquals(List.of("Francais1182"), store.listBooks("pizan"));
        assertEquals(List.of("Douce195", "Douce332"), store.listBooks("rose").stream().sorted().toList());

        List<String> errors = new ArrayList<>();
        for (String collectionId : store.listCollections()) {
            BookCollection collection = store.loadCollection(collectionId, errors);
            for (String bookId : store.listBooks(collectionId)) {
                store.loadBook(collection, bookId, errors);
            }
        }
        // The only problem is the id_locations.csv row kept to test the reader's error reporting
        assertEquals(1, errors.size(), errors.toString());
        assertTrue(errors.get(0).startsWith("id_locations.csv line 2:"), errors.get(0));
    }

    @Test
    void collectionConfigIsRead() throws Exception {
        BookCollection aor = store.loadCollection("aor");
        assertEquals("Archaeology of Reading", aor.getLabel());
        assertArrayEquals(new String[] {"en"}, aor.getAllSupportedLanguages());

        BookCollection pizan = store.loadCollection("pizan");
        assertEquals("Christine de Pizan Digital Scriptorium", pizan.getLabel());
        assertArrayEquals(new String[] {"dlmm"}, pizan.getParentCollections());

        BookCollection rose = store.loadCollection("rose");
        assertArrayEquals(new String[] {"en", "fr"}, rose.getAllSupportedLanguages());
        assertEquals(329, rose.getMissingImage().getWidth());
        assertEquals(446, rose.getMissingImage().getHeight());
    }

    @Test
    void aorReferenceSheetsAreRead() throws Exception {
        BookCollection aor = store.loadCollection("aor");

        // Names used by the PrincetonU101 pages
        assertEquals(List.of("Sextus Julius Frontinus"), aor.getPeopleRef().getAlternates("Frontinus"));
        assertTrue(aor.getPeopleRef().getKeys().contains("Machiavelli"));
        assertEquals(List.of("Art of Warre", "Dell'arte della guerra"), aor.getBooksRef().getAlternates("Art of War"));
        assertEquals(List.of("Machiavelli"), aor.getBooksRef().getAuthors("Art of War"));
        assertEquals(List.of("Alto velo", "Altovelo"), aor.getLocationsRef().getAlternates("Alto Velo Island"));
    }

    @Test
    void roseNarrativeSectionsAreRead() throws Exception {
        List<NarrativeScene> scenes = store.loadCollection("rose").getNarrativeSections().asScenes();
        assertFalse(scenes.isEmpty());
        assertEquals("G 1a", scenes.get(0).getId());
        assertEquals("Preface", scenes.get(0).getDescription());
        assertEquals(1, scenes.get(0).getCriticalEditionStart());
        assertEquals(20, scenes.get(0).getCriticalEditionEnd());
    }

    @Test
    void aorBookIsRead() throws Exception {
        Book book = store.loadBook(store.loadCollection("aor"), "PrincetonU101");

        BookMetadata metadata = book.getBookMetadata();
        assertEquals(1573, metadata.getYearStart());
        assertEquals(124, metadata.getNumberOfPages());
        assertEquals("http://creativecommons.org/licenses/by/4.0/", metadata.getLicenseUrl());
        assertEquals("Art of warre (London, 1573)", book.getBiblioData("en").getTitle());
        assertEquals("Gabriel Harvey", book.getBiblioData("en").getReaders()[0].getName());

        assertEquals(260, book.getImages().getImages().size());
        // Only the pages the generator tests look at are kept
        assertEquals(8, book.getAnnotatedPages().size());
    }

    @Test
    void manuscriptBooksAreRead() throws Exception {
        for (String[] id : new String[][] {{"rose", "Douce195"}, {"rose", "Douce332"}, {"pizan", "Francais1182"}}) {
            BookCollection collection = store.loadCollection(id[0]);
            Book book = store.loadBook(collection, id[1]);
            String name = id[1];

            assertNotNull(book.getBookMetadata(), name);
            assertFalse(book.getBiblioData("en").getTitle().isBlank(), name);
            assertFalse(book.getBiblioData("fr").getTitle().isBlank(), name);
            assertFalse(book.getImages().getImages().isEmpty(), name);
            for (String lang : collection.getAllSupportedLanguages()) {
                assertFalse(book.getPermission(lang).getPermission().isBlank(), name + " permission " + lang);
                // The pizan descriptions have no notesStmt, which BookDescriptionPublishingTest relies on
                assertEquals(id[0].equals("pizan"), book.getDescription(lang).getFullText().isBlank(),
                        name + " description " + lang);
            }
        }
    }

    @Test
    void douce195DetailsAreRead() throws Exception {
        Book book = store.loadBook(store.loadCollection("rose"), "Douce195");

        BookMetadata metadata = book.getBookMetadata();
        assertEquals(1450, metadata.getYearStart());
        assertEquals(1500, metadata.getYearEnd());
        assertEquals(125, metadata.getNumberOfIllustrations());
        assertEquals("Roman de la Rose", metadata.getBookTexts().get(0).getTitle());
        assertEquals(2, metadata.getBookTexts().get(0).getColumnsPerPage());

        assertTrue(book.getPermission("en").getPermission().contains("Bodleian Library, University of Oxford"));
        assertTrue(book.getPermission("fr").getPermission().contains("Bodleian"));

        assertNotNull(book.getChecksum(), "Douce195.SHA1SUM should be read");
        assertEquals("9a4aee9c0f6190439ebc68fac852b97def6da5d5", book.getChecksum().checksums().get("Douce195.images.csv"));
    }

    @Test
    void cropDataIsRead() throws Exception {
        BookCollection rose = store.loadCollection("rose");
        for (String bookId : List.of("Douce195", "Douce332")) {
            Book book = store.loadBook(rose, bookId);
            List<BookImage> images = book.getImages().getImages();
            List<BookImage> cropped = book.getCroppedImages().getImages();

            // A cropped list names the same images in the same order, with smaller dimensions
            assertEquals(images.size(), cropped.size(), bookId);
            BookImage pastedown = cropped.get(1);
            assertEquals(bookId + ".frontmatter.pastedown.tif", pastedown.getId());
            assertEquals(images.get(1).getId(), pastedown.getId());
            assertTrue(pastedown.getWidth() < images.get(1).getWidth(), bookId);

            assertNotNull(book.getCropInfo().getCropDataForPage(pastedown.getId()), bookId);
        }

        Book douce195 = store.loadBook(rose, "Douce195");
        assertEquals(4016, douce195.getCroppedImages().getImages().get(1).getWidth());
        assertEquals(5808, douce195.getCroppedImages().getImages().get(1).getHeight());
        CropData crop = douce195.getCropInfo().getCropDataForPage("Douce195.frontmatter.pastedown.tif");
        assertEquals(0.039261, crop.getLeft(), 1e-9);
        assertEquals(0.115473, crop.getRight(), 1e-9);
        assertEquals(0.054167, crop.getTop(), 1e-9);
        assertEquals(0.063333, crop.getBottom(), 1e-9);
    }
}
