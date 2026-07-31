package rosa.archive.cli;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import rosa.archive.core.FileSystemArchiveStore;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.Callable;

/**
 * Interactively generates a file renaming map for a book.
 *
 * <p>Prompts the user for book structure details (covers, flyleaves, misc images,
 * new book ID) via stdin, then delegates to {@link rosa.archive.core.ArchiveStore#generateFileMap}
 * to produce a {@code filemap.csv} that maps existing image filenames to new
 * standardized filenames.</p>
 */
@Command(name = "file-map",
         mixinStandardHelpOptions = true,
         description = "Interactively generate a file renaming map for a book")
public final class FileMapCommand implements Callable<Integer> {

    @Option(names = "--archive", required = true, description = "Path to archive directory")
    private Path archivePath;

    @Option(names = "--collection", required = true, description = "Collection ID")
    private String collectionId;

    @Option(names = "--book", required = true, description = "Book ID")
    private String bookId;

    /**
     * Executes the interactive file map generation.
     *
     * <p>Reads book structure information from stdin, then generates the file map.
     * Errors are printed to stderr.</p>
     *
     * @return 0 if file map was generated without errors, 1 otherwise
     */
    @Override
    public Integer call() {
        if (!Files.isDirectory(archivePath)) {
            System.err.println("Error: archive path does not exist or is not a directory: " + archivePath);
            return 1;
        }

        var store = new FileSystemArchiveStore(archivePath);
        List<String> errors = new ArrayList<>();

        try (Scanner in = new Scanner(System.in)) {
            System.out.print("Has a front cover + front pastedown image? (true|false) ");
            boolean hasFrontCover = in.nextBoolean();

            System.out.print("Has a back cover and back pastedown image? (true|false) ");
            boolean hasBackCover = in.nextBoolean();

            System.out.print("Number of frontmatter flyleaves: ");
            int frontmatter = in.nextInt();

            System.out.print("Number of endmatter flyleaves: ");
            int endmatter = in.nextInt();

            System.out.print("Number of misc images: ");
            int misc = in.nextInt();

            System.out.print("Book ID: ");
            String newId = in.next();

            store.generateFileMap(collectionId, bookId, newId, hasFrontCover, hasBackCover,
                    frontmatter, endmatter, misc, errors);
        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
            return 1;
        }

        errors.forEach(System.err::println);
        return errors.isEmpty() ? 0 : 1;
    }
}
