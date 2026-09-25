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
 * Renames image files in a book directory according to the file map ({@code filemap.csv}).
 *
 * <p>When {@code --reverse} is false (the default), renames old filenames to new filenames
 * as defined in the file map. When {@code --reverse} is true, renames new filenames back
 * to old filenames. When {@code --change-id} is true, replaces the ID prefix in image
 * filenames with the book directory name.</p>
 *
 * <p>Requires {@code filemap.csv} to exist in the book directory. Reports an error
 * and exits with code 1 if it is missing.</p>
 */
@Command(name = "rename-images",
         mixinStandardHelpOptions = true,
         description = "Rename image files using the file map")
public final class RenameImagesCommand implements Callable<Integer> {

    @Option(names = "--archive", required = true, description = "Path to archive directory")
    private Path archivePath;

    @Option(names = "--collection", required = true, description = "Collection ID")
    private String collectionId;

    @Option(names = "--book", required = true, description = "Book ID")
    private String bookId;

    @Option(names = "--change-id", description = "Also change ID prefix portion of filenames")
    private boolean changeId;

    @Option(names = "--reverse", description = "Apply renaming in reverse (new-to-old)")
    private boolean reverse;

    /**
     * Executes the image rename operation.
     *
     * @return 0 if no errors occurred, 1 otherwise
     */
    @Override
    public Integer call() {
        if (!Files.isDirectory(archivePath)) {
            System.err.println("Error: archive path does not exist or is not a directory: " + archivePath);
            return 1;
        }

        try {
            var store = new FileSystemArchiveStore(archivePath);
            List<String> errors = new ArrayList<>();

            store.renameImages(collectionId, bookId, changeId, reverse, errors);

            errors.forEach(System.err::println);
            return errors.isEmpty() ? 0 : 1;
        } catch (IOException e) {
            System.err.println("Error renaming images: " + e.getMessage());
            return 1;
        }
    }
}
