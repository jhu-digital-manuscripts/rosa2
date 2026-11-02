package rosa.archive.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import rosa.archive.iiif.IIIFJsonWriter;
import rosa.archive.iiif.IIIFPresentationGenerator;
import rosa.archive.model.BookCollection;
import rosa.archive.opensearch.OpensearchIngestGenerator;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * A shallow copy keeps everything the generators need, so it regenerates the same output.
 */
class FileSystemArchiveStoreShallowCopyTest {

    @Test
    void shallowCopyRegeneratesIdenticalOutput(@TempDir Path tempDir) throws Exception {
        Path archive = Path.of(getClass().getClassLoader().getResource("archive").toURI());
        Path copy = tempDir.resolve("copy");
        new FileSystemArchiveStore(archive).shallowCopy(copy);

        // Files the generators depend on beyond xml/txt/html/csv
        assertTrue(Files.exists(copy.resolve("rose").resolve("config.properties")));
        assertTrue(Files.exists(copy.resolve("rose").resolve("annos.jsonld")));
        assertTrue(Files.exists(copy.resolve("rose").resolve("rosecollection.SHA1SUM")));
        assertTrue(Files.exists(copy.resolve("rose").resolve("Douce195").resolve("Douce195.SHA1SUM")));

        Path fromArchive = generate(archive, tempDir.resolve("a"));
        Path fromCopy = generate(copy, tempDir.resolve("b"));
        assertSameTree(fromArchive, fromCopy);

        // Copying again over the existing copy must not fail
        new FileSystemArchiveStore(archive).shallowCopy(copy);
    }

    @Test
    void shallowCopySkipsIgnoredDirectories(@TempDir Path tempDir) throws IOException {
        Path archive = tempDir.resolve("archive");
        Files.createDirectories(archive.resolve("rose.ignore").resolve("Book"));
        Files.createDirectories(archive.resolve("biblehistoriale").resolve("Book"));
        Files.createDirectories(archive.resolve("rose").resolve("Book.ignore"));
        Files.createDirectories(archive.resolve("rose").resolve("Book"));
        for (String f : List.of("rose.ignore/Book/Book.images.csv", "biblehistoriale/Book/Book.images.csv",
                "rose/Book.ignore/Book.images.csv", "rose/Book/Book.images.csv", "rose/config.properties")) {
            Files.writeString(archive.resolve(f), "x");
        }

        Path copy = tempDir.resolve("copy");
        new FileSystemArchiveStore(archive).shallowCopy(copy);

        assertTrue(Files.exists(copy.resolve("rose").resolve("Book").resolve("Book.images.csv")));
        assertTrue(Files.exists(copy.resolve("rose").resolve("config.properties")));
        assertFalse(Files.exists(copy.resolve("rose.ignore")));
        assertFalse(Files.exists(copy.resolve("biblehistoriale")));
        assertFalse(Files.exists(copy.resolve("rose").resolve("Book.ignore")));
    }

    @Test
    void collectionChecksumFileIsLoaded() throws Exception {
        Path archive = Path.of(getClass().getClassLoader().getResource("archive").toURI());
        List<String> errors = new ArrayList<>();
        BookCollection rose = new FileSystemArchiveStore(archive).loadCollection("rose", errors);
        assertNotNull(rose.getChecksum(), "rosecollection.SHA1SUM should be read");
        assertTrue(rose.getChecksum().checksums().containsKey("character_names.csv"));
    }

    private static Path generate(Path archive, Path out) throws IOException {
        FileSystemArchiveStore store = new FileSystemArchiveStore(archive);
        new IIIFPresentationGenerator(new IIIFJsonWriter())
                .generate(store, out.resolve("iiif"), "http://example.org", null, 2, "http://search.example.org/_search");
        new OpensearchIngestGenerator().generate(store, out.resolve("bulk"));
        return out;
    }

    private static void assertSameTree(Path a, Path b) throws IOException {
        List<Path> filesA;
        try (Stream<Path> s = Files.walk(a)) {
            filesA = s.filter(Files::isRegularFile).map(a::relativize).sorted().toList();
        }
        List<Path> filesB;
        try (Stream<Path> s = Files.walk(b)) {
            filesB = s.filter(Files::isRegularFile).map(b::relativize).sorted().toList();
        }
        assertEquals(filesA, filesB, "same set of generated files");
        for (Path rel : filesA) {
            assertEquals(-1L, Files.mismatch(a.resolve(rel), b.resolve(rel)), "differs: " + rel);
        }
    }
}
