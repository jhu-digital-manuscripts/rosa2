package rosa.archive.cli;

import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;
import rosa.archive.aor.AoRTranscriptionChecker;
import rosa.archive.core.CheckResult;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Callable;

/**
 * Checks the AoR transcription XML files of a collection.
 *
 * <p>Every transcription must be well-formed XML and, with {@code --schema}, valid against
 * the given schema. People, book and location references in annotations must match entries
 * in the reference CSV files, annotation IDs must be unique, internal reference targets must
 * resolve, and transcription file names must agree with the page's image name.</p>
 */
@Command(name = "check-aor",
         mixinStandardHelpOptions = true, versionProvider = VersionProvider.class,
         description = "Check AoR transcriptions: well-formed XML (valid against a local XML Schema with --schema), "
                 + "references against the reference spreadsheets, unique annotation ids, internal reference targets")
public final class CheckAorCommand implements Callable<Integer> {

    @Mixin
    private ArchiveOptions archive;

    @Option(names = "--collection", required = true, description = "Collection ID")
    private String collectionId;

    @Option(names = "--book", description = "Book ID (if omitted, checks all books in collection)")
    private String bookId;

    @Option(names = "--spreadsheet-dir", description = "Directory containing reference spreadsheets")
    private Path spreadsheetDir;

    @Option(names = "--schema", description = "Local XML Schema (.xsd) file to validate the transcriptions against; "
            + "the files' own schemaLocation is never fetched")
    private Path schema;

    /**
     * Executes the AoR transcription checks.
     *
     * @return 0 if no errors were found, 1 if errors were detected or a fatal error occurred
     */
    @Override
    public Integer call() {
        if (!archive.exists()) {
            return 1;
        }

        if (spreadsheetDir != null && !Files.isDirectory(spreadsheetDir)) {
            System.err.println("Error: spreadsheet directory does not exist: " + spreadsheetDir);
            return 1;
        }

        if (schema != null && !Files.isRegularFile(schema)) {
            System.err.println("Error: schema file does not exist: " + schema);
            return 1;
        }

        var checker = new AoRTranscriptionChecker(archive.path(), spreadsheetDir, schema);
        CheckResult result = checker.run(collectionId, bookId);

        result.errors().forEach(error -> System.out.println("ERROR: " + error));
        result.warnings().forEach(warning -> System.out.println("WARN:  " + warning));
        return result.errors().isEmpty() ? 0 : 1;
    }
}
