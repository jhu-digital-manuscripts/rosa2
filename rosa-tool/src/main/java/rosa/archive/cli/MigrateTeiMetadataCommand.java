package rosa.archive.cli;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import rosa.archive.core.TeiMetadataMigrator;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * Migrates book metadata from TEI description files into the custom XML metadata format.
 *
 * <p>For each book in the specified collection (or a single book if {@code --book} is provided),
 * reads language-specific TEI description files and writes a consolidated
 * {@code <bookId>.metadata.xml} file.</p>
 */
@Command(name = "migrate-tei-metadata",
         mixinStandardHelpOptions = true,
         description = "Migrate metadata from TEI description files to custom XML format")
public final class MigrateTeiMetadataCommand implements Callable<Integer> {

    @Option(names = "--archive", required = true, description = "Path to archive directory")
    private Path archivePath;

    @Option(names = "--collection", required = true, description = "Collection ID")
    private String collectionId;

    @Option(names = "--book", description = "Book ID (if omitted, processes all books in collection)")
    private String bookId;

    /**
     * Executes the TEI metadata migration.
     *
     * @return 0 if no errors occurred, 1 if errors were detected or a fatal error occurred
     */
    @Override
    public Integer call() {
        if (!Files.isDirectory(archivePath)) {
            System.err.println("Error: archive path does not exist or is not a directory: " + archivePath);
            return 1;
        }

        var migrator = new TeiMetadataMigrator(archivePath);
        List<String> errors = new ArrayList<>();

        if (bookId != null) {
            migrator.migrateBook(collectionId, bookId, errors);
        } else {
            migrator.migrateCollection(collectionId, errors);
        }

        errors.forEach(System.err::println);
        return errors.isEmpty() ? 0 : 1;
    }
}
