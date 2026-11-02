package rosa.archive.cli;

import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

import java.io.IOException;
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
         mixinStandardHelpOptions = true, versionProvider = VersionProvider.class,
         description = "Rename image files using the file map")
public final class RenameImagesCommand implements Callable<Integer> {

    @Mixin
    private ArchiveOptions archive;

    @Option(names = "--collection", required = true, description = "Collection ID")
    private String collectionId;

    @Option(names = "--book", required = true, description = "Book ID")
    private String bookId;

    @ArgGroup
    private Mode mode = new Mode();

    /** The two renaming modes exclude each other. */
    static final class Mode {
        @Option(names = "--change-id",
                description = "Instead of applying the file map, replace the ID prefix of every image file name with the book directory name")
        boolean changeId;

        @Option(names = "--reverse", description = "Apply the file map in reverse (new-to-old)")
        boolean reverse;
    }

    /**
     * Executes the image rename operation.
     *
     * @return 0 if no errors occurred, 1 otherwise
     */
    @Override
    public Integer call() {
        if (!archive.exists()) {
            return 1;
        }

        try {
            var store = archive.openStore();
            List<String> errors = new ArrayList<>();

            store.renameImages(collectionId, bookId, mode.changeId, mode.reverse, errors);

            errors.forEach(System.err::println);
            return errors.isEmpty() ? 0 : 1;
        } catch (IOException e) {
            System.err.println("Error renaming images: " + e.getMessage());
            return 1;
        }
    }
}
