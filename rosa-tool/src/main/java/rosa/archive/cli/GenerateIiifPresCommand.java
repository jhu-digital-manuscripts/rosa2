package rosa.archive.cli;

import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;
import rosa.archive.core.ArchiveStore;
import rosa.archive.iiif.IIIFJsonWriter;
import rosa.archive.iiif.IIIFPresentationGenerator;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * Generates static IIIF Presentation API 3.0 files from archive data.
 */
@Command(name = "generate-iiif-pres",
         mixinStandardHelpOptions = true, versionProvider = VersionProvider.class,
         description = "Generate static IIIF Presentation API 3.0 files from archive data")
public final class GenerateIiifPresCommand implements Callable<Integer> {

    @Mixin
    private ArchiveOptions archive;

    @Option(names = "--output", required = true, description = "Path to output directory")
    private Path outputPath;

    @Option(names = "--base-url", description = "Base URL prefix for resource IDs")
    private String baseUrl;

    @Option(names = "--image-base-url",
            description = "Base URL for IIIF Image API services (defaults to --base-url)")
    private String imageBaseUrl;

    @Option(names = "--image-api-version", defaultValue = "2",
            description = "IIIF Image API version: 2 or 3 (default: ${DEFAULT-VALUE})")
    private int imageApiVersion;

    @Option(names = "--opensearch-url", description = "Opensearch search endpoint URL for JHSearch service")
    private String opensearchUrl;

    @Option(names = "--annotation-stylesheet-url",
            description = "URL of the stylesheet referenced by commenting annotations (IIIF cookbook recipe 0045). "
                    + "{collection} is replaced by the top-level collection id, e.g. "
                    + "https://example.org/aor/styles/{collection}.css")
    private String annotationStylesheetUrl;

    @Option(names = "--viewer-url",
            description = "URL of the viewer site that the people, books and locations named in AoR "
                    + "annotations link to (advanced search). {collection} is replaced by the top-level "
                    + "collection id, e.g. https://example.org/{collection}/. Without it the names are plain text")
    private String viewerUrl;

    @Option(names = "--collection", paramLabel = "<id>",
            description = "Generate only this collection; repeat for several. Include a parent (dlmm) with "
                    + "its children (rose, pizan) to list them under it. Default: every collection")
    private List<String> collections;

    @Override
    public Integer call() {
        if (!archive.exists()) {
            return 1;
        }
        if (imageApiVersion != 2 && imageApiVersion != 3) {
            System.err.println("Error: --image-api-version must be 2 or 3, got " + imageApiVersion);
            return 1;
        }

        try {
            ArchiveStore store = archive.openStore();
            if (store.listCollections().isEmpty()) {
                System.err.println("Error: archive contains no readable collections: " + archive.path());
                return 1;
            }

            if (collections != null) {
                List<String> unknown = new ArrayList<>(collections);
                unknown.removeAll(store.listCollections());
                if (!unknown.isEmpty()) {
                    System.err.println("Error: unknown collection(s): " + String.join(", ", unknown)
                            + " (available: " + String.join(", ", store.listCollections()) + ")");
                    return 1;
                }
            }

            IIIFJsonWriter writer = new IIIFJsonWriter();
            IIIFPresentationGenerator generator = new IIIFPresentationGenerator(writer);
            generator.setAnnotationStylesheetUrl(annotationStylesheetUrl);
            generator.setViewerUrl(viewerUrl);
            generator.setCollections(collections);
            generator.generate(store, outputPath, baseUrl, imageBaseUrl, imageApiVersion, opensearchUrl);
            return 0;
        } catch (IOException e) {
            System.err.println("Error generating IIIF files: " + e.getMessage());
            return 1;
        }
    }
}
