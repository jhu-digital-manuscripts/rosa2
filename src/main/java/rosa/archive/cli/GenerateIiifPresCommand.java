package rosa.archive.cli;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import rosa.archive.core.ArchiveStore;
import rosa.archive.core.FileSystemArchiveStore;
import rosa.archive.iiif.IIIFJsonWriter;
import rosa.archive.iiif.IIIFPresentationGenerator;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Callable;

/**
 * Generates static IIIF Presentation API 3.0 files from archive data.
 */
@Command(name = "generate-iiif-pres",
         mixinStandardHelpOptions = true,
         description = "Generate static IIIF Presentation API 3.0 files from archive data")
public final class GenerateIiifPresCommand implements Callable<Integer> {

    @Option(names = "--archive", required = true, description = "Path to archive directory")
    private Path archivePath;

    @Option(names = "--output", required = true, description = "Path to output directory")
    private Path outputPath;

    @Option(names = "--base-url", description = "Base URL prefix for resource IDs")
    private String baseUrl;

    @Option(names = "--image-base-url",
            description = "Base URL for IIIF Image API services (defaults to --base-url)")
    private String imageBaseUrl;

    @Option(names = "--image-api-version", defaultValue = "2",
            description = "IIIF Image API version (2 or 3)")
    private int imageApiVersion;

    @Option(names = "--opensearch-url", description = "Opensearch search endpoint URL for JHSearch service")
    private String opensearchUrl;

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

            IIIFJsonWriter writer = new IIIFJsonWriter();
            IIIFPresentationGenerator generator = new IIIFPresentationGenerator(writer);
            generator.generate(store, outputPath, baseUrl, imageBaseUrl, imageApiVersion, opensearchUrl);
            return 0;
        } catch (IOException e) {
            System.err.println("Error generating IIIF files: " + e.getMessage());
            return 1;
        }
    }
}
