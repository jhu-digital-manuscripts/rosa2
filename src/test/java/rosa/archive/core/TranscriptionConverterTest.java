package rosa.archive.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link TranscriptionConverter} — converting per-page text
 * transcription files into a single TEI P5 XML document.
 */
class TranscriptionConverterTest {

    private static final Charset LATIN1 = Charset.forName("Latin1");

    private Path writeTextFile(Path dir, String name, String content) throws IOException {
        Path file = dir.resolve(name);
        Files.writeString(file, content, LATIN1);
        return file;
    }

    private String readOutput(Path file) throws IOException {
        return Files.readString(file, StandardCharsets.UTF_8);
    }

    @Test
    void singlePage_producesValidTEI(@TempDir Path tempDir) throws IOException {
        Path input = writeTextFile(tempDir, "Book.transcription.001r.txt",
                "[Book]\n[1r a]\n\nLine one\nLine two\t\t2\n\n");

        Path output = tempDir.resolve("Book.transcription.xml");
        var converter = new TranscriptionConverter();
        converter.convert(List.of(input), output);

        assertTrue(converter.getErrors().isEmpty(), "Errors: " + converter.getErrors());
        assertTrue(Files.exists(output));

        String xml = readOutput(output);
        assertTrue(xml.contains("<TEI xmlns=\"http://www.tei-c.org/ns/1.0\""));
        assertTrue(xml.contains("<pb n=\"001r\""));
        assertTrue(xml.contains("<cb n=\"a\""));
        assertTrue(xml.contains("<lg type=\"couplet\""));
        assertTrue(xml.contains("<l>"));
        assertTrue(xml.contains("<milestone"));
        assertTrue(xml.contains("ed=\"lecoy\""));
    }

    @Test
    void multiPage_multiplePageBreaks(@TempDir Path tempDir) throws IOException {
        Path p1 = writeTextFile(tempDir, "Book.transcription.001r.txt",
                "[Book]\n[1r a]\n\nLine1\nLine2\t\t10\n\n");
        Path p2 = writeTextFile(tempDir, "Book.transcription.001v.txt",
                "[Book]\n[1v a]\n\nLine3\nLine4\t\t12\n\n");

        Path output = tempDir.resolve("Book.transcription.xml");
        var converter = new TranscriptionConverter();
        converter.convert(List.of(p1, p2), output);

        assertTrue(converter.getErrors().isEmpty(), "Errors: " + converter.getErrors());
        String xml = readOutput(output);

        // Should have two page breaks
        int firstPb = xml.indexOf("<pb n=\"001r\"");
        int secondPb = xml.indexOf("<pb n=\"001v\"");
        assertTrue(firstPb >= 0, "First pb missing");
        assertTrue(secondPb > firstPb, "Second pb missing or not after first");
    }

    @Test
    void rubricLine_generatesHiRubric(@TempDir Path tempDir) throws IOException {
        Path input = writeTextFile(tempDir, "Book.transcription.002r.txt",
                "[Book]\n[2r a]\n\n<rubric>L'Amant</rubric>\n\n");

        Path output = tempDir.resolve("Book.transcription.xml");
        var converter = new TranscriptionConverter();
        converter.convert(List.of(input), output);

        assertTrue(converter.getErrors().isEmpty(), "Errors: " + converter.getErrors());
        String xml = readOutput(output);
        assertTrue(xml.contains("<hi rend=\"rubric\">"), "Expected rubric hi element");
        assertTrue(xml.contains("L'Amant"), "Expected rubric text");
    }

    @Test
    void illustration_generatesFigure(@TempDir Path tempDir) throws IOException {
        Path input = writeTextFile(tempDir, "Book.transcription.003r.txt",
                "[Book]\n[3r a]\n\n<illustration characters=\"Lover\">Lover in garden</illustration>\n\n");

        Path output = tempDir.resolve("Book.transcription.xml");
        var converter = new TranscriptionConverter();
        converter.convert(List.of(input), output);

        assertTrue(converter.getErrors().isEmpty(), "Errors: " + converter.getErrors());
        String xml = readOutput(output);
        assertTrue(xml.contains("<figure"), "Expected figure element");
        assertTrue(xml.contains("type=\"miniature\""), "Expected miniature type");
        assertTrue(xml.contains("<head>"), "Expected head element");
        assertTrue(xml.contains("Lover in garden"), "Expected illustration text");
        assertTrue(xml.contains("<note type=\"character\""), "Expected character note");
        assertTrue(xml.contains("Lover"), "Expected character name");
    }

    @Test
    void catchphrase_generatesFw(@TempDir Path tempDir) throws IOException {
        Path input = writeTextFile(tempDir, "Book.transcription.004r.txt",
                "[Book]\n[4r a]\n\nLine1\nLine2\t\t100\n\n<catchphrase>Car beau chanter</catchphrase>\n\n");

        Path output = tempDir.resolve("Book.transcription.xml");
        var converter = new TranscriptionConverter();
        converter.convert(List.of(input), output);

        assertTrue(converter.getErrors().isEmpty(), "Errors: " + converter.getErrors());
        String xml = readOutput(output);
        assertTrue(xml.contains("<fw type=\"catch\""), "Expected fw catchword element");
        assertTrue(xml.contains("Car beau chanter"), "Expected catchphrase text");
    }

    @Test
    void abbreviation_generatesExpan(@TempDir Path tempDir) throws IOException {
        Path input = writeTextFile(tempDir, "Book.transcription.005r.txt",
                "[Book]\n[5r a]\n\nMainte/s/ gen/s/ dient\nSecond line\t\t200\n\n");

        Path output = tempDir.resolve("Book.transcription.xml");
        var converter = new TranscriptionConverter();
        converter.convert(List.of(input), output);

        assertTrue(converter.getErrors().isEmpty(), "Errors: " + converter.getErrors());
        String xml = readOutput(output);
        assertTrue(xml.contains("<expan>s</expan>"), "Expected expan element for abbreviation");
    }

    @Test
    void initial_generatesHiInit(@TempDir Path tempDir) throws IOException {
        Path input = writeTextFile(tempDir, "Book.transcription.006r.txt",
                "[Book]\n[6r a]\n\n<initial/>Maintes gens\nSecond line\t\t300\n\n");

        Path output = tempDir.resolve("Book.transcription.xml");
        var converter = new TranscriptionConverter();
        converter.convert(List.of(input), output);

        assertTrue(converter.getErrors().isEmpty(), "Errors: " + converter.getErrors());
        String xml = readOutput(output);
        assertTrue(xml.contains("<hi rend=\"init\">M</hi>"), "Expected init hi element");
    }

    @Test
    void folioOverrideFromFilename(@TempDir Path tempDir) throws IOException {
        // The file is named 042r but internally says [1r a] — filename wins
        Path input = writeTextFile(tempDir, "Book.transcription.042r.txt",
                "[Book]\n[1r a]\n\nTest line\nAnother\t\t400\n\n");

        Path output = tempDir.resolve("Book.transcription.xml");
        var converter = new TranscriptionConverter();
        converter.convert(List.of(input), output);

        assertTrue(converter.getErrors().isEmpty(), "Errors: " + converter.getErrors());
        String xml = readOutput(output);
        assertTrue(xml.contains("<pb n=\"042r\""), "Expected folio from filename");
    }

    @Test
    void noInputFiles_noOutput(@TempDir Path tempDir) throws IOException {
        Path output = tempDir.resolve("Book.transcription.xml");
        var converter = new TranscriptionConverter();
        converter.convert(List.of(), output);

        // With no input, StAX still creates a minimal document
        // The real check is no errors
        assertTrue(converter.getErrors().isEmpty());
    }

    @Test
    void douce332Integration(@TempDir Path tempDir) throws IOException {
        // Test against real Douce332 data - just verify it parses without errors
        Path testResources = Path.of("src/test/resources/archive/rose/Douce332");
        if (!Files.isDirectory(testResources)) return; // skip if not present

        List<Path> txtFiles;
        try (var stream = Files.list(testResources)) {
            txtFiles = stream
                    .filter(p -> p.getFileName().toString().startsWith("Douce332.transcription.")
                            && p.getFileName().toString().endsWith(".txt"))
                    .sorted()
                    .toList();
        }
        if (txtFiles.isEmpty()) return;

        Path output = tempDir.resolve("Douce332.transcription.xml");
        var converter = new TranscriptionConverter();
        converter.convert(txtFiles, output);

        assertTrue(converter.getErrors().isEmpty(),
                "Errors converting Douce332: " + converter.getErrors());
        assertTrue(Files.exists(output));
        String xml = readOutput(output);
        assertTrue(xml.contains("<TEI"), "Expected TEI root element");
        assertTrue(xml.contains("<pb"), "Expected page breaks");
        assertTrue(xml.length() > 10000, "Expected substantial output");
    }
}
