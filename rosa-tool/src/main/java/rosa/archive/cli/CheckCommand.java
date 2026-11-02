package rosa.archive.cli;

import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;
import rosa.archive.core.CheckResult;
import rosa.archive.model.Book;
import rosa.archive.model.BookCollection;

import java.io.IOException;
import java.util.ArrayList;
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
         mixinStandardHelpOptions = true, versionProvider = VersionProvider.class,
         description = "Check archive data consistency: readable files, image lists against files on disk, metadata presence, optionally checksums")
public final class CheckCommand implements Callable<Integer> {

    @Mixin
    private ArchiveOptions archive;

    @Option(names = "--check-bits", description = "Also verify bit-level integrity via SHA1 checksums")
    private boolean checkBits;

    @Option(names = "--skip-images",
            description = "Do not report listed images that are absent on disk (for shallow copies without images)")
    private boolean skipImages;

    /**
     * Executes the check operation on all collections and books.
     *
     * @return 0 if no errors found, 1 on error
     */
    @Override
    public Integer call() {
        if (!archive.exists()) {
            return 1;
        }

        try {
            var store = archive.openStore();
            List<String> collections = store.listCollections();

            if (collections.isEmpty()) {
                System.out.println("No collections found in archive.");
                return 0;
            }

            boolean hasErrors = false;
            for (String collectionId : collections) {
                List<String> loadErrors = new ArrayList<>();
                BookCollection collection = store.loadCollection(collectionId, loadErrors);
                List<String> collectionWarnings = new ArrayList<>();
                // Book-less parent collections such as dlmm carry no checksum file
                if (collection.getChecksum() == null && collection.books() != null && collection.books().length > 0) {
                    collectionWarnings.add("Collection checksum file " + collectionId + "collection.SHA1SUM not found.");
                }
                hasErrors |= report("[" + collectionId + "]", loadErrors, collectionWarnings);

                List<String> bookIds = store.listBooks(collectionId);
                for (String bookId : bookIds) {
                    List<String> errors = new ArrayList<>();
                    Book book = store.loadBook(collection, bookId, errors);
                    CheckResult result = store.check(collection, book, checkBits, !skipImages);
                    errors.addAll(result.errors());
                    hasErrors |= report("[" + collectionId + "/" + bookId + "]", errors, result.warnings());
                }
            }
            return hasErrors ? 1 : 0;
        } catch (IOException e) {
            System.err.println("Error checking archive: " + e.getMessage());
            return 1;
        }
    }

    /**
     * Prints the errors and warnings of one archive item, if any.
     *
     * @return true if there were errors
     */
    private static boolean report(String heading, List<String> errors, List<String> warnings) {
        if (!errors.isEmpty() || !warnings.isEmpty()) {
            System.out.println(heading);
            for (String error : errors) {
                System.out.println("  ERROR: " + error);
            }
            for (String warning : warnings) {
                System.out.println("  WARN:  " + warning);
            }
        }
        return !errors.isEmpty();
    }
}
