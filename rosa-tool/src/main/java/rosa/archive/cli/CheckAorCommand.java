package rosa.archive.cli;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import rosa.archive.aor.AoRTranscriptionChecker;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * Validates AoR transcription data against reference spreadsheets.
 *
 * <p>Checks that people, book, and location references in annotations match
 * entries in the respective reference CSV files. Also detects duplicate annotation
 * IDs, validates internal reference targets, and checks transcription filename
 * consistency with image names.</p>
 */
@Command(name = "check-aor",
         mixinStandardHelpOptions = true,
         description = "Validate AoR transcription data against reference spreadsheets")
public final class CheckAorCommand implements Callable<Integer> {

    @Option(names = "--archive", required = true, description = "Path to archive directory")
    private Path archivePath;

    @Option(names = "--collection", required = true, description = "Collection ID")
    private String collectionId;

    @Option(names = "--book", description = "Book ID (if omitted, checks all books in collection)")
    private String bookId;

    @Option(names = "--spreadsheet-dir", description = "Directory containing reference spreadsheets")
    private Path spreadsheetDir;

    /**
     * Executes the AoR transcription validation.
     *
     * @return 0 if no errors were found, 1 if errors were detected or a fatal error occurred
     */
    @Override
    public Integer call() {
        if (!Files.isDirectory(archivePath)) {
            System.err.println("Error: archive path does not exist or is not a directory: " + archivePath);
            return 1;
        }

        if (spreadsheetDir != null && !Files.isDirectory(spreadsheetDir)) {
            System.err.println("Error: spreadsheet directory does not exist: " + spreadsheetDir);
            return 1;
        }

        var checker = new AoRTranscriptionChecker(archivePath, spreadsheetDir);
        List<String> errors = checker.run(collectionId, bookId);

        errors.forEach(System.out::println);
        return errors.isEmpty() ? 0 : 1;
    }
}
