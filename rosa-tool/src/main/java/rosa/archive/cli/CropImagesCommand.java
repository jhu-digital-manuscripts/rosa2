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
 * Crops images based on stored crop data and generates cropped image lists.
 *
 * <p>Reads crop coordinates from {@code <bookId>.crop.txt}, applies the crop region
 * to each image file, writes cropped versions to a {@code cropped/} subdirectory,
 * and generates the cropped image list CSV ({@code <bookId>.images.crop.csv}).
 *
 * <p>If {@code --book} is provided, only that book is processed. Otherwise, all
 * books in the specified collection are processed.</p>
 */
@Command(name = "crop-images",
         mixinStandardHelpOptions = true,
         description = "Crop images based on stored crop data")
public final class CropImagesCommand implements Callable<Integer> {

    @Option(names = "--archive", required = true, description = "Path to archive directory")
    private Path archivePath;

    @Option(names = "--collection", required = true, description = "Collection ID")
    private String collectionId;

    @Option(names = "--book", description = "Book ID (if omitted, processes all books in collection)")
    private String bookId;

    @Option(names = "--force", description = "Overwrite existing cropped images")
    private boolean force;

    /**
     * Executes the crop images operation.
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

            if (bookId != null) {
                store.cropImages(collectionId, bookId, force, errors);
                store.generateAndWriteCropList(collectionId, bookId, force, errors);
            } else {
                for (String book : store.listBooks(collectionId)) {
                    store.cropImages(collectionId, book, force, errors);
                    store.generateAndWriteCropList(collectionId, book, force, errors);
                }
            }

            errors.forEach(System.err::println);
            return errors.isEmpty() ? 0 : 1;
        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
            return 1;
        }
    }
}
