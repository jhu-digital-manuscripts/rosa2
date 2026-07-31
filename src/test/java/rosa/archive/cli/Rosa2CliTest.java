package rosa.archive.cli;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import picocli.CommandLine;

import java.io.PrintWriter;
import java.io.StringWriter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for the Rosa2Cli main class, command registration, help output, and error handling.
 */
class Rosa2CliTest {

    @Test
    void noArgsExitsWithNonZero() {
        int exitCode = new CommandLine(new Rosa2Cli()).execute();
        assertEquals(1, exitCode);
    }

    @Test
    void helpExitsWithZero() {
        int exitCode = new CommandLine(new Rosa2Cli()).execute("--help");
        assertEquals(0, exitCode);
    }

    @Test
    void helpOutputContainsDescriptionAndAllCommands() {
        StringWriter out = new StringWriter();
        CommandLine cmd = new CommandLine(new Rosa2Cli());
        cmd.setOut(new PrintWriter(out));
        int exitCode = cmd.execute("--help");
        assertEquals(0, exitCode);
        String output = out.toString();
        assertTrue(output.contains("CLI tool for managing rosa digital manuscript archives"));
        assertTrue(output.contains("generate-iiif-pres"));
        assertTrue(output.contains("generate-opensearch-ingest"));
        assertTrue(output.contains("shallow-copy"));
        assertTrue(output.contains("check"));
        assertTrue(output.contains("list"));
        assertTrue(output.contains("validate-xml"));
        assertTrue(output.contains("aor-stats"));
        assertTrue(output.contains("update"));
        assertTrue(output.contains("update-image-list"));
        assertTrue(output.contains("crop-images"));
        assertTrue(output.contains("file-map"));
        assertTrue(output.contains("rename-images"));
        assertTrue(output.contains("rename-files"));
        assertTrue(output.contains("rename-transcriptions"));
        assertTrue(output.contains("generate-tei"));
        assertTrue(output.contains("check-aor"));
        assertTrue(output.contains("generate-annotation-map"));
        assertTrue(output.contains("migrate-tei-metadata"));
        assertTrue(output.contains("decorate-image-list"));
    }

    @Test
    void invalidCommandExitsWithOne() {
        StringWriter err = new StringWriter();
        CommandLine cmd = new CommandLine(new Rosa2Cli());
        cmd.setErr(new PrintWriter(err));
        cmd.setParameterExceptionHandler((ex, args) -> {
            cmd.getErr().println("Error: " + ex.getMessage());
            cmd.usage(cmd.getErr());
            return 1;
        });
        int exitCode = cmd.execute("nonexistent-command");
        assertEquals(1, exitCode);
        String output = err.toString();
        assertTrue(output.contains("nonexistent-command"));
    }

    @Test
    void missingRequiredOptionsExitsWithOne() {
        StringWriter err = new StringWriter();
        CommandLine cmd = new CommandLine(new Rosa2Cli());
        cmd.setErr(new PrintWriter(err));
        cmd.setParameterExceptionHandler((ex, args) -> {
            CommandLine subCmd = ex.getCommandLine();
            subCmd.getErr().println("Error: " + ex.getMessage());
            subCmd.usage(subCmd.getErr());
            return 1;
        });
        int exitCode = cmd.execute("generate-iiif-pres");
        assertEquals(1, exitCode);
        String output = err.toString();
        assertTrue(output.contains("Missing required option"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "generate-iiif-pres",
            "generate-opensearch-ingest",
            "shallow-copy",
            "check",
            "list",
            "validate-xml",
            "aor-stats",
            "update",
            "update-image-list",
            "crop-images",
            "file-map",
            "rename-images",
            "rename-files",
            "rename-transcriptions",
            "generate-tei",
            "check-aor",
            "generate-annotation-map",
            "migrate-tei-metadata",
            "decorate-image-list"
    })
    void subcommandHelpExitsWithZero(String subcommand) {
        StringWriter out = new StringWriter();
        CommandLine cmd = new CommandLine(new Rosa2Cli());
        cmd.setOut(new PrintWriter(out));
        int exitCode = cmd.execute(subcommand, "--help");
        assertEquals(0, exitCode, "Expected exit code 0 for --help on " + subcommand);
        String output = out.toString();
        assertTrue(output.contains("--help"), "Help output for " + subcommand + " should mention --help option");
        assertTrue(output.length() > 50, "Help output for " + subcommand + " should contain meaningful content");
    }

    @Test
    void allSubcommandsRegistered() {
        CommandLine cmd = new CommandLine(new Rosa2Cli());
        var subcommands = cmd.getSubcommands();
        assertTrue(subcommands.containsKey("generate-iiif-pres"));
        assertTrue(subcommands.containsKey("generate-opensearch-ingest"));
        assertTrue(subcommands.containsKey("shallow-copy"));
        assertTrue(subcommands.containsKey("check"));
        assertTrue(subcommands.containsKey("list"));
        assertTrue(subcommands.containsKey("validate-xml"));
        assertTrue(subcommands.containsKey("aor-stats"));
        assertTrue(subcommands.containsKey("update"));
        assertTrue(subcommands.containsKey("update-image-list"));
        assertTrue(subcommands.containsKey("crop-images"));
        assertTrue(subcommands.containsKey("file-map"));
        assertTrue(subcommands.containsKey("rename-images"));
        assertTrue(subcommands.containsKey("rename-files"));
        assertTrue(subcommands.containsKey("rename-transcriptions"));
        assertTrue(subcommands.containsKey("generate-tei"));
        assertTrue(subcommands.containsKey("check-aor"));
        assertTrue(subcommands.containsKey("generate-annotation-map"));
        assertTrue(subcommands.containsKey("migrate-tei-metadata"));
        assertTrue(subcommands.containsKey("decorate-image-list"));
        assertEquals(19, subcommands.size());
    }

    @Test
    void versionExitsWithZero() {
        int exitCode = new CommandLine(new Rosa2Cli()).execute("--version");
        assertEquals(0, exitCode);
    }
}
