package rosa.archive.aor;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link AoRTranscriptionChecker}: internal references resolved through
 * id_locations.csv, cross-reference names checked against the sheets, and the XML
 * well-formedness and schema checks.
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
        List<String> errors = new AoRTranscriptionChecker(archive(dir, true), null).run("TestCol", null).errors();
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
        List<String> errors = new AoRTranscriptionChecker(archive(dir, false), null).run("TestCol", null).errors();
        assertTrue(errors.contains("[Book/Book.aor.001r.xml] Internal reference target not found: Book:00000002"), errors.toString());
        assertTrue(errors.stream().noneMatch(e -> e.contains("not resolved")), errors.toString());
        assertTrue(errors.stream().noneMatch(e -> e.contains("Book:00000002:m0001")), errors.toString());
    }

    @Test
    void reportsFilesThatAreNotWellFormedOnce(@TempDir Path dir) throws Exception {
        Path archive = archive(dir, false);
        Path book = archive.resolve("TestCol").resolve("Book");
        Files.writeString(book.resolve("Book.images.csv"), "Book.003r.tif,100,100\nBook.004r.tif,100,100\n",
                java.nio.file.StandardOpenOption.APPEND);
        Files.writeString(book.resolve("Book.aor.003r.xml"), PAGE_HEAD.formatted("Book.003r.tif")
                + "<marginalia hand=\"Italian\">\n" + PAGE_TAIL);
        Files.writeString(book.resolve("Book.aor.004r.xml"), """
                <?xml version="1.0"?>
                <!DOCTYPE transcription>
                <transcription><page filename="Book.004r.tif"/></transcription>
                """);

        List<String> errors = new AoRTranscriptionChecker(archive, null).run("TestCol", null).errors();

        List<String> broken = errors.stream().filter(e -> e.startsWith("[Book/Book.aor.003r.xml]")).toList();
        assertEquals(1, broken.size(), errors.toString());
        assertTrue(broken.getFirst().startsWith("[Book/Book.aor.003r.xml] Fatal (6:"), broken.toString());
        List<String> doctype = errors.stream().filter(e -> e.startsWith("[Book/Book.aor.004r.xml]")).toList();
        assertEquals(1, doctype.size(), errors.toString());
        assertTrue(doctype.getFirst().contains("DOCTYPE"), doctype.toString());
        // The parser's own failure to read the file is not reported on top
        assertTrue(errors.stream().noneMatch(e -> e.contains("Failed to parse AoR XML")), errors.toString());
    }

    @Test
    void validatesAgainstSchemaWhenGiven(@TempDir Path dir) throws Exception {
        Path archive = archive(dir, false);
        Path schema = dir.resolve("aor.xsd");
        Files.writeString(schema, """
                <?xml version="1.0"?>
                <xs:schema xmlns:xs="http://www.w3.org/2001/XMLSchema">
                  <xs:element name="transcription">
                    <xs:complexType>
                      <xs:sequence>
                        <xs:element name="page">
                          <xs:complexType>
                            <xs:attribute name="filename" type="xs:string" use="required"/>
                            <xs:attribute name="reader" type="xs:string" use="required"/>
                            <xs:attribute name="pagination" type="xs:string" use="required"/>
                          </xs:complexType>
                        </xs:element>
                        <xs:any processContents="skip" minOccurs="0" maxOccurs="unbounded"/>
                      </xs:sequence>
                    </xs:complexType>
                  </xs:element>
                </xs:schema>
                """);

        List<String> errors = new AoRTranscriptionChecker(archive, null, schema).run("TestCol", null).errors();

        // Every page lacks the pagination attribute the schema requires
        for (String file : List.of("Book.aor.001r.xml", "Book.aor.002r.xml")) {
            assertTrue(errors.stream().anyMatch(e -> e.startsWith("[Book/" + file + "] Error (")
                    && e.contains("pagination")), errors.toString());
        }
        // Invalid files are still parsed and checked against the sheets
        assertTrue(errors.contains("[Book/Book.aor.001r.xml] Unknown cross-reference person: Rabelais"), errors.toString());

        Files.writeString(schema, "<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\">");
        errors = new AoRTranscriptionChecker(archive, null, schema).run("TestCol", null).errors();
        assertTrue(errors.stream().anyMatch(e -> e.startsWith("Cannot read schema [")), errors.toString());
        assertTrue(errors.stream().noneMatch(e -> e.contains("pagination")), errors.toString());
    }

    @Test
    void hintsAtStandardNamesAndReportsSheetAndImageListProblems(@TempDir Path dir) throws Exception {
        Path collection = dir.resolve("archive").resolve("TestCol");
        Path book = collection.resolve("Book");
        Files.createDirectories(book);
        Files.writeString(collection.resolve("people.csv"), "sci,Alternative name (1)\nPlutarch,Plutarchus\nPlutarch\n");
        Files.writeString(collection.resolve("books.csv"), "Standard ID\nEclogues,Bucolics\n");
        Files.writeString(collection.resolve("locations.csv"), "Rome,Roma\n");
        Files.writeString(book.resolve("Book.images.csv"), "Book.001r.tif,100,100\n");
        // Names with surrounding whitespace are trimmed before they are looked up
        Files.writeString(book.resolve("Book.aor.001r.xml"), PAGE_HEAD.formatted(" Book.001r.tif ")
                + marginalia(null, "<person name=\"Plutarchus\"/><person name=\" Plutarch \"/>"
                        + "<book title=\"Bucolics\"/><location name=\"Roma\"/><location name=\"Rome\"/>"
                        + "<X-ref person=\"Plutarchus\" book_title=\"Bucolics\"/>")
                + PAGE_TAIL);
        Files.writeString(book.resolve("Book.aor.002r.xml"), PAGE_HEAD.formatted("Book.002r.tif") + PAGE_TAIL);

        List<String> errors = new AoRTranscriptionChecker(dir.resolve("archive"), null).run("TestCol", null).errors();

        String context = "[Book/Book.aor.001r.xml]";
        String plutarch = " (alternate of 'Plutarch'; use the standard name)";
        String eclogues = " (alternate of 'Eclogues'; use the standard name)";
        assertTrue(errors.contains(context + " Unknown person reference: Plutarchus" + plutarch), errors.toString());
        assertTrue(errors.contains(context + " Unknown book reference: Bucolics" + eclogues), errors.toString());
        assertTrue(errors.contains(context + " Unknown location reference: Roma (alternate of 'Rome'; use the standard name)"),
                errors.toString());
        assertTrue(errors.contains(context + " Unknown cross-reference person: Plutarchus" + plutarch), errors.toString());
        assertTrue(errors.contains(context + " Unknown cross-reference title: Bucolics" + eclogues), errors.toString());
        assertTrue(errors.contains("Duplicate key 'Plutarch' in people.csv"), errors.toString());
        assertTrue(errors.contains("[Book/Book.aor.002r.xml] No image 'Book.002r.tif' in Book.images.csv for this transcription"),
                errors.toString());
        assertEquals(7, errors.size(), errors.toString());
    }
}
