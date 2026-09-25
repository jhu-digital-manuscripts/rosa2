package rosa.archive.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link TeiMetadataMigrator}.
 */
class TeiMetadataMigratorTest {

    @TempDir
    Path tempDir;

    @Test
    void migrateBook_parsesEnglishTeiAndWritesMetadataXml() throws IOException {
        // Set up archive/collection/book structure
        Path bookDir = tempDir.resolve("testcol").resolve("TestBook");
        Files.createDirectories(bookDir);

        // Write a TEI description file
        String teiContent = """
                <?xml version="1.0" encoding="UTF-8"?>
                <TEI xmlns="http://www.tei-c.org/ns/1.0" version="5.0">
                  <teiheader>
                    <sourceDesc>
                      <bibl>
                        <title>The Test Book</title>
                        <pubPlace>France</pubPlace>
                        <date notBefore="1400" notAfter="1450">early 15th century</date>
                        <note type="format">manuscript</note>
                        <note type="commonName">Test MS</note>
                        <note type="material">parchment</note>
                        <note type="illustrations">12</note>
                        <extent>
                          <measure quantity="100" unit="folios">100 folios</measure>
                          <dimensions>
                            <height unit="mm">300</height>
                            <width unit="mm">200</width>
                          </dimensions>
                        </extent>
                      </bibl>
                      <msDesc>
                        <msIdentifier>
                          <settlement>Paris</settlement>
                          <repository>Bibliothèque nationale de France</repository>
                          <idno>fr. 1234</idno>
                        </msIdentifier>
                        <msContents>
                          <msItem n="0">
                            <locus from="1r" to="50v">1r-50v</locus>
                            <title>Roman de la Rose</title>
                            <note type="folios">50</note>
                            <note type="illustrations">6</note>
                            <note type="linesPerColumn">40</note>
                            <note type="leavesPerGathering">8</note>
                            <note type="columnsPerFolio">2</note>
                            <note type="author">Guillaume de Lorris</note>
                          </msItem>
                        </msContents>
                      </msDesc>
                    </sourceDesc>
                  </teiheader>
                </TEI>
                """;

        Files.writeString(bookDir.resolve("TestBook.description_en.xml"), teiContent, StandardCharsets.UTF_8);

        // Run migration
        var migrator = new TeiMetadataMigrator(tempDir);
        List<String> errors = new ArrayList<>();
        migrator.migrateBook("testcol", "TestBook", errors);

        // Verify no errors
        assertTrue(errors.isEmpty(), "Expected no errors but got: " + errors);

        // Verify metadata.xml was created
        Path metadataFile = bookDir.resolve("TestBook.metadata.xml");
        assertTrue(Files.exists(metadataFile), "metadata.xml should be created");

        String content = Files.readString(metadataFile, StandardCharsets.UTF_8);
        assertTrue(content.contains("<book>"), "Should have <book> root element");
        assertTrue(content.contains("<width>200</width>"), "Should contain width");
        assertTrue(content.contains("<height>300</height>"), "Should contain height");
        assertTrue(content.contains("<totalPages>100</totalPages>"), "Should contain page count");
        assertTrue(content.contains("<illustrations>12</illustrations>"), "Should contain illustration count");
        assertTrue(content.contains("<startDate>1400</startDate>"), "Should contain start date");
        assertTrue(content.contains("<endDate>1450</endDate>"), "Should contain end date");
        assertTrue(content.contains("lang=\"en\""), "Should have English bibliography");
        assertTrue(content.contains("<title>The Test Book</title>"), "Should contain title in bibliography");
        assertTrue(content.contains("<repository>Bibliothèque nationale de France</repository>"),
                "Should contain repository");
        assertTrue(content.contains("<shelfmark>fr. 1234</shelfmark>"), "Should contain shelfmark");
        assertTrue(content.contains("<origin>France</origin>"), "Should contain origin");
        assertTrue(content.contains("<currentLocation>Paris</currentLocation>"), "Should contain location");
    }

    @Test
    void migrateBook_skipsWhenMetadataAlreadyExists() throws IOException {
        Path bookDir = tempDir.resolve("testcol").resolve("TestBook");
        Files.createDirectories(bookDir);

        // Create an existing metadata file
        Files.writeString(bookDir.resolve("TestBook.metadata.xml"), "<book/>", StandardCharsets.UTF_8);

        // Also write a description file
        Files.writeString(bookDir.resolve("TestBook.description_en.xml"),
                "<?xml version=\"1.0\"?><TEI><teiheader><sourceDesc><bibl><title>X</title></bibl></sourceDesc></teiheader></TEI>",
                StandardCharsets.UTF_8);

        var migrator = new TeiMetadataMigrator(tempDir);
        List<String> errors = new ArrayList<>();
        migrator.migrateBook("testcol", "TestBook", errors);

        // Should have no errors (skip is not an error)
        assertTrue(errors.isEmpty(), "Expected no errors but got: " + errors);

        // Metadata file should still contain original content (not overwritten)
        String content = Files.readString(bookDir.resolve("TestBook.metadata.xml"));
        assertEquals("<book/>", content);
    }

    @Test
    void migrateBook_reportsErrorForMissingBookDirectory() {
        var migrator = new TeiMetadataMigrator(tempDir);
        List<String> errors = new ArrayList<>();
        migrator.migrateBook("nosuchcol", "nosuchbook", errors);

        assertFalse(errors.isEmpty(), "Should report error for missing directory");
        assertTrue(errors.getFirst().contains("does not exist"));
    }

    @Test
    void migrateCollection_processesAllBooks() throws IOException {
        Path colDir = tempDir.resolve("mycol");
        Files.createDirectories(colDir.resolve("BookA"));
        Files.createDirectories(colDir.resolve("BookB"));

        String tei = """
                <?xml version="1.0" encoding="UTF-8"?>
                <TEI xmlns="http://www.tei-c.org/ns/1.0">
                  <teiheader>
                    <sourceDesc>
                      <bibl><title>Title</title><pubPlace>Italy</pubPlace>
                        <date notBefore="1500" notAfter="1550">16th century</date>
                      </bibl>
                      <msDesc><msIdentifier>
                        <settlement>Rome</settlement>
                        <repository>Vatican Library</repository>
                        <idno>MS 42</idno>
                      </msIdentifier></msDesc>
                    </sourceDesc>
                  </teiheader>
                </TEI>
                """;

        Files.writeString(colDir.resolve("BookA").resolve("BookA.description_en.xml"), tei, StandardCharsets.UTF_8);
        Files.writeString(colDir.resolve("BookB").resolve("BookB.description_fr.xml"), tei, StandardCharsets.UTF_8);

        var migrator = new TeiMetadataMigrator(tempDir);
        List<String> errors = new ArrayList<>();
        migrator.migrateCollection("mycol", errors);

        assertTrue(errors.isEmpty(), "Expected no errors but got: " + errors);
        assertTrue(Files.exists(colDir.resolve("BookA").resolve("BookA.metadata.xml")));
        assertTrue(Files.exists(colDir.resolve("BookB").resolve("BookB.metadata.xml")));
    }

    @Test
    void migrateBook_consolidatesMultipleLanguages() throws IOException {
        Path bookDir = tempDir.resolve("testcol").resolve("MultiLang");
        Files.createDirectories(bookDir);

        String enTei = """
                <?xml version="1.0" encoding="UTF-8"?>
                <TEI xmlns="http://www.tei-c.org/ns/1.0">
                  <teiheader><sourceDesc>
                    <bibl><title>English Title</title><pubPlace>France</pubPlace>
                      <date notBefore="1350" notAfter="1400">14th century</date>
                      <extent><measure quantity="80" unit="folios">80 folios</measure>
                        <dimensions><height unit="mm">250</height><width unit="mm">180</width></dimensions>
                      </extent>
                    </bibl>
                    <msDesc><msIdentifier>
                      <settlement>London</settlement>
                      <repository>British Library</repository>
                      <idno>MS Royal 19</idno>
                    </msIdentifier></msDesc>
                  </sourceDesc></teiheader>
                </TEI>
                """;

        String frTei = """
                <?xml version="1.0" encoding="UTF-8"?>
                <TEI xmlns="http://www.tei-c.org/ns/1.0">
                  <teiheader><sourceDesc>
                    <bibl><title>Titre français</title><pubPlace>France</pubPlace>
                      <date notBefore="1350" notAfter="1400">XIVe siècle</date>
                    </bibl>
                    <msDesc><msIdentifier>
                      <settlement>Londres</settlement>
                      <repository>British Library</repository>
                      <idno>MS Royal 19</idno>
                    </msIdentifier></msDesc>
                  </sourceDesc></teiheader>
                </TEI>
                """;

        Files.writeString(bookDir.resolve("MultiLang.description_en.xml"), enTei, StandardCharsets.UTF_8);
        Files.writeString(bookDir.resolve("MultiLang.description_fr.xml"), frTei, StandardCharsets.UTF_8);

        var migrator = new TeiMetadataMigrator(tempDir);
        List<String> errors = new ArrayList<>();
        migrator.migrateBook("testcol", "MultiLang", errors);

        assertTrue(errors.isEmpty(), "Expected no errors but got: " + errors);

        Path metadataFile = bookDir.resolve("MultiLang.metadata.xml");
        assertTrue(Files.exists(metadataFile));

        String content = Files.readString(metadataFile);
        assertTrue(content.contains("lang=\"en\""), "Should have English bibliography");
        assertTrue(content.contains("lang=\"fr\""), "Should have French bibliography");
        assertTrue(content.contains("<title>English Title</title>"));
        assertTrue(content.contains("<title>Titre français</title>"));
    }

    @Test
    void migrateBook_handlesParseError() throws IOException {
        Path bookDir = tempDir.resolve("testcol").resolve("BadBook");
        Files.createDirectories(bookDir);

        // Write invalid XML
        Files.writeString(bookDir.resolve("BadBook.description_en.xml"),
                "not valid xml <<<<", StandardCharsets.UTF_8);

        var migrator = new TeiMetadataMigrator(tempDir);
        List<String> errors = new ArrayList<>();
        migrator.migrateBook("testcol", "BadBook", errors);

        assertFalse(errors.isEmpty(), "Should report parse error");
        assertTrue(errors.getFirst().contains("Failed to parse"));
    }

    @Test
    void migrateBook_noDescriptionFiles_doesNotCreateMetadata() throws IOException {
        Path bookDir = tempDir.resolve("testcol").resolve("EmptyBook");
        Files.createDirectories(bookDir);

        var migrator = new TeiMetadataMigrator(tempDir);
        List<String> errors = new ArrayList<>();
        migrator.migrateBook("testcol", "EmptyBook", errors);

        assertTrue(errors.isEmpty());
        assertFalse(Files.exists(bookDir.resolve("EmptyBook.metadata.xml")),
                "Should not create metadata.xml when no description files exist");
    }
}
