package rosa.archive.iiif;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import rosa.archive.core.FileSystemArchiveStore;
import rosa.archive.model.Book;
import rosa.archive.model.BookDescription;

/**
 * The English TEI book description is copied next to the manifest as description.xml
 * and referenced by the manifest's seeAlso, but only when its notesStmt has content.
 */
class BookDescriptionPublishingTest {

    private static final String NO_NOTES_TEI = """
            <?xml version="1.0" encoding="UTF-8"?>
            <TEI xmlns="http://www.tei-c.org/ns/1.0">
              <teiHeader><fileDesc>
                <titleStmt><title>Roman de la Rose</title></titleStmt>
                <sourceDesc><bibl><title>Roman de la Rose</title></bibl></sourceDesc>
              </fileDesc></teiHeader>
            </TEI>
            """;

    private final ObjectMapper mapper = new ObjectMapper();

    /** Copies the test archive so a fixture can be changed without touching the shared one. */
    private static Path copyArchive(Path target) throws IOException, URISyntaxException {
        Path source = Path.of(BookDescriptionPublishingTest.class.getClassLoader()
                .getResource("archive").toURI());
        try (Stream<Path> paths = Files.walk(source)) {
            for (Path p : (Iterable<Path>) paths::iterator) {
                Path dest = target.resolve(source.relativize(p).toString());
                if (Files.isDirectory(p)) {
                    Files.createDirectories(dest);
                } else {
                    Files.copy(p, dest);
                }
            }
        }
        return target;
    }

    @Test
    void publishesDescriptionAndSeeAlsoOnlyForBooksWithNotes(@TempDir Path tmp) throws Exception {
        Path archive = copyArchive(tmp.resolve("archive"));
        // Douce195: a description without notesStmt
        Files.writeString(archive.resolve("rose/Douce195/Douce195.description_en.xml"), NO_NOTES_TEI);

        Path out = tmp.resolve("out");
        new IIIFPresentationGenerator(new IIIFJsonWriter())
                .generate(new FileSystemArchiveStore(archive), out, "http://example.org", null, 2);

        // Douce332: copied byte for byte, and referenced from the manifest
        Path published = out.resolve("rose/Douce332/description.xml");
        assertTrue(Files.exists(published));
        assertArrayEquals(Files.readAllBytes(archive.resolve("rose/Douce332/Douce332.description_en.xml")),
                Files.readAllBytes(published));

        JsonNode manifest = mapper.readTree(out.resolve("rose/Douce332/manifest.json").toFile());
        JsonNode seeAlso = manifest.get("seeAlso");
        assertEquals(1, seeAlso.size());
        JsonNode entry = seeAlso.get(0);
        assertEquals("http://example.org/rose/Douce332/description.xml", entry.get("id").asText());
        assertEquals("Dataset", entry.get("type").asText());
        assertEquals("application/tei+xml", entry.get("format").asText());
        assertEquals("http://www.tei-c.org/ns/1.0", entry.get("profile").asText());
        assertEquals("Book description (TEI)", entry.get("label").get("en").get(0).asText());

        // pizan/Francais1182's fixture description has no notesStmt either
        assertFalse(Files.exists(out.resolve("pizan/Francais1182/description.xml")));
        assertNull(mapper.readTree(out.resolve("pizan/Francais1182/manifest.json").toFile()).get("seeAlso"));

        // Douce195: no notes, so neither file nor seeAlso
        assertFalse(Files.exists(out.resolve("rose/Douce195/description.xml")));
        JsonNode douce195 = mapper.readTree(out.resolve("rose/Douce195/manifest.json").toFile());
        assertNull(douce195.get("seeAlso"));

        // AoR books have no descriptions
        try (Stream<Path> files = Files.walk(out.resolve("aor"))) {
            assertTrue(files.noneMatch(p -> p.getFileName().toString().equals("description.xml")));
        }
    }

    @Test
    void hasDisplayableDescriptionRequiresNonBlankEnglishNotes() {
        Book book = new Book();
        assertFalse(IIIFPresentationGenerator.hasDisplayableDescription(book));

        BookDescription empty = new BookDescription();
        empty.setId("X.description_en.xml");
        book.addDescription(empty, "en");
        assertFalse(IIIFPresentationGenerator.hasDisplayableDescription(book));

        BookDescription french = new BookDescription();
        french.setId("X.description_fr.xml");
        french.addNote("IDENTIFICATION", "Manuscrit");
        Book frenchOnly = new Book();
        frenchOnly.addDescription(french, "fr");
        assertFalse(IIIFPresentationGenerator.hasDisplayableDescription(frenchOnly));

        empty.addNote("IDENTIFICATION", "MS Douce 332");
        assertTrue(IIIFPresentationGenerator.hasDisplayableDescription(book));
    }
}
