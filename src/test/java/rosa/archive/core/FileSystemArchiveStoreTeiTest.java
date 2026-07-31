package rosa.archive.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Additional TEI generation tests focusing on output naming, multi-page handling,
 * structural completeness, and edge cases not covered by FileSystemArchiveStoreTEITest.
 */
class FileSystemArchiveStoreTeiTest {

    @Test
    void generateTEI_outputNamesFromPageFilenameAttribute(@TempDir Path tempDir) throws Exception {
        Path archiveDir = tempDir.resolve("archive");
        Path bookDir = archiveDir.resolve("testcol").resolve("TestBook");
        Files.createDirectories(bookDir);

        // The page filename attribute determines the output filename
        String aorXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <transcription>
                  <page filename="Ha2.005r.tif" reader="Harvey"/>
                  <annotation>
                    <underline method="pen" type="straight" text="text" language="IT"/>
                  </annotation>
                </transcription>
                """;
        Files.writeString(bookDir.resolve("TestBook.aor.005r.xml"), aorXml);

        var store = new FileSystemArchiveStore(archiveDir);
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        store.generateTEITranscriptions("testcol", "TestBook", errors, warnings);

        assertTrue(errors.isEmpty(), "Expected no errors: " + errors);

        // Output should be named from page filename (without .tif) + .tei.xml
        Path teiFile = bookDir.resolve("Ha2.005r.tei.xml");
        assertTrue(Files.exists(teiFile), "Output should be named <page>.tei.xml from page filename attribute");
    }

    @Test
    void generateTEI_fallbackNamingWhenNoPageFilename(@TempDir Path tempDir) throws Exception {
        Path archiveDir = tempDir.resolve("archive");
        Path bookDir = archiveDir.resolve("testcol").resolve("TestBook");
        Files.createDirectories(bookDir);

        // AoR file with no filename attribute on <page>
        String aorXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <transcription>
                  <page reader="Harvey"/>
                  <annotation>
                    <mark name="dot" method="pen" place="head"/>
                  </annotation>
                </transcription>
                """;
        Files.writeString(bookDir.resolve("TestBook.aor.042v.xml"), aorXml);

        var store = new FileSystemArchiveStore(archiveDir);
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        store.generateTEITranscriptions("testcol", "TestBook", errors, warnings);

        assertTrue(errors.isEmpty(), "Expected no errors: " + errors);

        // Should fall back to deriving page id from AoR filename: "TestBook.aor.042v.xml" -> "042v"
        Path teiFile = bookDir.resolve("042v.tei.xml");
        assertTrue(Files.exists(teiFile), "Should fall back to page id derived from AoR filename");
    }

    @Test
    void generateTEI_noAorFilesProducesNoOutput(@TempDir Path tempDir) throws Exception {
        Path archiveDir = tempDir.resolve("archive");
        Path bookDir = archiveDir.resolve("testcol").resolve("TestBook");
        Files.createDirectories(bookDir);

        // Only non-AoR files
        Files.writeString(bookDir.resolve("TestBook.metadata.xml"), "<metadata/>");
        Files.writeString(bookDir.resolve("notes.txt"), "some notes");

        var store = new FileSystemArchiveStore(archiveDir);
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        store.generateTEITranscriptions("testcol", "TestBook", errors, warnings);

        assertTrue(errors.isEmpty());
        assertTrue(warnings.isEmpty());

        long teiCount = Files.list(bookDir)
                .filter(p -> p.getFileName().toString().endsWith(".tei.xml"))
                .count();
        assertEquals(0, teiCount, "No TEI files when no AoR transcriptions exist");
    }

    @Test
    void generateTEI_multiplePagesProduceSeparateFiles(@TempDir Path tempDir) throws Exception {
        Path archiveDir = tempDir.resolve("archive");
        Path bookDir = archiveDir.resolve("testcol").resolve("TestBook");
        Files.createDirectories(bookDir);

        String aorXml1 = """
                <?xml version="1.0" encoding="UTF-8"?>
                <transcription>
                  <page filename="page001.tif" reader="Harvey"/>
                  <annotation>
                    <marginalia hand="Italian">
                      <language ident="LA">
                        <position place="head" book_orientation="0">
                          <marginalia_text>First page</marginalia_text>
                        </position>
                      </language>
                    </marginalia>
                  </annotation>
                </transcription>
                """;
        String aorXml2 = """
                <?xml version="1.0" encoding="UTF-8"?>
                <transcription>
                  <page filename="page002.tif" reader="Harvey"/>
                  <annotation>
                    <underline method="pen" type="straight" text="second page" language="LA"/>
                  </annotation>
                </transcription>
                """;
        String aorXml3 = """
                <?xml version="1.0" encoding="UTF-8"?>
                <transcription>
                  <page filename="page003.tif" reader="Harvey"/>
                  <annotation>
                    <symbol name="Mars" place="left_margin"/>
                  </annotation>
                </transcription>
                """;
        Files.writeString(bookDir.resolve("TestBook.aor.001.xml"), aorXml1);
        Files.writeString(bookDir.resolve("TestBook.aor.002.xml"), aorXml2);
        Files.writeString(bookDir.resolve("TestBook.aor.003.xml"), aorXml3);

        var store = new FileSystemArchiveStore(archiveDir);
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        store.generateTEITranscriptions("testcol", "TestBook", errors, warnings);

        assertTrue(errors.isEmpty(), "Expected no errors: " + errors);

        // Each page gets its own TEI file
        assertTrue(Files.exists(bookDir.resolve("page001.tei.xml")));
        assertTrue(Files.exists(bookDir.resolve("page002.tei.xml")));
        assertTrue(Files.exists(bookDir.resolve("page003.tei.xml")));
    }

    @Test
    void generateTEI_completeTeiP5Structure(@TempDir Path tempDir) throws Exception {
        Path archiveDir = tempDir.resolve("archive");
        Path bookDir = archiveDir.resolve("testcol").resolve("TestBook");
        Files.createDirectories(bookDir);

        String aorXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <transcription>
                  <page filename="TestBook.001r.tif" reader="Harvey"/>
                  <annotation>
                    <mark name="dot" method="pen" place="head"/>
                  </annotation>
                </transcription>
                """;
        Files.writeString(bookDir.resolve("TestBook.aor.001r.xml"), aorXml);

        var store = new FileSystemArchiveStore(archiveDir);
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        store.generateTEITranscriptions("testcol", "TestBook", errors, warnings);

        assertTrue(errors.isEmpty());

        Path teiFile = bookDir.resolve("TestBook.001r.tei.xml");
        Document doc = parseXml(teiFile);

        // Required TEI P5 structure elements
        Element root = doc.getDocumentElement();
        assertEquals("TEI", root.getTagName());
        assertEquals("http://www.tei-c.org/ns/1.0", root.getAttribute("xmlns"));

        // teiHeader > fileDesc > titleStmt, publicationStmt, sourceDesc
        assertEquals(1, doc.getElementsByTagName("teiHeader").getLength());
        assertEquals(1, doc.getElementsByTagName("fileDesc").getLength());
        assertEquals(1, doc.getElementsByTagName("titleStmt").getLength());
        assertEquals(1, doc.getElementsByTagName("publicationStmt").getLength());
        assertEquals(1, doc.getElementsByTagName("sourceDesc").getLength());

        // text > body > div[@type='annotations']
        assertEquals(1, doc.getElementsByTagName("text").getLength());
        assertEquals(1, doc.getElementsByTagName("body").getLength());
        NodeList divs = doc.getElementsByTagName("div");
        assertEquals(1, divs.getLength());
        assertEquals("annotations", ((Element) divs.item(0)).getAttribute("type"));

        // title should mention the page
        NodeList titles = doc.getElementsByTagName("title");
        assertTrue(titles.getLength() > 0);
        assertTrue(titles.item(0).getTextContent().contains("TestBook.001r"));
    }

    @Test
    void generateTEI_emptyAnnotationElement(@TempDir Path tempDir) throws Exception {
        Path archiveDir = tempDir.resolve("archive");
        Path bookDir = archiveDir.resolve("testcol").resolve("TestBook");
        Files.createDirectories(bookDir);

        // AoR file with empty annotation element (page with no annotations)
        String aorXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <transcription>
                  <page filename="empty.tif" reader="Harvey"/>
                  <annotation/>
                </transcription>
                """;
        Files.writeString(bookDir.resolve("TestBook.aor.empty.xml"), aorXml);

        var store = new FileSystemArchiveStore(archiveDir);
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        store.generateTEITranscriptions("testcol", "TestBook", errors, warnings);

        assertTrue(errors.isEmpty(), "Expected no errors: " + errors);
        assertTrue(warnings.isEmpty(), "Expected no warnings: " + warnings);

        // Should still produce a valid TEI file (just no annotation content in the div)
        Path teiFile = bookDir.resolve("empty.tei.xml");
        assertTrue(Files.exists(teiFile));

        Document doc = parseXml(teiFile);
        assertNotNull(doc);
        assertEquals("TEI", doc.getDocumentElement().getTagName());
    }

    @Test
    void generateTEI_eachOutputIsIndependentlyWellFormed(@TempDir Path tempDir) throws Exception {
        Path archiveDir = tempDir.resolve("archive");
        Path bookDir = archiveDir.resolve("testcol").resolve("TestBook");
        Files.createDirectories(bookDir);

        // Create several pages with different annotation types
        Files.writeString(bookDir.resolve("TestBook.aor.001r.xml"), """
                <?xml version="1.0" encoding="UTF-8"?>
                <transcription>
                  <page filename="p1.tif" reader="Harvey"/>
                  <annotation>
                    <marginalia hand="Italian">
                      <language ident="LA">
                        <position place="head" book_orientation="0">
                          <marginalia_text>Note</marginalia_text>
                        </position>
                      </language>
                    </marginalia>
                  </annotation>
                </transcription>
                """);
        Files.writeString(bookDir.resolve("TestBook.aor.002v.xml"), """
                <?xml version="1.0" encoding="UTF-8"?>
                <transcription>
                  <page filename="p2.tif" reader="Harvey"/>
                  <annotation>
                    <errata language="LA" copytext="bad" amendedtext="good"/>
                  </annotation>
                </transcription>
                """);

        var store = new FileSystemArchiveStore(archiveDir);
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        store.generateTEITranscriptions("testcol", "TestBook", errors, warnings);

        assertTrue(errors.isEmpty());

        // Each output file should independently parse as well-formed XML
        for (String name : List.of("p1.tei.xml", "p2.tei.xml")) {
            Path teiFile = bookDir.resolve(name);
            assertTrue(Files.exists(teiFile), name + " should exist");
            Document doc = parseXml(teiFile);
            assertNotNull(doc, name + " should be well-formed XML");
            assertEquals("TEI", doc.getDocumentElement().getTagName());
        }
    }

    // ---- Helper method ----

    private Document parseXml(Path xmlFile) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        try (InputStream in = Files.newInputStream(xmlFile)) {
            return builder.parse(in);
        }
    }
}
