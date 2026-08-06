package rosa.archive.cli;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for the standalone RenameFilesCommand.
 */
class RenameFilesCommandTest {

    @TempDir
    Path tempDir;

    private int execute(String... args) {
        return new CommandLine(new RenameFilesCommand()).execute(args);
    }

    /** Captures System.err output during command execution. */
    private record CapturedExec(int exitCode, String stderr) {}

    private CapturedExec executeCapturingErr(String... args) {
        PrintStream original = System.err;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        System.setErr(new PrintStream(baos));
        try {
            int exitCode = new CommandLine(new RenameFilesCommand()).execute(args);
            return new CapturedExec(exitCode, baos.toString());
        } finally {
            System.setErr(original);
        }
    }

    @Test
    void renamesFilesAccordingToCsv() throws IOException {
        Files.writeString(tempDir.resolve("a.txt"), "content-a");
        Files.writeString(tempDir.resolve("b.txt"), "content-b");

        Path csv = tempDir.resolve("mapping.csv");
        Files.writeString(csv, "a.txt,alpha.txt\nb.txt,beta.txt\n");

        int exitCode = execute(tempDir.toString(), csv.toString());

        assertEquals(0, exitCode);
        assertFalse(Files.exists(tempDir.resolve("a.txt")));
        assertFalse(Files.exists(tempDir.resolve("b.txt")));
        assertTrue(Files.exists(tempDir.resolve("alpha.txt")));
        assertTrue(Files.exists(tempDir.resolve("beta.txt")));
        assertEquals("content-a", Files.readString(tempDir.resolve("alpha.txt")));
        assertEquals("content-b", Files.readString(tempDir.resolve("beta.txt")));
    }

    @Test
    void reportsErrorForMissingSourceFile() throws IOException {
        Path csv = tempDir.resolve("mapping.csv");
        Files.writeString(csv, "nonexistent.txt,target.txt\n");

        var result = executeCapturingErr(tempDir.toString(), csv.toString());

        assertEquals(1, result.exitCode());
        assertTrue(result.stderr().contains("Source file not found: nonexistent.txt"));
    }

    @Test
    void reportsErrorForMalformedCsvLine() throws IOException {
        Files.writeString(tempDir.resolve("a.txt"), "content");

        Path csv = tempDir.resolve("mapping.csv");
        Files.writeString(csv, "a.txt,alpha.txt\nbad-line-no-comma\n");

        var result = executeCapturingErr(tempDir.toString(), csv.toString());

        assertEquals(1, result.exitCode());
        assertTrue(result.stderr().contains("Invalid CSV line: bad-line-no-comma"));
        // The valid rename should still have been performed
        assertTrue(Files.exists(tempDir.resolve("alpha.txt")));
    }

    @Test
    void exitsWithErrorForNonexistentDirectory() throws IOException {
        Path csv = tempDir.resolve("mapping.csv");
        Files.writeString(csv, "a.txt,b.txt\n");

        var result = executeCapturingErr("/nonexistent/dir", csv.toString());

        assertEquals(1, result.exitCode());
        assertTrue(result.stderr().contains("Directory does not exist"));
    }

    @Test
    void exitsWithErrorForNonexistentCsvFile() {
        var result = executeCapturingErr(tempDir.toString(), "/nonexistent/file.csv");

        assertEquals(1, result.exitCode());
        assertTrue(result.stderr().contains("CSV file does not exist or is not readable"));
    }

    @Test
    void handlesBlankFieldsInCsvLine() throws IOException {
        Files.writeString(tempDir.resolve("a.txt"), "content");

        Path csv = tempDir.resolve("mapping.csv");
        Files.writeString(csv, " ,target.txt\na.txt, \n");

        var result = executeCapturingErr(tempDir.toString(), csv.toString());

        assertEquals(1, result.exitCode());
        assertTrue(result.stderr().contains("Invalid CSV line"));
    }

    @Test
    void helpExitsWithZero() {
        int exitCode = execute("--help");
        assertEquals(0, exitCode);
    }
}
