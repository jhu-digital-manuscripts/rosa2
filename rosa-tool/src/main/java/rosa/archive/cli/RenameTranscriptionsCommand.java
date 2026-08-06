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
 * Renames AoR transcription XML files in a book directory according to the file map
 * ({@code filemap.csv}). Also updates the internal {@code page} element's
 * {@code filename} attribute to reference the new image filename.
 *
 * <p>When {@code --reverse} is false (the default), renames transcription files to match
 * new image names as defined in the file map. When {@code --reverse} is true, renames
 * them back to match old image names.</p>
 *
 * <p>Requires {@code filemap.csv} to exist in the book directory. Reports an error
 * and exits with code 1 if it is missing.</p>
 */
@Command(name = "rename-transcriptions",
         mixinStandardHelpOptions = true,
         description = "Rename AoR transcription XML files using the file map")
public final class RenameTranscriptionsCommand implements Callable<Integer> {

    @Option(names = "--archive", required = true, description = "Path to archive directory")
    private Path archivePath;

    @Option(names = "--collection", required = true, description = "Collection ID")
    private String collectionId;

    @Option(names = "--book", required = true, description = "Book ID")
    private String bookId;

    @Option(names = "--reverse", description = "Apply renaming in reverse (new-to-old)")
    private boolean reverse;

    /**
     * Executes the transcription rename operation.
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

            store.renameTranscriptions(collectionId, bookId, reverse, errors);

            errors.forEach(System.err::println);
            return errors.isEmpty() ? 0 : 1;
        } catch (IOException e) {
            System.err.println("Error renaming transcriptions: " + e.getMessage());
            return 1;
        }
    }
}
