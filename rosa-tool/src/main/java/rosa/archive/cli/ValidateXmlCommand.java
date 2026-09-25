package rosa.archive.cli;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import rosa.archive.core.FileSystemArchiveStore;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * Validates AoR transcription XML files against the schema.
 *
 * <p>Iterates over all books in the specified collection, validating each
 * AoR transcription XML file for well-formedness. Reports errors and warnings
 * to standard output.</p>
 */
@Command(name = "validate-xml",
         mixinStandardHelpOptions = true,
         description = "Validate AoR transcription XML files against schema")
public final class ValidateXmlCommand implements Callable<Integer> {

    @Option(names = "--archive", required = true, description = "Path to archive directory")
    private Path archivePath;

    @Option(names = "--collection", required = true, description = "Collection ID to validate")
    private String collectionId;

    /**
     * Executes the XML validation operation.
     *
     * @return 0 if no errors found, 1 on error
     */
    @Override
    public Integer call() {
        if (!Files.isDirectory(archivePath)) {
            System.err.println("Error: archive path does not exist or is not a directory: " + archivePath);
            return 1;
        }

        try {
            var store = new FileSystemArchiveStore(archivePath);
            List<String> bookIds = store.listBooks(collectionId);

            boolean hasErrors = false;

            for (String bookId : bookIds) {
                List<String> errors = new ArrayList<>();
                List<String> warnings = new ArrayList<>();

                store.validateXml(collectionId, bookId, errors, warnings);

                if (!errors.isEmpty() || !warnings.isEmpty()) {
                    System.out.println("[" + collectionId + "/" + bookId + "]");

                    for (String error : errors) {
                        System.out.println("  ERROR: " + error);
                    }
                    for (String warning : warnings) {
                        System.out.println("  WARN:  " + warning);
                    }
                }

                if (!errors.isEmpty()) {
                    hasErrors = true;
                }
            }

            return hasErrors ? 1 : 0;
        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
            return 1;
        }
    }
}
