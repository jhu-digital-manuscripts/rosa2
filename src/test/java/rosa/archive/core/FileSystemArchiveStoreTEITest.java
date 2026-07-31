package rosa.archive.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for TEI transcription generation in FileSystemArchiveStore.
 */
class FileSystemArchiveStoreTEITest {

    private static final String AOR_WITH_MARGINALIA = """
            <?xml version="1.0" encoding="UTF-8" standalone="no"?>
            <transcription>
                <page filename="TestBook.001r.tif" reader="Harvey"/>
                <annotation>
                    <marginalia hand="Italian">
                        <language ident="LA">
                            <position place="head" book_orientation="0">
                                <marginalia_text>Gabrielis Haruey</marginalia_text>
                                <person name="Gabriel Harvey"/>
                            </position>
                        </language>
                    </marginalia>
                </annotation>
            </transcription>
            """;

    private static final String AOR_WITH_UNDERLINE = """
            <?xml version="1.0" encoding="UTF-8" standalone="no"?>
            <transcription>
                <page filename="TestBook.002r.tif" reader="Harvey"/>
                <annotation>
                    <underline method="pen" type="straight" language="IT" text="una donna"/>
                </annotation>
            </transcription>
            """;

    private static final String AOR_WITH_MARKS_AND_SYMBOLS = """
            <?xml version="1.0" encoding="UTF-8" standalone="no"?>
            <transcription>
                <page filename="TestBook.003r.tif" reader="Harvey"/>
                <annotation>
                    <mark name="plus_sign" method="pen" place="intext" language="IT" text="calor naturale"/>
                    <symbol name="Sun" place="head"/>
                </annotation>
            </transcription>
            """;

    private static final String AOR_WITH_ERRATA = """
            <?xml version="1.0" encoding="UTF-8" standalone="no"?>
            <transcription>
                <page filename="TestBook.004r.tif" reader="Harvey"/>
                <annotation>
                    <errata copytext="cacommodato" amendedtext="acommodato" language="IT"/>
                </annotation>
            </transcription>
            """;

    private static final String AOR_WITH_ALL_TYPES = """
            <?xml version="1.0" encoding="UTF-8" standalone="no"?>
            <transcription>
                <page filename="TestBook.005r.tif" reader="Harvey"/>
                <annotation>
                    <marginalia hand="Italian">
                        <language ident="LA">
                            <position place="right_margin" book_orientation="0">
                                <marginalia_text>Some note text</marginalia_text>
                            </position>
                        </language>
                    </marginalia>
                    <underline method="pen" type="straight" language="IT" text="underlined text"/>
                    <mark name="dot" method="pen" place="intext"/>
                    <symbol name="Mercury" place="left_margin"/>
                    <errata copytext="wrongword" amendedtext="rightword" language="LA"/>
                </annotation>
            </transcription>
            """;

    private static final String AOR_WITH_UNKNOWN_TYPE = """
            <?xml version="1.0" encoding="UTF-8" standalone="no"?>
            <transcription>
                <page filename="TestBook.006r.tif" reader="Harvey"/>
                <annotation>
                    <numeral place="intext" language="LA" text="12"/>
                    <marginalia hand="Italian">
                        <language ident="LA">
                            <position place="head" book_orientation="0">
                                <marginalia_text>Note</marginalia_text>
                            </position>
                        </language>
                    </marginalia>
                </annotation>
            </transcription>
            """;

    @Test
    void generateTEI_producesWellFormedXml(@TempDir Path tempDir) throws Exception {
        Path archiveDir = setupArchiveWithFile(tempDir, "TestBook.aor.001r.xml", AOR_WITH_MARGINALIA);

        FileSystemArchiveStore store = new FileSystemArchiveStore(archiveDir);
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        store.generateTEITranscriptions("testcol", "TestBook", errors, warnings);

        assertTrue(errors.isEmpty(), "Expected no errors but got: " + errors);
        assertTrue(warnings.isEmpty(), "Expected no warnings but got: " + warnings);

        // Verify output file exists
        Path teiFile = archiveDir.resolve("testcol/TestBook/TestBook.001r.tei.xml");
        assertTrue(Files.exists(teiFile), "TEI file should exist");

        // Parse the output to verify well-formedness
        Document doc = parseXml(teiFile);
        assertNotNull(doc);
        assertEquals("TEI", doc.getDocumentElement().getTagName());
    }

    @Test
    void generateTEI_hasTEIP5Structure(@TempDir Path tempDir) throws Exception {
        Path archiveDir = setupArchiveWithFile(tempDir, "TestBook.aor.001r.xml", AOR_WITH_MARGINALIA);

        FileSystemArchiveStore store = new FileSystemArchiveStore(archiveDir);
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        store.generateTEITranscriptions("testcol", "TestBook", errors, warnings);

        Path teiFile = archiveDir.resolve("testcol/TestBook/TestBook.001r.tei.xml");
        Document doc = parseXml(teiFile);

        // Check TEI namespace
        Element root = doc.getDocumentElement();
        assertEquals("http://www.tei-c.org/ns/1.0", root.getAttribute("xmlns"));

        // Check teiHeader exists
        NodeList headers = doc.getElementsByTagName("teiHeader");
        assertEquals(1, headers.getLength());

        // Check text > body exists
        NodeList bodies = doc.getElementsByTagName("body");
        assertEquals(1, bodies.getLength());
    }

    @Test
    void generateTEI_marginaliaMappedToNote(@TempDir Path tempDir) throws Exception {
        Path archiveDir = setupArchiveWithFile(tempDir, "TestBook.aor.001r.xml", AOR_WITH_MARGINALIA);

        FileSystemArchiveStore store = new FileSystemArchiveStore(archiveDir);
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        store.generateTEITranscriptions("testcol", "TestBook", errors, warnings);

        Path teiFile = archiveDir.resolve("testcol/TestBook/TestBook.001r.tei.xml");
        Document doc = parseXml(teiFile);

        NodeList notes = doc.getElementsByTagName("note");
        assertEquals(1, notes.getLength());

        Element note = (Element) notes.item(0);
        assertEquals("marginalia", note.getAttribute("type"));
        assertEquals("head", note.getAttribute("place"));
        assertEquals("Italian", note.getAttribute("hand"));
        assertTrue(note.getTextContent().contains("Gabrielis Haruey"));
    }

    @Test
    void generateTEI_underlineMappedToHi(@TempDir Path tempDir) throws Exception {
        Path archiveDir = setupArchiveWithFile(tempDir, "TestBook.aor.002r.xml", AOR_WITH_UNDERLINE);

        FileSystemArchiveStore store = new FileSystemArchiveStore(archiveDir);
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        store.generateTEITranscriptions("testcol", "TestBook", errors, warnings);

        Path teiFile = archiveDir.resolve("testcol/TestBook/TestBook.002r.tei.xml");
        Document doc = parseXml(teiFile);

        NodeList his = doc.getElementsByTagName("hi");
        assertEquals(1, his.getLength());

        Element hi = (Element) his.item(0);
        assertEquals("underline", hi.getAttribute("rend"));
        assertEquals("una donna", hi.getTextContent());
    }

    @Test
    void generateTEI_markMappedToMetamark(@TempDir Path tempDir) throws Exception {
        Path archiveDir = setupArchiveWithFile(tempDir, "TestBook.aor.003r.xml", AOR_WITH_MARKS_AND_SYMBOLS);

        FileSystemArchiveStore store = new FileSystemArchiveStore(archiveDir);
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        store.generateTEITranscriptions("testcol", "TestBook", errors, warnings);

        Path teiFile = archiveDir.resolve("testcol/TestBook/TestBook.003r.tei.xml");
        Document doc = parseXml(teiFile);

        NodeList metamarks = doc.getElementsByTagName("metamark");
        assertEquals(1, metamarks.getLength());

        Element metamark = (Element) metamarks.item(0);
        assertEquals("plus_sign", metamark.getAttribute("function"));
        assertEquals("intext", metamark.getAttribute("place"));
        assertEquals("calor naturale", metamark.getTextContent());
    }

    @Test
    void generateTEI_symbolMappedToGlyph(@TempDir Path tempDir) throws Exception {
        Path archiveDir = setupArchiveWithFile(tempDir, "TestBook.aor.003r.xml", AOR_WITH_MARKS_AND_SYMBOLS);

        FileSystemArchiveStore store = new FileSystemArchiveStore(archiveDir);
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        store.generateTEITranscriptions("testcol", "TestBook", errors, warnings);

        Path teiFile = archiveDir.resolve("testcol/TestBook/TestBook.003r.tei.xml");
        Document doc = parseXml(teiFile);

        NodeList glyphs = doc.getElementsByTagName("g");
        assertEquals(1, glyphs.getLength());

        Element g = (Element) glyphs.item(0);
        assertEquals("#Sun", g.getAttribute("ref"));
        assertEquals("head", g.getAttribute("place"));
    }

    @Test
    void generateTEI_errataMappedToChoice(@TempDir Path tempDir) throws Exception {
        Path archiveDir = setupArchiveWithFile(tempDir, "TestBook.aor.004r.xml", AOR_WITH_ERRATA);

        FileSystemArchiveStore store = new FileSystemArchiveStore(archiveDir);
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        store.generateTEITranscriptions("testcol", "TestBook", errors, warnings);

        Path teiFile = archiveDir.resolve("testcol/TestBook/TestBook.004r.tei.xml");
        Document doc = parseXml(teiFile);

        NodeList choices = doc.getElementsByTagName("choice");
        assertEquals(1, choices.getLength());

        Element choice = (Element) choices.item(0);
        NodeList sics = choice.getElementsByTagName("sic");
        NodeList corrs = choice.getElementsByTagName("corr");
        assertEquals(1, sics.getLength());
        assertEquals(1, corrs.getLength());
        assertEquals("cacommodato", sics.item(0).getTextContent());
        assertEquals("acommodato", corrs.item(0).getTextContent());
    }

    @Test
    void generateTEI_allAnnotationTypesMapped(@TempDir Path tempDir) throws Exception {
        Path archiveDir = setupArchiveWithFile(tempDir, "TestBook.aor.005r.xml", AOR_WITH_ALL_TYPES);

        FileSystemArchiveStore store = new FileSystemArchiveStore(archiveDir);
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        store.generateTEITranscriptions("testcol", "TestBook", errors, warnings);

        assertTrue(errors.isEmpty(), "Expected no errors but got: " + errors);
        assertTrue(warnings.isEmpty(), "Expected no warnings but got: " + warnings);

        Path teiFile = archiveDir.resolve("testcol/TestBook/TestBook.005r.tei.xml");
        Document doc = parseXml(teiFile);

        assertEquals(1, doc.getElementsByTagName("note").getLength());
        assertEquals(1, doc.getElementsByTagName("hi").getLength());
        assertEquals(1, doc.getElementsByTagName("metamark").getLength());
        assertEquals(1, doc.getElementsByTagName("g").getLength());
        assertEquals(1, doc.getElementsByTagName("choice").getLength());
    }

    @Test
    void generateTEI_reportsWarningForUnmappableTypes(@TempDir Path tempDir) throws Exception {
        Path archiveDir = setupArchiveWithFile(tempDir, "TestBook.aor.006r.xml", AOR_WITH_UNKNOWN_TYPE);

        FileSystemArchiveStore store = new FileSystemArchiveStore(archiveDir);
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        store.generateTEITranscriptions("testcol", "TestBook", errors, warnings);

        assertTrue(errors.isEmpty(), "Expected no errors but got: " + errors);
        assertEquals(1, warnings.size());
        assertTrue(warnings.get(0).contains("numeral"), "Warning should mention the unmappable type");

        // The mapped types should still be present
        Path teiFile = archiveDir.resolve("testcol/TestBook/TestBook.006r.tei.xml");
        Document doc = parseXml(teiFile);
        assertEquals(1, doc.getElementsByTagName("note").getLength());
    }

    @Test
    void generateTEI_reportsErrorForUnreadableFile(@TempDir Path tempDir) throws Exception {
        Path archiveDir = tempDir.resolve("archive");
        Path bookDir = archiveDir.resolve("testcol/TestBook");
        Files.createDirectories(bookDir);

        // Write a malformed XML file
        Files.writeString(bookDir.resolve("TestBook.aor.001r.xml"), "not valid xml <<>>");

        FileSystemArchiveStore store = new FileSystemArchiveStore(archiveDir);
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        store.generateTEITranscriptions("testcol", "TestBook", errors, warnings);

        assertFalse(errors.isEmpty(), "Should report an error for unreadable file");
        assertTrue(errors.get(0).contains("TestBook.aor.001r.xml"));
    }

    @Test
    void generateTEI_continuesProcessingAfterError(@TempDir Path tempDir) throws Exception {
        Path archiveDir = tempDir.resolve("archive");
        Path bookDir = archiveDir.resolve("testcol/TestBook");
        Files.createDirectories(bookDir);

        // One malformed file and one valid file
        Files.writeString(bookDir.resolve("TestBook.aor.001r.xml"), "not valid xml <<>>");
        Files.writeString(bookDir.resolve("TestBook.aor.002r.xml"), AOR_WITH_UNDERLINE);

        FileSystemArchiveStore store = new FileSystemArchiveStore(archiveDir);
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        store.generateTEITranscriptions("testcol", "TestBook", errors, warnings);

        // Should have an error for the malformed file
        assertEquals(1, errors.size());
        // But the valid file should still produce output
        Path teiFile = archiveDir.resolve("testcol/TestBook/TestBook.002r.tei.xml");
        assertTrue(Files.exists(teiFile), "Valid file should still produce TEI output");
    }

    @Test
    void generateTEI_throwsOnMissingBook(@TempDir Path tempDir) throws IOException {
        Path archiveDir = tempDir.resolve("archive");
        Path collectionDir = archiveDir.resolve("testcol");
        Files.createDirectories(collectionDir);

        FileSystemArchiveStore store = new FileSystemArchiveStore(archiveDir);
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        assertThrows(IOException.class, () ->
                store.generateTEITranscriptions("testcol", "nonexistent", errors, warnings));
    }

    @Test
    void generateTEI_skipsNonAorFiles(@TempDir Path tempDir) throws Exception {
        Path archiveDir = tempDir.resolve("archive");
        Path bookDir = archiveDir.resolve("testcol/TestBook");
        Files.createDirectories(bookDir);

        // Write an AoR file and a non-AoR XML file
        Files.writeString(bookDir.resolve("TestBook.aor.001r.xml"), AOR_WITH_MARGINALIA);
        Files.writeString(bookDir.resolve("TestBook.metadata.xml"), "<metadata/>");

        FileSystemArchiveStore store = new FileSystemArchiveStore(archiveDir);
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        store.generateTEITranscriptions("testcol", "TestBook", errors, warnings);

        assertTrue(errors.isEmpty());
        // Only the AoR file should produce a TEI file
        Path teiFile = archiveDir.resolve("testcol/TestBook/TestBook.001r.tei.xml");
        assertTrue(Files.exists(teiFile));
        // No TEI file for the metadata file
        assertFalse(Files.exists(archiveDir.resolve("testcol/TestBook/TestBook.metadata.tei.xml")));
    }

    @Test
    void generateTEI_multipleMarginalia(@TempDir Path tempDir) throws Exception {
        String aorMultiMarginalia = """
                <?xml version="1.0" encoding="UTF-8" standalone="no"?>
                <transcription>
                    <page filename="TestBook.007r.tif" reader="Harvey"/>
                    <annotation>
                        <marginalia hand="Italian">
                            <language ident="LA">
                                <position place="head" book_orientation="0">
                                    <marginalia_text>First note</marginalia_text>
                                </position>
                            </language>
                        </marginalia>
                        <marginalia hand="English_secretary">
                            <language ident="EN">
                                <position place="tail" book_orientation="0">
                                    <marginalia_text>Second note</marginalia_text>
                                </position>
                            </language>
                        </marginalia>
                    </annotation>
                </transcription>
                """;

        Path archiveDir = setupArchiveWithFile(tempDir, "TestBook.aor.007r.xml", aorMultiMarginalia);

        FileSystemArchiveStore store = new FileSystemArchiveStore(archiveDir);
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        store.generateTEITranscriptions("testcol", "TestBook", errors, warnings);

        Path teiFile = archiveDir.resolve("testcol/TestBook/TestBook.007r.tei.xml");
        Document doc = parseXml(teiFile);

        NodeList notes = doc.getElementsByTagName("note");
        assertEquals(2, notes.getLength());

        Element note1 = (Element) notes.item(0);
        assertEquals("head", note1.getAttribute("place"));
        assertEquals("Italian", note1.getAttribute("hand"));

        Element note2 = (Element) notes.item(1);
        assertEquals("tail", note2.getAttribute("place"));
        assertEquals("English_secretary", note2.getAttribute("hand"));
    }

    // ---- Helper methods ----

    private Path setupArchiveWithFile(Path tempDir, String fileName, String content) throws IOException {
        Path archiveDir = tempDir.resolve("archive");
        Path bookDir = archiveDir.resolve("testcol/TestBook");
        Files.createDirectories(bookDir);
        Files.writeString(bookDir.resolve(fileName), content, StandardCharsets.UTF_8);
        return archiveDir;
    }

    private Document parseXml(Path xmlFile) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(false);
        DocumentBuilder builder = factory.newDocumentBuilder();
        try (InputStream in = Files.newInputStream(xmlFile)) {
            return builder.parse(in);
        }
    }
}
