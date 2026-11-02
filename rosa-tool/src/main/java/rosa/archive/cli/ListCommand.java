package rosa.archive.cli;

import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * Lists collections or books in the archive.
 *
 * <p>When invoked without {@code --collection}, prints all collection names.
 * When {@code --collection} is provided, prints all book names within that collection.
 * Output is one name per line to standard output.</p>
 */
@Command(name = "list",
         mixinStandardHelpOptions = true, versionProvider = VersionProvider.class,
         description = "List collections or books in the archive")
public final class ListCommand implements Callable<Integer> {

    @Mixin
    private ArchiveOptions archive;

    @Option(names = "--collection", description = "Collection ID to list books for")
    private String collectionId;

    /**
     * Executes the list operation.
     *
     * @return 0 on success, 1 on error
     */
    @Override
    public Integer call() {
        if (!archive.exists()) {
            return 1;
        }

        try {
            var store = archive.openStore();

            if (collectionId != null) {
                List<String> books = store.listBooks(collectionId);
                for (String book : books) {
                    System.out.println(book);
                }
            } else {
                List<String> collections = store.listCollections();
                for (String collection : collections) {
                    System.out.println(collection);
                }
            }

            return 0;
        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
            return 1;
        }
    }
}
