package rosa.archive.cli;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import rosa.archive.core.ArchiveStore;
import rosa.archive.core.FileSystemArchiveStore;
import rosa.archive.opensearch.OpensearchIngestGenerator;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Callable;

/**
 * Generates Opensearch bulk ingest files from archive data.
 */
@Command(name = "generate-opensearch-ingest",
         mixinStandardHelpOptions = true,
         description = "Generate Opensearch bulk ingest files from archive data")
public final class GenerateOpensearchIngestCommand implements Callable<Integer> {

    @Option(names = "--archive", required = true, description = "Path to archive directory")
    private Path archivePath;

    @Option(names = "--output", required = true, description = "Path to output directory")
    private Path outputPath;

    @Override
    public Integer call() {
        if (!Files.isDirectory(archivePath)) {
            System.err.println("Error: archive path does not exist or is not a directory: " + archivePath);
            return 1;
        }

        try {
            ArchiveStore store = new FileSystemArchiveStore(archivePath);
            if (store.listCollections().isEmpty()) {
                System.err.println("Error: archive contains no readable collections: " + archivePath);
                return 1;
            }

            OpensearchIngestGenerator generator = new OpensearchIngestGenerator();
            generator.generate(store, outputPath);
            return 0;
        } catch (IOException e) {
            System.err.println("Error generating Opensearch ingest files: " + e.getMessage());
            return 1;
        }
    }
}
