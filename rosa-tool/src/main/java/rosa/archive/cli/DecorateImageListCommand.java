package rosa.archive.cli;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import rosa.archive.core.ImageListDecorator;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * Reads page labels from AoR transcription files and writes them into the
 * book's image list CSV.
 *
 * <p>For each book, the command extracts pagination and signature attributes
 * from AoR transcription XML files and updates the image list with page labels.
 * Pagination is preferred over signature when both are present. Images without
 * a corresponding transcription file are marked as auto-generated.</p>
 */
@Command(name = "decorate-image-list",
         mixinStandardHelpOptions = true,
         description = "Copy page labels from AoR transcriptions to image list CSV")
public final class DecorateImageListCommand implements Callable<Integer> {

    @Option(names = "--archive", required = true, description = "Path to archive directory")
    private Path archivePath;

    @Option(names = "--collection", required = true, description = "Collection ID")
    private String collectionId;

    @Option(names = "--book", description = "Book ID (if omitted, processes all books in collection)")
    private String bookId;

    /**
     * Executes the image list decoration.
     *
     * @return 0 if no errors occurred, 1 on error
     */
    @Override
    public Integer call() {
        if (!Files.isDirectory(archivePath)) {
            System.err.println("Error: archive path does not exist or is not a directory: " + archivePath);
            return 1;
        }

        var decorator = new ImageListDecorator(archivePath);
        List<String> errors = new ArrayList<>();

        if (bookId != null) {
            decorator.decorateBook(collectionId, bookId, errors);
        } else {
            decorator.decorateCollection(collectionId, errors);
        }

        errors.forEach(System.err::println);
        return errors.isEmpty() ? 0 : 1;
    }
}
