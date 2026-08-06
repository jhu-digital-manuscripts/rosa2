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
 * Recomputes SHA1 checksums for archive collections and books.
 *
 * <p>When invoked with only {@code --archive}, updates all collections and books.
 * When {@code --collection} is specified, updates that collection and its books.
 * When both {@code --collection} and {@code --book} are specified, updates only
 * the specified book. The {@code --force} flag causes all checksums to be
 * recomputed regardless of file modification dates.</p>
 */
@Command(name = "update",
         mixinStandardHelpOptions = true,
         description = "Recompute SHA1 checksums for collections and books")
public final class UpdateChecksumCommand implements Callable<Integer> {

    @Option(names = "--archive", required = true, description = "Path to archive directory")
    private Path archivePath;

    @Option(names = "--collection", description = "Collection ID (if omitted, processes all collections)")
    private String collectionId;

    @Option(names = "--book", description = "Book ID (requires --collection)")
    private String bookId;

    @Option(names = "--force", description = "Force recomputation of all checksums regardless of modification date")
    private boolean force;

    /**
     * Executes the checksum update operation.
     *
     * @return 0 if no errors occurred, 1 otherwise
     */
    @Override
    public Integer call() {
        if (!Files.isDirectory(archivePath)) {
            System.err.println("Error: archive path does not exist or is not a directory: " + archivePath);
            return 1;
        }

        if (bookId != null && collectionId == null) {
            System.err.println("Error: --book requires --collection to be specified");
            return 1;
        }

        try {
            var store = new FileSystemArchiveStore(archivePath);
            List<String> errors = new ArrayList<>();

            if (bookId != null) {
                // Update single book
                store.updateChecksum(collectionId, bookId, force, errors);
            } else if (collectionId != null) {
                // Update collection + all books in it
                store.updateChecksum(collectionId, force, errors);
                for (String book : store.listBooks(collectionId)) {
                    store.updateChecksum(collectionId, book, force, errors);
                }
            } else {
                // Update all collections and all books
                for (String col : store.listCollections()) {
                    store.updateChecksum(col, force, errors);
                    for (String book : store.listBooks(col)) {
                        store.updateChecksum(col, book, force, errors);
                    }
                }
            }

            errors.forEach(System.err::println);
            return errors.isEmpty() ? 0 : 1;
        } catch (IOException e) {
            System.err.println("Error updating checksums: " + e.getMessage());
            return 1;
        }
    }
}
