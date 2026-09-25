package rosa.archive.aor;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Assume;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

/**
 * Property-based tests for AoR transcription checker.
 *
 * <p>Feature: rosa2-cli-refactor, Property 23: AoR Reference Validation Soundness</p>
 * <p>For any set of annotations containing people references, and any reference dataset,
 * every reference string that does NOT appear in the reference dataset SHALL be reported
 * as an error by the checker, and valid references produce no error.</p>
 *
 * <p>Feature: rosa2-cli-refactor, Property 24: Duplicate ID Detection</p>
 * <p>For any corpus of annotated pages where two or more pages contain annotations with
 * the same ID, the checker SHALL report that ID as a duplicate.</p>
 */
class AoRTranscriptionCheckerPropertyTest {

    // Feature: rosa2-cli-refactor, Property 23: AoR Reference Validation Soundness
    // Every reference not in reference dataset is reported as an error
    @Property(tries = 100)
    void everyInvalidPersonReferenceIsReportedAsError(
            @ForAll("validPersonNames") Set<String> validPeople,
            @ForAll("invalidPersonNames") Set<String> invalidPeople
    ) throws IOException {
        // Ensure no overlap between valid and invalid sets
        Set<String> effectiveInvalid = new HashSet<>(invalidPeople);
        effectiveInvalid.removeAll(validPeople);
        Assume.that(!effectiveInvalid.isEmpty());

        Path tempDir = Files.createTempDirectory("rosa2-prop-checkaor-");
        try {
            // Set up archive structure: archive/TestCol/TestBook/
            Path archiveDir = tempDir.resolve("archive");
            Path collectionDir = archiveDir.resolve("TestCol");
            Path bookDir = collectionDir.resolve("TestBook");
            Files.createDirectories(bookDir);

            // Create people.csv with valid references
            Path peopleCsv = collectionDir.resolve("people.csv");
            writePeopleCsv(peopleCsv, validPeople);

            // Create books.csv and locations.csv (empty but present to avoid load errors)
            Files.writeString(collectionDir.resolve("books.csv"), "", StandardCharsets.UTF_8);
            Files.writeString(collectionDir.resolve("locations.csv"), "", StandardCharsets.UTF_8);

            // Combine valid and invalid references for the transcription
            List<String> allReferences = new ArrayList<>();
            allReferences.addAll(validPeople);
            allReferences.addAll(effectiveInvalid);

            // Create an AoR transcription XML file with all person references
            String xmlContent = buildAorXml("TestBook.001r.tif", allReferences);
            Path xmlFile = bookDir.resolve("TestBook.aor.001r.xml");
            Files.writeString(xmlFile, xmlContent, StandardCharsets.UTF_8);

            // Run the checker
            var checker = new AoRTranscriptionChecker(archiveDir, null);
            List<String> errors = checker.run("TestCol", "TestBook");

            // Verify: every invalid person reference should produce an error
            for (String invalidPerson : effectiveInvalid) {
                boolean found = errors.stream().anyMatch(e ->
                        e.contains("Unknown person reference") && e.contains(invalidPerson));
                assertTrue(found,
                        "Expected error for invalid person reference '" + invalidPerson
                                + "' but errors were: " + errors);
            }

            // Verify: no error should mention any valid person reference
            for (String validPerson : validPeople) {
                boolean foundAsError = errors.stream().anyMatch(e ->
                        e.contains("Unknown person reference") && e.contains(validPerson));
                assertFalse(foundAsError,
                        "Valid person reference '" + validPerson
                                + "' should NOT produce an error but found in: " + errors);
            }

        } finally {
            deleteRecursively(tempDir);
        }
    }

    /**
     * Generates a set of valid person names (between 1 and 10 names).
     */
    @Provide
    Arbitrary<Set<String>> validPersonNames() {
        return personNameArbitrary()
                .set().ofMinSize(1).ofMaxSize(10);
    }

    /**
     * Generates a set of invalid person names (between 1 and 5 names)
     * that are distinct from valid names by using a different prefix.
     */
    @Provide
    Arbitrary<Set<String>> invalidPersonNames() {
        return Arbitraries.strings()
                .alpha()
                .ofMinLength(3)
                .ofMaxLength(12)
                .map(s -> "INVALID_" + s)
                .set().ofMinSize(1).ofMaxSize(5);
    }

    /**
     * Generates a plausible person name string.
     */
    private Arbitrary<String> personNameArbitrary() {
        return Arbitraries.strings()
                .alpha()
                .ofMinLength(3)
                .ofMaxLength(15)
                .map(s -> "Person_" + s);
    }

    /**
     * Writes a people.csv file with one entry per valid person.
     * Format: one person name per line (first column is the key).
     */
    private void writePeopleCsv(Path path, Set<String> validPeople) throws IOException {
        StringBuilder sb = new StringBuilder();
        for (String person : validPeople) {
            sb.append(person).append("\n");
        }
        Files.writeString(path, sb.toString(), StandardCharsets.UTF_8);
    }

    /**
     * Builds a minimal AoR transcription XML containing marginalia with
     * person references.
     */
    private String buildAorXml(String pageFilename, List<String> personReferences) {
        StringBuilder persons = new StringBuilder();
        for (String person : personReferences) {
            persons.append("              <person name=\"")
                    .append(escapeXml(person))
                    .append("\"/>\n");
        }

        return """
                <?xml version="1.0" encoding="UTF-8"?>
                <transcription xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
                  <page filename="%s" pagination="1r" reader="Harvey"/>
                  <annotation>
                    <marginalia hand="Italian" id="marg_001" place="head">
                      <language ident="en">
                        <position place="head" book_orientation="0">
                %s
                        </position>
                      </language>
                    </marginalia>
                  </annotation>
                </transcription>
                """.formatted(pageFilename, persons.toString().stripTrailing());
    }

    /**
     * Escapes special XML characters in attribute values.
     */
    private String escapeXml(String value) {
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }

    // Feature: rosa2-cli-refactor, Property 24: Duplicate ID Detection
    // Annotations with same ID across pages are reported as duplicates
    @Property(tries = 100)
    void duplicateAnnotationIdAcrossPagesIsReportedAsDuplicate(
            @ForAll("annotationIds") String duplicateId
    ) throws IOException {
        Path tempDir = Files.createTempDirectory("rosa2-prop-dupid-");
        try {
            // Set up archive structure: archive/TestCol/TestBook/
            Path archiveDir = tempDir.resolve("archive");
            Path collectionDir = archiveDir.resolve("TestCol");
            Path bookDir = collectionDir.resolve("TestBook");
            Files.createDirectories(bookDir);

            // Create empty reference CSVs to avoid load errors
            Files.writeString(collectionDir.resolve("people.csv"), "", StandardCharsets.UTF_8);
            Files.writeString(collectionDir.resolve("books.csv"), "", StandardCharsets.UTF_8);
            Files.writeString(collectionDir.resolve("locations.csv"), "", StandardCharsets.UTF_8);

            // Create TWO separate AoR transcription XML files, each containing
            // a marginalia element with the SAME ID
            String xml1 = buildAorXmlWithId("TestBook.001r.tif", "1r", duplicateId);
            Path xmlFile1 = bookDir.resolve("TestBook.aor.001r.xml");
            Files.writeString(xmlFile1, xml1, StandardCharsets.UTF_8);

            String xml2 = buildAorXmlWithId("TestBook.002r.tif", "2r", duplicateId);
            Path xmlFile2 = bookDir.resolve("TestBook.aor.002r.xml");
            Files.writeString(xmlFile2, xml2, StandardCharsets.UTF_8);

            // Run the checker
            var checker = new AoRTranscriptionChecker(archiveDir, null);
            List<String> errors = checker.run("TestCol", "TestBook");

            // Verify: at least one error mentions duplicate annotation ID
            boolean hasDuplicateError = errors.stream().anyMatch(e ->
                    e.contains("Duplicate annotation ID") && e.contains(duplicateId));
            assertTrue(hasDuplicateError,
                    "Expected a 'Duplicate annotation ID' error for ID '" + duplicateId
                            + "' but errors were: " + errors);

        } finally {
            deleteRecursively(tempDir);
        }
    }

    /**
     * Generates plausible annotation ID strings.
     * IDs must be non-blank and contain only alphanumeric characters and underscores.
     */
    @Provide
    Arbitrary<String> annotationIds() {
        return Arbitraries.strings()
                .withCharRange('a', 'z')
                .withCharRange('A', 'Z')
                .withCharRange('0', '9')
                .withChars('_')
                .ofMinLength(3)
                .ofMaxLength(20)
                .filter(s -> !s.isBlank());
    }

    /**
     * Builds a minimal AoR transcription XML containing a single marginalia
     * with the given annotation ID.
     */
    private String buildAorXmlWithId(String pageFilename, String pagination, String annotationId) {
        return """
                <?xml version="1.0" encoding="UTF-8"?>
                <transcription xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
                  <page filename="%s" pagination="%s" reader="Harvey"/>
                  <annotation>
                    <marginalia hand="Italian" id="%s">
                      <language ident="en">
                        <position place="head" book_orientation="0">
                          <marginalia_text>Some text</marginalia_text>
                        </position>
                      </language>
                    </marginalia>
                  </annotation>
                </transcription>
                """.formatted(pageFilename, pagination, escapeXml(annotationId));
    }

    /**
     * Recursively deletes a directory tree.
     */
    private void deleteRecursively(Path dir) throws IOException {
        if (!Files.exists(dir)) return;
        try (var walk = Files.walk(dir)) {
            walk.sorted(Comparator.reverseOrder())
                    .forEach(p -> {
                        try {
                            Files.deleteIfExists(p);
                        } catch (IOException e) {
                            // best effort cleanup
                        }
                    });
        }
    }
}
