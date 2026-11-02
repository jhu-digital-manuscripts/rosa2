package rosa.archive.cli;

import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * Validates AoR transcription XML files against the schema.
 *
 * <p>Iterates over all books in the specified collection, validating each
 * AoR transcription XML file for well-formedness. Reports errors and warnings
 * to standard output.</p>
 */
@Command(name = "validate-xml",
         mixinStandardHelpOptions = true, versionProvider = VersionProvider.class,
         description = "Check that AoR transcription XML files are well-formed, and valid against a local XML Schema when --schema is given")
public final class ValidateXmlCommand implements Callable<Integer> {

    @Mixin
    private ArchiveOptions archive;

    @Option(names = "--collection", required = true, description = "Collection ID to validate")
    private String collectionId;

    @Option(names = "--schema", description = "Local XML Schema (.xsd) file to validate against; the files' own schemaLocation is never fetched")
    private Path schema;

    /**
     * Executes the XML validation operation.
     *
     * @return 0 if no errors found, 1 on error
     */
    @Override
    public Integer call() {
        if (!archive.exists()) {
            return 1;
        }

        try {
            var store = archive.openStore();
            List<String> bookIds = store.listBooks(collectionId);

            boolean hasErrors = false;

            for (String bookId : bookIds) {
                List<String> errors = new ArrayList<>();
                List<String> warnings = new ArrayList<>();

                store.validateXml(collectionId, bookId, schema, errors, warnings);

                if (!errors.isEmpty() || !warnings.isEmpty()) {
                    System.out.println("[" + collectionId + "/" + bookId + "]");

                    for (String error : errors) {
                        System.out.println("  ERROR: " + error);
                    }
                    for (String warning : warnings) {
                        System.out.println("  WARN:  " + warning);
                    }
                }

                if (!errors.isEmpty()) {
                    hasErrors = true;
                }
            }

            return hasErrors ? 1 : 0;
        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
            return 1;
        }
    }
}
