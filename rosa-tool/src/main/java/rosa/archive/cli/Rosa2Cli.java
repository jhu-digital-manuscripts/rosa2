package rosa.archive.cli;

import picocli.CommandLine;
import picocli.CommandLine.Command;

import java.util.concurrent.Callable;

/**
 * Main entry point for the rosa2 CLI tool.
 *
 * <p>Provides subcommands for managing rosa digital manuscript archives,
 * generating IIIF Presentation API 3.0 files, Opensearch bulk ingest files,
 * and AoR annotation statistics.</p>
 */
@Command(name = "rosa2", version = "2.0.0-SNAPSHOT",
         mixinStandardHelpOptions = true,
         description = "CLI tool for managing rosa digital manuscript archives",
         subcommands = {
             GenerateIiifPresCommand.class,
             GenerateOpensearchIngestCommand.class,
             ShallowCopyCommand.class,
             CheckCommand.class,
             ListCommand.class,
             ValidateXmlCommand.class,
             AorStatsCommand.class,
             UpdateChecksumCommand.class,
             UpdateImageListCommand.class,
             CropImagesCommand.class,
             FileMapCommand.class,
             RenameImagesCommand.class,
             RenameFilesCommand.class,
             RenameTranscriptionsCommand.class,
             GenerateTeiCommand.class,
             CheckAorCommand.class,
             GenerateAnnotationMapCommand.class,
             MigrateTeiMetadataCommand.class,
             DecorateImageListCommand.class
         })
public final class Rosa2Cli implements Callable<Integer> {

    /**
     * When invoked with no subcommand, prints usage and exits with code 1.
     *
     * @return exit code 1 indicating no command was provided
     */
    @Override
    public Integer call() {
        CommandLine cmd = new CommandLine(this);
        cmd.usage(System.err);
        return 1;
    }

    /**
     * Application entry point.
     *
     * @param args command-line arguments
     */
    public static void main(String[] args) {
        int exitCode = new CommandLine(new Rosa2Cli())
                .setExecutionExceptionHandler((ex, _, _) -> {
                    System.err.println("Error: " + ex.getMessage());
                    return 1;
                })
                .setParameterExceptionHandler((ex, _) -> {
                    CommandLine cmd = ex.getCommandLine();
                    cmd.getErr().println("Error: " + ex.getMessage());
                    cmd.usage(cmd.getErr());
                    return 1;
                })
                .execute(args);
        System.exit(exitCode);
    }
}
