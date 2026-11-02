package rosa.archive.cli;

import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

import java.io.IOException;
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
         mixinStandardHelpOptions = true, versionProvider = VersionProvider.class,
         description = "Crop images into cropped/ using {book}.crop.txt and write {book}.images.crop.csv")
public final class CropImagesCommand implements Callable<Integer> {

    @Mixin
    private ArchiveOptions archive;

    @Option(names = "--collection", required = true, description = "Collection ID")
    private String collectionId;

    @Option(names = "--book", description = "Book ID (if omitted, processes all books in collection)")
    private String bookId;

    @Option(names = "--force", description = "Overwrite existing cropped images and the cropped image list")
    private boolean force;

    /**
     * Executes the crop images operation.
     *
     * @return 0 on success, 1 if errors occurred
     */
    @Override
    public Integer call() {
        if (!archive.exists()) {
            return 1;
        }

        try {
            var store = archive.openStore();
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
