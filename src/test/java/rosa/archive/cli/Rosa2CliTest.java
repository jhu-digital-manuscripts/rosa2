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
    void helpOutputContainsDescriptionAndCommands() {
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
            "aor-stats"
    })
    void subcommandHelpExitsWithZero(String subcommand) {
        StringWriter out = new StringWriter();
        CommandLine cmd = new CommandLine(new Rosa2Cli());
        cmd.setOut(new PrintWriter(out));
        int exitCode = cmd.execute(subcommand, "--help");
        assertEquals(0, exitCode);
        String output = out.toString();
        assertTrue(output.contains("--help"), "Help output should mention --help option");
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
        assertEquals(7, subcommands.size());
    }

    @Test
    void versionExitsWithZero() {
        int exitCode = new CommandLine(new Rosa2Cli()).execute("--version");
        assertEquals(0, exitCode);
    }
}
