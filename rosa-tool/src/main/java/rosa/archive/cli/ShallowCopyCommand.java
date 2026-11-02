package rosa.archive.cli;

import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.Callable;

/**
 * Copies only metadata files from the archive, excluding images.
 *
 * <p>Copies all XML, TXT, HTML, and CSV files while preserving the archive
 * directory hierarchy. Image files (TIF, TIFF, JPG, JPEG, PNG) are excluded.</p>
 */
@Command(name = "shallow-copy",
         mixinStandardHelpOptions = true, versionProvider = VersionProvider.class,
         description = "Copy the archive without its images: XML, TXT, HTML, CSV, JSON-LD, properties and SHA1SUM files")
public final class ShallowCopyCommand implements Callable<Integer> {

    @Mixin
    private ArchiveOptions archive;

    @Option(names = "--output", required = true, description = "Path to output directory")
    private Path outputPath;

    /**
     * Executes the shallow copy operation.
     *
     * @return 0 on success, 1 on error
     */
    @Override
    public Integer call() {
        if (!archive.exists()) {
            return 1;
        }

        try {
            var store = archive.openStore();
            store.shallowCopy(outputPath);
            return 0;
        } catch (IOException e) {
            System.err.println("Error during shallow copy: " + e.getMessage());
            return 1;
        }
    }
}
