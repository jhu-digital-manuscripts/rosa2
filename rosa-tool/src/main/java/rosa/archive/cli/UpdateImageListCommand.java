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
 * Generates an image list CSV from image files in a book directory.
 *
 * <p>Scans the specified book directory for image files ({@code .tif}, {@code .jpg}),
 * reads their dimensions, and writes the image list as
 * {@code <bookId>.images.csv}. When {@code --force} is not specified, an
 * existing image list file is left unchanged.</p>
 */
@Command(name = "update-image-list",
         mixinStandardHelpOptions = true,
         description = "Generate image list CSV from image files in a book directory")
public final class UpdateImageListCommand implements Callable<Integer> {

    @Option(names = "--archive", required = true, description = "Path to archive directory")
    private Path archivePath;

    @Option(names = "--collection", required = true, description = "Collection ID")
    private String collectionId;

    @Option(names = "--book", required = true, description = "Book ID")
    private String bookId;

    @Option(names = "--force", description = "Overwrite existing image list")
    private boolean force;

    /**
     * Executes the image list generation operation.
     *
     * @return 0 on success, 1 if errors occurred
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

            store.generateAndWriteImageList(collectionId, bookId, force, errors);

            errors.forEach(System.err::println);
            return errors.isEmpty() ? 0 : 1;
        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
            return 1;
        }
    }
}
