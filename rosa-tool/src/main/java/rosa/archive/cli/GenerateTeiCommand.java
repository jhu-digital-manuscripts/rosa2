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
 * Generates a single TEI P5 XML transcription from per-page text transcription files.
 *
 * <p>Delegates to {@link rosa.archive.core.ArchiveStore#generateTEITranscriptions}
 * which finds all per-page text transcription files ({@code BookId.transcription.NNNr.txt})
 * in the specified book, parses their custom text format, and combines them into a
 * single TEI P5 XML file ({@code BookId.transcription.xml}). Warnings for formatting
 * issues are printed to stdout; errors are printed to stderr.</p>
 */
@Command(name = "generate-tei",
         mixinStandardHelpOptions = true,
         description = "Generate TEI XML from per-page text transcription files")
public final class GenerateTeiCommand implements Callable<Integer> {

    @Option(names = "--archive", required = true, description = "Path to archive directory")
    private Path archivePath;

    @Option(names = "--collection", required = true, description = "Collection ID")
    private String collectionId;

    @Option(names = "--book", required = true, description = "Book ID")
    private String bookId;

    /**
     * Executes the TEI generation operation.
     *
     * @return 0 if no errors occurred, 1 on error
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
            List<String> warnings = new ArrayList<>();

            store.generateTEITranscriptions(collectionId, bookId, errors, warnings);

            warnings.forEach(w -> System.out.println("Warning: " + w));
            errors.forEach(System.err::println);
            return errors.isEmpty() ? 0 : 1;
        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
            return 1;
        }
    }
}
