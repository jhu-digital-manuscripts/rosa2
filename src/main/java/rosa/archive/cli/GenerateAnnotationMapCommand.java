package rosa.archive.cli;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import rosa.archive.aor.AnnotationMapGenerator;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * Generates a mapping of annotation IDs to their location within a collection.
 *
 * <p>Produces {@code id_locations.csv} in the collection directory, containing entries
 * for each collection, book, page, and annotation. This mapping enables construction
 * of IIIF URIs from annotation identifiers.</p>
 */
@Command(name = "generate-annotation-map",
         mixinStandardHelpOptions = true,
         description = "Generate annotation ID to location mapping")
public final class GenerateAnnotationMapCommand implements Callable<Integer> {

    @Option(names = "--archive", required = true, description = "Path to archive directory")
    private Path archivePath;

    @Option(names = "--collection", required = true, description = "Collection ID")
    private String collectionId;

    /**
     * Executes the annotation map generation.
     *
     * @return 0 if no errors occurred, 1 on error
     */
    @Override
    public Integer call() {
        if (!Files.isDirectory(archivePath)) {
            System.err.println("Error: archive path does not exist or is not a directory: " + archivePath);
            return 1;
        }

        var generator = new AnnotationMapGenerator(archivePath);
        List<String> errors = generator.run(collectionId);

        errors.forEach(System.err::println);
        return errors.isEmpty() ? 0 : 1;
    }
}
