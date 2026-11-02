package rosa.archive.cli;

import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Files;
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
         mixinStandardHelpOptions = true, versionProvider = VersionProvider.class,
         description = "Interactively generate a file renaming map for a book")
public final class FileMapCommand implements Callable<Integer> {

    @Mixin
    private ArchiveOptions archive;

    @Option(names = "--collection", required = true, description = "Collection ID")
    private String collectionId;

    @Option(names = "--book", required = true, description = "Book ID")
    private String bookId;

    @Option(names = "--force", description = "Replace an existing filemap.csv")
    private boolean force;

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
        if (!archive.exists()) {
            return 1;
        }

        var store = archive.openStore();
        Path fileMap = archive.path().resolve(collectionId).resolve(bookId).resolve("filemap.csv");
        if (Files.exists(fileMap) && !force) {
            System.err.println("Error: " + fileMap + " already exists (use --force to replace it)");
            return 1;
        }
        List<String> errors = new ArrayList<>();
        try (Scanner in = new Scanner(System.in)) {
            boolean hasFrontCover = askBoolean(in, "Has a front cover + front pastedown image? (true|false) ");
            boolean hasBackCover = askBoolean(in, "Has a back cover and back pastedown image? (true|false) ");
            int frontmatter = askInt(in, "Number of frontmatter flyleaves: ");
            int endmatter = askInt(in, "Number of endmatter flyleaves: ");
            int misc = askInt(in, "Number of misc images: ");
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

    private static boolean askBoolean(Scanner in, String prompt) {
        while (true) {
            System.out.print(prompt);
            if (in.hasNextBoolean()) {
                return in.nextBoolean();
            }
            if (!in.hasNext()) {
                throw new java.util.NoSuchElementException("no answer");
            }
            System.out.println("Please answer true or false, not '" + in.next() + "'.");
        }
    }

    private static int askInt(Scanner in, String prompt) {
        while (true) {
            System.out.print(prompt);
            if (in.hasNextInt()) {
                int value = in.nextInt();
                if (value >= 0) {
                    return value;
                }
                System.out.println("Please enter a number of 0 or more.");
                continue;
            }
            if (!in.hasNext()) {
                throw new java.util.NoSuchElementException("no answer");
            }
            System.out.println("Please enter a whole number, not '" + in.next() + "'.");
        }
    }
}
