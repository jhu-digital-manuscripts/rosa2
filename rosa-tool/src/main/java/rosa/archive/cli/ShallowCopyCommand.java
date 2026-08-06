package rosa.archive.cli;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import rosa.archive.core.FileSystemArchiveStore;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Callable;

/**
 * Copies only metadata files from the archive, excluding images.
 *
 * <p>Copies all XML, TXT, HTML, and CSV files while preserving the archive
 * directory hierarchy. Image files (TIF, TIFF, JPG, JPEG, PNG) are excluded.</p>
 */
@Command(name = "shallow-copy",
         mixinStandardHelpOptions = true,
         description = "Copy metadata files from archive, excluding images")
public final class ShallowCopyCommand implements Callable<Integer> {

    @Option(names = "--archive", required = true, description = "Path to archive directory")
    private Path archivePath;

    @Option(names = "--output", required = true, description = "Path to output directory")
    private Path outputPath;

    /**
     * Executes the shallow copy operation.
     *
     * @return 0 on success, 1 on error
     */
    @Override
    public Integer call() {
        if (!Files.isDirectory(archivePath)) {
            System.err.println("Error: archive path does not exist or is not a directory: " + archivePath);
            return 1;
        }

        try {
            var store = new FileSystemArchiveStore(archivePath);
            store.shallowCopy(outputPath);
            return 0;
        } catch (IOException e) {
            System.err.println("Error during shallow copy: " + e.getMessage());
            return 1;
        }
    }
}
