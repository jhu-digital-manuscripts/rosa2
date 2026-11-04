package rosa.archive.aor;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link AoRTranscriptionChecker}: internal references resolved through
 * id_locations.csv and cross-reference names checked against the sheets.
 */
class AoRTranscriptionCheckerTest {

    private static final String PAGE_HEAD = """
            <?xml version="1.0" encoding="UTF-8"?>
            <transcription>
              <page filename="%s" reader="Reader"/>
              <annotation>
            """;
    private static final String PAGE_TAIL = """
              </annotation>
            </transcription>
            """;

    private static String marginalia(String id, String body) {
        return "<marginalia hand=\"Italian\"" + (id == null ? "" : " id=\"" + id + "\"") + ">"
                + "<language ident=\"LA\"><position place=\"intext\" book_orientation=\"0\">"
                + "<marginalia_text>nota</marginalia_text>" + body
                + "</position></language></marginalia>\n";
    }

    private static Path archive(Path dir, boolean withIdLocations) throws Exception {
        Path collection = dir.resolve("archive").resolve("TestCol");
        Path book = collection.resolve("Book");
        Files.createDirectories(book);
        Files.writeString(collection.resolve("people.csv"), "sci,Alternative name (1)\nPlutarch\n");
        Files.writeString(collection.resolve("books.csv"), "Standard ID\nMoralia\n");
        Files.writeString(collection.resolve("locations.csv"), "");
        if (withIdLocations) {
            Files.writeString(collection.resolve("id_locations.csv"), """
                    Book:00000001,TestCol,Book,1r,
                    Book:00000002,TestCol,Book,2r,
                    Book:00000002:m0001,TestCol,Book,2r,Book:00000002:m0001
                    Book:00000009,TestCol,Book,9r,
                    """);
        }
        Files.writeString(book.resolve("Book.images.csv"), "Book.001r.tif,100,100\nBook.002r.tif,100,100\n");
        Files.writeString(book.resolve("Book.aor.001r.xml"), PAGE_HEAD.formatted("Book.001r.tif")
                + marginalia(null, "<X-ref person=\"Plutarch\" book_title=\"Moralia\" text=\"x\"/>"
                        + "<X-ref person=\"Rabelais\" book_title=\"Pantagruel\"/>"
                        + "<internal_ref><target ref=\"Book:00000002\" text=\"2\"/></internal_ref>"
                        + "<internal_ref><target ref=\"Book:00000002:m0001\" text=\"2\"/></internal_ref>"
                        + "<internal_ref><target ref=\"Book:00000009\" text=\"9\"/></internal_ref>"
                        + "<internal_ref><target ref=\"Other:1\" text=\"o\"/></internal_ref>"
                        + "<internal_ref><target ref=\"\" text=\"e\"/></internal_ref>")
                + PAGE_TAIL);
        // The annotation Book:00000002:m0001 is on page 1r, while id_locations.csv puts it on 2r
        Files.writeString(book.resolve("Book.aor.002r.xml"), PAGE_HEAD.formatted("Book.002r.tif")
                + marginalia("Book:00000001:m0001", "") + PAGE_TAIL);
        Files.writeString(book.resolve("Book.aor.001r.xml"), Files.readString(book.resolve("Book.aor.001r.xml"))
                .replace("<marginalia hand=\"Italian\">", "<marginalia hand=\"Italian\" id=\"Book:00000002:m0001\">"));
        return dir.resolve("archive");
    }

    @Test
    void resolvesTargetsThroughIdLocationsAndChecksCrossReferences(@TempDir Path dir) throws Exception {
        List<String> errors = new AoRTranscriptionChecker(archive(dir, true), null).run("TestCol", null);
        String context = "[Book/Book.aor.001r.xml]";

        assertTrue(errors.contains(context + " Unknown cross-reference person: Rabelais"), errors.toString());
        assertTrue(errors.contains(context + " Unknown cross-reference title: Pantagruel"), errors.toString());
        assertTrue(errors.contains(context + " Internal reference target not resolved: 'Book:00000009' "
                + "(id_locations.csv page '9r' is not an image of Book)"), errors.toString());
        assertTrue(errors.contains(context + " Internal reference target not resolved: 'Other:1' (unknown book 'Other')"), errors.toString());
        assertTrue(errors.contains(context + " Internal reference target not resolved: '' (empty ref)"), errors.toString());
        assertTrue(errors.stream().anyMatch(e -> e.startsWith(context + " Internal reference target 'Book:00000002:m0001' is at")
                && e.endsWith("but id_locations.csv puts it on Book 002r")), errors.toString());

        // The resolvable target and the known names are not reported
        assertTrue(errors.stream().noneMatch(e -> e.contains("'Book:00000002'")), errors.toString());
        assertTrue(errors.stream().noneMatch(e -> e.contains("Plutarch") || e.contains("Moralia")), errors.toString());
        assertEquals(6, errors.size(), errors.toString());
    }

    @Test
    void withoutIdLocationsTargetsAreCheckedAgainstKnownIds(@TempDir Path dir) throws Exception {
        List<String> errors = new AoRTranscriptionChecker(archive(dir, false), null).run("TestCol", null);
        assertTrue(errors.contains("[Book/Book.aor.001r.xml] Internal reference target not found: Book:00000002"), errors.toString());
        assertTrue(errors.stream().noneMatch(e -> e.contains("not resolved")), errors.toString());
        assertTrue(errors.stream().noneMatch(e -> e.contains("Book:00000002:m0001")), errors.toString());
    }
}
