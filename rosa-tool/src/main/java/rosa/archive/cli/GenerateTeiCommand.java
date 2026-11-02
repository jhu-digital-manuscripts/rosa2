package rosa.archive.cli;

import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

import java.io.IOException;
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
         mixinStandardHelpOptions = true, versionProvider = VersionProvider.class,
         description = "Generate TEI XML from per-page text transcription files (refuses to replace an existing transcription.xml without --force)")
public final class GenerateTeiCommand implements Callable<Integer> {

    @Mixin
    private ArchiveOptions archive;

    @Option(names = "--collection", required = true, description = "Collection ID")
    private String collectionId;

    @Option(names = "--book", required = true, description = "Book ID")
    private String bookId;

    @Option(names = "--force", description = "Replace an existing transcription.xml")
    private boolean force;

    /**
     * Executes the TEI generation operation.
     *
     * @return 0 if no errors occurred, 1 on error
     */
    @Override
    public Integer call() {
        if (!archive.exists()) {
            return 1;
        }

        try {
            var store = archive.openStore();
            List<String> errors = new ArrayList<>();
            List<String> warnings = new ArrayList<>();

            store.generateTEITranscriptions(collectionId, bookId, force, errors, warnings);

            warnings.forEach(w -> System.out.println("Warning: " + w));
            errors.forEach(System.err::println);
            return errors.isEmpty() ? 0 : 1;
        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
            return 1;
        }
    }
}
