package rosa.archive.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for TEI transcription generation via
 * {@link FileSystemArchiveStore#generateTEITranscriptions}.
 */
class FileSystemArchiveStoreGenerateTEITest {

    private static final Charset LATIN1 = Charset.forName("Latin1");

    private void writeTextFile(Path dir, String name, String content) throws IOException {
        Files.writeString(dir.resolve(name), content, LATIN1);
    }

    @Test
    void generateTEI_producesTranscriptionXml(@TempDir Path tempDir) throws Exception {
        Path bookDir = tempDir.resolve("testcol").resolve("TestBook");
        Files.createDirectories(bookDir);

        writeTextFile(bookDir, "TestBook.transcription.001r.txt",
                "[TestBook]\n[1r a]\n\nFirst line\nSecond line\t\t2\n\n");
        writeTextFile(bookDir, "TestBook.transcription.001v.txt",
                "[TestBook]\n[1v a]\n\nThird line\nFourth line\t\t4\n\n");

        var store = new FileSystemArchiveStore(tempDir);
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        store.generateTEITranscriptions("testcol", "TestBook", errors, warnings);

        assertTrue(errors.isEmpty(), "Expected no errors: " + errors);

        Path output = bookDir.resolve("TestBook.transcription.xml");
        assertTrue(Files.exists(output), "Expected transcription.xml to be generated");

        String xml = Files.readString(output);
        assertTrue(xml.contains("<TEI"), "Expected TEI element");
        assertTrue(xml.contains("<pb n=\"001r\""), "Expected first page break");
        assertTrue(xml.contains("<pb n=\"001v\""), "Expected second page break");
        assertTrue(xml.contains("<lg type=\"couplet\""), "Expected couplet lg");
    }

    @Test
    void generateTEI_noTxtFiles_noOutput(@TempDir Path tempDir) throws Exception {
        // A book with only AoR XML files — should produce no output
        Path bookDir = tempDir.resolve("aorcol").resolve("AorBook");
        Files.createDirectories(bookDir);

        Files.writeString(bookDir.resolve("AorBook.aor.001r.xml"),
                "<?xml version=\"1.0\"?><transcription><page filename=\"001r.tif\"/></transcription>");

        var store = new FileSystemArchiveStore(tempDir);
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        store.generateTEITranscriptions("aorcol", "AorBook", errors, warnings);

        assertTrue(errors.isEmpty(), "Expected no errors: " + errors);
        assertTrue(warnings.isEmpty(), "Expected no warnings");
        assertFalse(Files.exists(bookDir.resolve("AorBook.transcription.xml")),
                "No transcription.xml should be generated for AoR-only books");
    }

    @Test
    void generateTEI_overwritesExistingTranscription(@TempDir Path tempDir) throws Exception {
        Path bookDir = tempDir.resolve("testcol").resolve("Book2");
        Files.createDirectories(bookDir);

        // Pre-existing transcription.xml
        Files.writeString(bookDir.resolve("Book2.transcription.xml"), "<old/>");

        writeTextFile(bookDir, "Book2.transcription.010r.txt",
                "[Book2]\n[10r a]\n\nNew content\nMore content\t\t100\n\n");

        var store = new FileSystemArchiveStore(tempDir);
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        store.generateTEITranscriptions("testcol", "Book2", errors, warnings);

        assertTrue(errors.isEmpty(), "Expected no errors: " + errors);
        String xml = Files.readString(bookDir.resolve("Book2.transcription.xml"));
        assertFalse(xml.contains("<old/>"), "Old content should be overwritten");
        assertTrue(xml.contains("<TEI"), "New TEI content expected");
    }

    @Test
    void generateTEI_bookNotFound_throwsIOException(@TempDir Path tempDir) throws Exception {
        var store = new FileSystemArchiveStore(tempDir);
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        assertThrows(IOException.class,
                () -> store.generateTEITranscriptions("nosuchcol", "NoBook", errors, warnings));
    }

    @Test
    void generateTEI_douce332_integration(@TempDir Path tempDir) throws Exception {
        // Use real test resources if available
        Path sourceBookDir = Path.of("src/test/resources/archive/rose/Douce332");
        if (!Files.isDirectory(sourceBookDir)) return;

        // Copy txt files to temp dir structure
        Path bookDir = tempDir.resolve("rose").resolve("Douce332");
        Files.createDirectories(bookDir);

        try (var stream = Files.list(sourceBookDir)) {
            stream.filter(p -> p.getFileName().toString().endsWith(".txt"))
                    .forEach(src -> {
                        try {
                            Files.copy(src, bookDir.resolve(src.getFileName()));
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });
        }

        var store = new FileSystemArchiveStore(tempDir);
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        store.generateTEITranscriptions("rose", "Douce332", errors, warnings);

        assertTrue(errors.isEmpty(), "Errors: " + errors);
        Path output = bookDir.resolve("Douce332.transcription.xml");
        assertTrue(Files.exists(output), "Expected transcription.xml");

        String xml = Files.readString(output);
        assertTrue(xml.contains("<TEI"), "Expected TEI");
        assertTrue(xml.contains("<pb"), "Expected page breaks");
        // Should contain verse content from the txt files
        assertTrue(xml.contains("Maintes") || xml.contains("gens"),
                "Expected some poetry content from Douce332");
    }
}
