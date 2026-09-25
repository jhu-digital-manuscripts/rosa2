package rosa.archive.cli;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import rosa.archive.core.CheckResult;
import rosa.archive.core.FileSystemArchiveStore;
import rosa.archive.model.Book;
import rosa.archive.model.BookCollection;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * Verifies structural data consistency of archive collections and books.
 *
 * <p>Iterates over all collections and books in the archive, checking structural
 * integrity. When {@code --check-bits} is specified, also verifies SHA1 checksums
 * for bit-level integrity.</p>
 */
@Command(name = "check",
         mixinStandardHelpOptions = true,
         description = "Check archive data consistency")
public final class CheckCommand implements Callable<Integer> {

    @Option(names = "--archive", required = true, description = "Path to archive directory")
    private Path archivePath;

    @Option(names = "--check-bits", description = "Also verify bit-level integrity via SHA1 checksums")
    private boolean checkBits;

    /**
     * Executes the check operation on all collections and books.
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
            List<String> collections = store.listCollections();

            if (collections.isEmpty()) {
                System.out.println("No collections found in archive.");
                return 0;
            }

            boolean hasErrors = false;

            for (String collectionId : collections) {
                BookCollection collection = store.loadCollection(collectionId);
                List<String> bookIds = store.listBooks(collectionId);

                for (String bookId : bookIds) {
                    Book book = store.loadBook(collection, bookId);
                    CheckResult result = store.check(collection, book, checkBits);

                    if (!result.errors().isEmpty() || !result.warnings().isEmpty()) {
                        System.out.println("[" + collectionId + "/" + bookId + "]");

                        for (String error : result.errors()) {
                            System.out.println("  ERROR: " + error);
                        }
                        for (String warning : result.warnings()) {
                            System.out.println("  WARN:  " + warning);
                        }
                    }

                    if (!result.passed()) {
                        hasErrors = true;
                    }
                }
            }

            return hasErrors ? 1 : 0;
        } catch (IOException e) {
            System.err.println("Error checking archive: " + e.getMessage());
            return 1;
        }
    }
}
