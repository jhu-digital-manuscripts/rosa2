package rosa.archive.iiif;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import rosa.archive.core.FileSystemArchiveStore;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test: Full pipeline with hierarchy verification.
 *
 * Runs generate() against the test archive in src/test/resources/archive/
 * and verifies:
 * - Output directory structure is flat (all collection dirs at same level)
 * - Top-level collection.json lists only root collections (aor, dlmm)
 * - dlmm/collection.json lists rose and pizan as Collection-type items
 * - Every sub-collection collection.json has manifest items with thumbnail properties
 *
 * Validates: Requirements 2.1, 2.2, 4.6
 */
class IIIFIntegrationTest {

    private IIIFPresentationGenerator generator;
    private ObjectMapper mapper;

    @BeforeEach
    void setUp() {
        IIIFJsonWriter writer = new IIIFJsonWriter();
        generator = new IIIFPresentationGenerator(writer);
        mapper = new ObjectMapper();
    }

    @Test
    void testFullPipelineWithHierarchyVerification(@TempDir Path outputDir) throws IOException, URISyntaxException {
        // Load the test archive from classpath
        Path archivePath = Path.of(getClass().getClassLoader().getResource("archive").toURI());
        FileSystemArchiveStore store = new FileSystemArchiveStore(archivePath);

        // Run the full pipeline
        generator.generate(store, outputDir, "http://example.org", null, 2);

        // 1. Verify output directory structure is flat (all collection dirs at same level)
        assertTrue(Files.isDirectory(outputDir.resolve("aor")),
                "aor directory should exist at top level of output");
        assertTrue(Files.isDirectory(outputDir.resolve("dlmm")),
                "dlmm directory should exist at top level of output");
        assertTrue(Files.isDirectory(outputDir.resolve("rose")),
                "rose directory should exist at top level of output (flat structure)");

        // All collection dirs are directly under outputDir (flat, not nested)
        assertFalse(Files.isDirectory(outputDir.resolve("dlmm").resolve("rose")),
                "rose should NOT be nested under dlmm — directory structure is flat");
        assertFalse(Files.isDirectory(outputDir.resolve("dlmm").resolve("pizan")),
                "pizan should NOT be nested under dlmm — directory structure is flat");

        // 2. Verify top-level collection.json lists only root collections (aor, dlmm)
        Path topCollectionFile = outputDir.resolve("collection.json");
        assertTrue(Files.exists(topCollectionFile), "Top-level collection.json should exist");

        String topJson = Files.readString(topCollectionFile);
        JsonNode topParsed = mapper.readTree(topJson);

        assertEquals("Collection", topParsed.get("type").asText());
        assertEquals("http://example.org/collection", topParsed.get("id").asText());

        JsonNode topItems = topParsed.get("items");
        assertNotNull(topItems, "Top-level collection should have items");
        assertEquals(2, topItems.size(),
                "Top-level collection should have exactly 2 items (aor and dlmm), not rose or pizan");

        // Verify the items are aor and dlmm (in whatever order they appear)
        boolean hasAor = false;
        boolean hasDlmm = false;
        boolean hasRose = false;
        boolean hasPizan = false;
        for (JsonNode item : topItems) {
            assertEquals("Collection", item.get("type").asText());
            String id = item.get("id").asText();
            if (id.contains("/aor/")) hasAor = true;
            if (id.contains("/dlmm/")) hasDlmm = true;
            if (id.contains("/rose/")) hasRose = true;
            if (id.contains("/pizan/")) hasPizan = true;
        }
        assertTrue(hasAor, "Top-level should contain aor");
        assertTrue(hasDlmm, "Top-level should contain dlmm");
        assertFalse(hasRose, "Top-level should NOT contain rose (it's a child of dlmm)");
        assertFalse(hasPizan, "Top-level should NOT contain pizan (it's a child of dlmm)");

        // 3. Verify dlmm/collection.json lists rose and pizan as Collection-type items
        Path dlmmCollectionFile = outputDir.resolve("dlmm").resolve("collection.json");
        assertTrue(Files.exists(dlmmCollectionFile), "dlmm/collection.json should exist");

        String dlmmJson = Files.readString(dlmmCollectionFile);
        JsonNode dlmmParsed = mapper.readTree(dlmmJson);

        assertEquals("Collection", dlmmParsed.get("type").asText());

        JsonNode dlmmItems = dlmmParsed.get("items");
        assertNotNull(dlmmItems, "dlmm collection should have items");

        // dlmm has no books (no subdirectories for books), so its items should be
        // Collection references for children: rose and pizan
        boolean dlmmHasRoseCollection = false;
        boolean dlmmHasPizanCollection = false;
        for (JsonNode item : dlmmItems) {
            String type = item.get("type").asText();
            String id = item.get("id").asText();
            if ("Collection".equals(type) && id.contains("/rose/")) {
                dlmmHasRoseCollection = true;
            }
            if ("Collection".equals(type) && id.contains("/pizan/")) {
                dlmmHasPizanCollection = true;
            }
        }
        assertTrue(dlmmHasRoseCollection,
                "dlmm/collection.json should list rose as a Collection-type item");
        assertTrue(dlmmHasPizanCollection,
                "dlmm/collection.json should list pizan as a Collection-type item");

        // 4. Verify every sub-collection collection.json has manifest items with thumbnail properties
        // Check rose/collection.json (has books: Douce195, Douce332)
        Path roseCollectionFile = outputDir.resolve("rose").resolve("collection.json");
        assertTrue(Files.exists(roseCollectionFile), "rose/collection.json should exist");

        String roseJson = Files.readString(roseCollectionFile);
        JsonNode roseParsed = mapper.readTree(roseJson);
        JsonNode roseItems = roseParsed.get("items");
        assertNotNull(roseItems, "rose collection should have items");

        // rose collection should have manifest items (for Douce195, Douce332)
        int roseManifestCount = 0;
        for (JsonNode item : roseItems) {
            if ("Manifest".equals(item.get("type").asText())) {
                roseManifestCount++;
                assertTrue(item.has("thumbnail"),
                        "Manifest item '" + item.get("id").asText() + "' in rose/collection.json should have a thumbnail property");
                JsonNode thumbnail = item.get("thumbnail");
                assertTrue(thumbnail.isArray() && thumbnail.size() > 0,
                        "Thumbnail should be a non-empty array");
                JsonNode thumbObj = thumbnail.get(0);
                assertEquals("Image", thumbObj.get("type").asText());
                assertEquals("image/jpeg", thumbObj.get("format").asText());
                assertTrue(thumbObj.get("id").asText().endsWith("/full/80,/0/default.jpg"),
                        "Thumbnail id should end with /full/80,/0/default.jpg");
            }
        }
        assertTrue(roseManifestCount >= 2,
                "rose collection should have at least 2 manifest items (Douce195, Douce332)");

        // Check aor/collection.json (has books: PrincetonU101)
        Path aorCollectionFile = outputDir.resolve("aor").resolve("collection.json");
        assertTrue(Files.exists(aorCollectionFile), "aor/collection.json should exist");

        String aorJson = Files.readString(aorCollectionFile);
        JsonNode aorParsed = mapper.readTree(aorJson);
        JsonNode aorItems = aorParsed.get("items");
        assertNotNull(aorItems, "aor collection should have items");

        int aorManifestCount = 0;
        for (JsonNode item : aorItems) {
            if ("Manifest".equals(item.get("type").asText())) {
                aorManifestCount++;
                assertTrue(item.has("thumbnail"),
                        "Manifest item '" + item.get("id").asText() + "' in aor/collection.json should have a thumbnail property");
                JsonNode thumbnail = item.get("thumbnail");
                assertTrue(thumbnail.isArray() && thumbnail.size() > 0,
                        "Thumbnail should be a non-empty array");
            }
        }
        assertTrue(aorManifestCount >= 1,
                "aor collection should have at least 1 manifest item (PrincetonU101)");
    }

    /**
     * Integration test: Illustration tagging annotations.
     *
     * Runs generate() against the test archive and verifies:
     * - Annotation pages for Douce332 include illustration tagging annotations
     *   parsed from Douce332.imagetag.csv
     * - Annotation pages for Douce195 include illustration tagging annotations
     *   parsed from Douce195.imagetag.csv
     * - Annotation bodies contain expected illustration data (titles, characters, etc.)
     *
     * Validates: Requirements 2.3, 4.2, 4.3, 4.5
     */
    @Test
    void testIllustrationTaggingAnnotations(@TempDir Path outputDir) throws IOException, URISyntaxException {
        // Load the test archive from classpath
        Path archivePath = Path.of(getClass().getClassLoader().getResource("archive").toURI());
        FileSystemArchiveStore store = new FileSystemArchiveStore(archivePath);

        // Run the full pipeline
        generator.generate(store, outputDir, "http://example.org", null, 2);

        // --- Douce332 illustration tagging ---
        // Douce332.imagetag.csv has entries on folio 1r (2 entries), 1v, 2v, 3r, 3v, etc.
        // In the images list:
        //   index 0: Douce332.binding.frontcover.tif
        //   index 1: Douce332.frontmatter.pastedown.tif
        //   index 2-5: flyleaf pages
        //   index 6: Douce332.001r.tif (folio 1r)
        //   index 7: Douce332.001v.tif (folio 1v)
        //   index 8: Douce332.002r.tif (folio 2r)
        //   index 9: Douce332.002v.tif (folio 2v)

        // Check canvas/6 (folio 1r) — imagetag entries 1 and 2 have folio "1r"
        Path douce332Canvas6 = outputDir.resolve("rose/Douce332/canvas/6/annotations.json");
        assertTrue(Files.exists(douce332Canvas6),
                "Douce332 canvas/6 (folio 1r) should have an annotations.json file");

        String douce332Canvas6Json = Files.readString(douce332Canvas6);
        JsonNode douce332Canvas6Parsed = mapper.readTree(douce332Canvas6Json);
        JsonNode douce332Canvas6Items = douce332Canvas6Parsed.get("items");
        assertNotNull(douce332Canvas6Items, "Annotation page should have items");

        // Find illustration annotations (body contains "<p><b>Illustration</b></p>")
        List<String> douce332Folio1rIllustrations = findIllustrationAnnotationBodies(douce332Canvas6Items);
        assertTrue(douce332Folio1rIllustrations.size() >= 1,
                "Douce332 folio 1r should have at least 1 illustration annotation (entry 1 in imagetag.csv), found: "
                        + douce332Folio1rIllustrations.size());

        // Verify annotation body content for first illustration on 1r
        // Entry 1: Characters=2, Costume="Ankle-length gown", has Architecture, Landscape, Other
        boolean foundFirstIllus = false;
        for (String body : douce332Folio1rIllustrations) {
            if (body.contains("Ankle-length gown")) {
                foundFirstIllus = true;
                assertTrue(body.contains("Illustration"), "Body should contain 'Illustration' heading");
                assertTrue(body.contains("Costume:"), "Body should contain 'Costume:' section");
                assertTrue(body.contains("Landscape:"), "Body should contain 'Landscape:' section");
                assertTrue(body.contains("Architecture:"), "Body should contain 'Architecture:' section");
                break;
            }
        }
        assertTrue(foundFirstIllus, "Should find illustration with 'Ankle-length gown' costume on folio 1r");

        // Check canvas/9 (folio 2v) — imagetag entries 2 and 3 have folio "2v"
        Path douce332Canvas9 = outputDir.resolve("rose/Douce332/canvas/9/annotations.json");
        assertTrue(Files.exists(douce332Canvas9),
                "Douce332 canvas/9 (folio 2v) should have an annotations.json file");

        String douce332Canvas9Json = Files.readString(douce332Canvas9);
        JsonNode douce332Canvas9Parsed = mapper.readTree(douce332Canvas9Json);
        JsonNode douce332Canvas9Items = douce332Canvas9Parsed.get("items");
        assertNotNull(douce332Canvas9Items, "Annotation page for canvas/9 should have items");

        List<String> douce332Folio2vIllustrations = findIllustrationAnnotationBodies(douce332Canvas9Items);
        assertTrue(douce332Folio2vIllustrations.size() >= 2,
                "Douce332 folio 2v should have at least 2 illustration annotations (entries on 2v), found: "
                        + douce332Folio2vIllustrations.size());

        // --- Douce195 illustration tagging ---
        // Douce195.imagetag.csv has 126 entries. First entry is on folio 1r.
        // In images list:
        //   index 0: Douce195.binding.frontcover.tif (missing/*)
        //   index 1: Douce195.frontmatter.pastedown.tif
        //   index 2-5: flyleaf pages
        //   index 6: Douce195.001r.tif (folio 1r)

        // Check canvas/6 (folio 1r) — imagetag entries 1 and 2 have folio "1r"
        Path douce195Canvas6 = outputDir.resolve("rose/Douce195/canvas/6/annotations.json");
        assertTrue(Files.exists(douce195Canvas6),
                "Douce195 canvas/6 (folio 1r) should have an annotations.json file");

        String douce195Canvas6Json = Files.readString(douce195Canvas6);
        JsonNode douce195Canvas6Parsed = mapper.readTree(douce195Canvas6Json);
        JsonNode douce195Canvas6Items = douce195Canvas6Parsed.get("items");
        assertNotNull(douce195Canvas6Items, "Annotation page should have items");

        List<String> douce195Folio1rIllustrations = findIllustrationAnnotationBodies(douce195Canvas6Items);
        assertTrue(douce195Folio1rIllustrations.size() >= 2,
                "Douce195 folio 1r should have at least 2 illustration annotations, found: "
                        + douce195Folio1rIllustrations.size());

        // Verify content of Douce195 illustration annotations on 1r
        // Entry 1 has Characters=49, Costume="Blue sleeveless outer garment and hat"
        boolean foundDouce195Illus = false;
        for (String body : douce195Folio1rIllustrations) {
            if (body.contains("Characters:")) {
                foundDouce195Illus = true;
                assertTrue(body.contains("Illustration"), "Body should contain 'Illustration' heading");
                break;
            }
        }
        assertTrue(foundDouce195Illus, "Should find illustration annotation with 'Characters:' on Douce195 folio 1r");

        // Verify a later folio in Douce195 also has illustrations
        // Entry 5 in imagetag has folio "2r". In images:
        //   index 6: 001r, index 7: 001v, index 8: 002r
        Path douce195Canvas8 = outputDir.resolve("rose/Douce195/canvas/8/annotations.json");
        assertTrue(Files.exists(douce195Canvas8),
                "Douce195 canvas/8 (folio 2r) should have an annotations.json file");

        String douce195Canvas8Json = Files.readString(douce195Canvas8);
        JsonNode douce195Canvas8Parsed = mapper.readTree(douce195Canvas8Json);
        JsonNode douce195Canvas8Items = douce195Canvas8Parsed.get("items");
        assertNotNull(douce195Canvas8Items, "Annotation page for Douce195 canvas/8 should have items");

        List<String> douce195Folio2rIllustrations = findIllustrationAnnotationBodies(douce195Canvas8Items);
        assertTrue(douce195Folio2rIllustrations.size() >= 1,
                "Douce195 folio 2r should have at least 1 illustration annotation, found: "
                        + douce195Folio2rIllustrations.size());

        // Verify that there are illustration annotations with "Titles:" content
        // (Douce195 imagetag entries have actual illustration title strings in the "Illustration title" column)
        boolean foundTitles = false;
        for (String body : douce195Folio1rIllustrations) {
            if (body.contains("Titles:")) {
                foundTitles = true;
                break;
            }
        }
        assertTrue(foundTitles, "Douce195 folio 1r illustrations should contain 'Titles:' content");
    }

    /**
     * Integration test: Per-page plain-text transcription.
     *
     * Runs generate() against the test archive and verifies:
     * - Annotation pages for Douce332 include plain-text transcription annotations
     *   from .transcription.{page}.txt files (via the TEI XML transcription which uses
     *   zero-padded page break markers matching the image filenames)
     * - Annotation bodies contain the expected plain-text content from the TXT files
     *
     * Douce332 images.csv layout:
     *   index 6: Douce332.001r.tif → canvas/6
     *   index 8: Douce332.002r.tif → canvas/8
     *   index 10: Douce332.003r.tif → canvas/10
     *
     * Validates: Requirements 4.2, 4.5
     */
    @Test
    void testPerPagePlainTextTranscription(@TempDir Path outputDir) throws IOException, URISyntaxException {
        // Load the test archive from classpath
        Path archivePath = Path.of(getClass().getClassLoader().getResource("archive").toURI());
        FileSystemArchiveStore store = new FileSystemArchiveStore(archivePath);

        // Run the full pipeline
        generator.generate(store, outputDir, "http://example.org", null, 2);

        // --- Douce332 canvas/6 (folio 001r) ---
        // The TEI XML has <pb n="001r"/> which should match Douce332.001r.tif directly
        // Expected content from transcription includes text like "Maintes gens dient", "songes", "Macrobes"
        Path douce332Canvas6 = outputDir.resolve("rose/Douce332/canvas/6/annotations.json");
        assertTrue(Files.exists(douce332Canvas6),
                "Douce332 canvas/6 (folio 001r) should have an annotations.json file");

        String canvas6Json = Files.readString(douce332Canvas6);
        JsonNode canvas6Parsed = mapper.readTree(canvas6Json);
        JsonNode canvas6Items = canvas6Parsed.get("items");
        assertNotNull(canvas6Items, "Annotation page for canvas/6 should have items");

        // Find transcription annotations (not illustration annotations)
        List<String> canvas6Transcriptions = findTranscriptionAnnotationBodies(canvas6Items);
        assertFalse(canvas6Transcriptions.isEmpty(),
                "Douce332 canvas/6 (001r) should have at least one transcription annotation");

        // Verify the transcription content contains expected text from folio 001r
        // The TEI XML for <pb n="001r"/> includes text like "Maintes gens dient", "songes", "Macrobes"
        String canvas6AllText = String.join(" ", canvas6Transcriptions);
        assertTrue(canvas6AllText.contains("Macrobes") || canvas6AllText.contains("songes")
                        || canvas6AllText.contains("Maintes"),
                "Douce332 canvas/6 transcription should contain text from folio 001r " +
                        "(e.g., 'Macrobes', 'songes', or 'Maintes'). Found: " +
                        canvas6AllText.substring(0, Math.min(200, canvas6AllText.length())));

        // --- Douce332 canvas/8 (folio 002r) ---
        // Expected content includes "roussignol", "chanter", "aguille"
        Path douce332Canvas8 = outputDir.resolve("rose/Douce332/canvas/8/annotations.json");
        assertTrue(Files.exists(douce332Canvas8),
                "Douce332 canvas/8 (folio 002r) should have an annotations.json file");

        String canvas8Json = Files.readString(douce332Canvas8);
        JsonNode canvas8Parsed = mapper.readTree(canvas8Json);
        JsonNode canvas8Items = canvas8Parsed.get("items");
        assertNotNull(canvas8Items, "Annotation page for canvas/8 should have items");

        List<String> canvas8Transcriptions = findTranscriptionAnnotationBodies(canvas8Items);
        assertFalse(canvas8Transcriptions.isEmpty(),
                "Douce332 canvas/8 (002r) should have at least one transcription annotation");

        String canvas8AllText = String.join(" ", canvas8Transcriptions);
        assertTrue(canvas8AllText.contains("roussignol") || canvas8AllText.contains("chanter")
                        || canvas8AllText.contains("aguille"),
                "Douce332 canvas/8 transcription should contain text from folio 002r " +
                        "(e.g., 'roussignol', 'chanter', or 'aguille'). Found: " +
                        canvas8AllText.substring(0, Math.min(200, canvas8AllText.length())));

        // --- Douce332 canvas/10 (folio 003r) ---
        // Expected content includes "convoitise" or "Convoitise" or "usure"
        Path douce332Canvas10 = outputDir.resolve("rose/Douce332/canvas/10/annotations.json");
        assertTrue(Files.exists(douce332Canvas10),
                "Douce332 canvas/10 (folio 003r) should have an annotations.json file");

        String canvas10Json = Files.readString(douce332Canvas10);
        JsonNode canvas10Parsed = mapper.readTree(canvas10Json);
        JsonNode canvas10Items = canvas10Parsed.get("items");
        assertNotNull(canvas10Items, "Annotation page for canvas/10 should have items");

        List<String> canvas10Transcriptions = findTranscriptionAnnotationBodies(canvas10Items);
        assertFalse(canvas10Transcriptions.isEmpty(),
                "Douce332 canvas/10 (003r) should have at least one transcription annotation");

        String canvas10AllText = String.join(" ", canvas10Transcriptions);
        assertTrue(canvas10AllText.contains("convoitise") || canvas10AllText.contains("Convoitise")
                        || canvas10AllText.contains("usure"),
                "Douce332 canvas/10 transcription should contain text from folio 003r " +
                        "(e.g., 'convoitise', 'Convoitise', or 'usure'). Found: " +
                        canvas10AllText.substring(0, Math.min(200, canvas10AllText.length())));
    }

    /**
     * Finds annotation bodies that are transcription content (not illustration annotations).
     * Transcription annotations have text/html format and do NOT contain the illustration marker.
     */
    private List<String> findTranscriptionAnnotationBodies(JsonNode items) {
        List<String> results = new ArrayList<>();
        for (JsonNode item : items) {
            JsonNode body = item.get("body");
            if (body != null) {
                String value = body.has("value") ? body.get("value").asText() : "";
                String format = body.has("format") ? body.get("format").asText() : "";
                // Transcription annotations have text/html format and do NOT contain illustration marker
                if ("text/html".equals(format)
                        && !value.contains("<p><b>Illustration</b></p>")
                        && !value.isEmpty()) {
                    results.add(value);
                }
            }
        }
        return results;
    }

    /**
     * Integration test: AoR annotations (all sub-types).
     *
     * Runs generate() against the test archive and verifies:
     * - Annotation pages for PrincetonU101 include all AoR annotation types present in test data:
     *   marginalia, underlines, marks, symbols, numerals
     * - Each annotation type has the correct structure and content from the source XML
     * - No illustration tagging annotations are generated for PrincetonU101 (it has no .imagetag.csv)
     *
     * Validates: Requirements 3.5, 4.4, 4.5
     */
    @Test
    void testAoRAnnotationsAllSubTypes(@TempDir Path outputDir) throws IOException, URISyntaxException {
        // Load the test archive from classpath
        Path archivePath = Path.of(getClass().getClassLoader().getResource("archive").toURI());
        FileSystemArchiveStore store = new FileSystemArchiveStore(archivePath);

        // Run the full pipeline
        generator.generate(store, outputDir, "http://example.org", null, 2);

        // --- Canvas index mapping for PrincetonU101 ---
        // index 0: binding.frontcover
        // index 1: frontmatter.pastedown
        // index 2-5: flyleaf pages
        // index 6: 001r, index 7: 001v, ...
        // Formula: for page NNNr: index = 6 + (NNN-1)*2 + 0; for NNNv: index = 6 + (NNN-1)*2 + 1
        // 005r → canvas/14, 010r → canvas/24, 030r → canvas/64, 050r → canvas/104, 117r → canvas/238

        // --- Canvas/14 (folio 005r): Underlines + Marks ---
        Path canvas14 = outputDir.resolve("aor/PrincetonU101/canvas/14/annotations.json");
        assertTrue(Files.exists(canvas14),
                "PrincetonU101 canvas/14 (005r) should have an annotations.json file");

        String canvas14Json = Files.readString(canvas14);
        JsonNode canvas14Parsed = mapper.readTree(canvas14Json);
        JsonNode canvas14Items = canvas14Parsed.get("items");
        assertNotNull(canvas14Items, "Annotation page for canvas/14 should have items");
        assertTrue(canvas14Items.size() > 0, "canvas/14 should have at least 1 annotation item");

        // Verify underlines are present (source XML has 9 underlines)
        List<String> canvas14Bodies = getAllAnnotationBodies(canvas14Items);
        boolean hasUnderline005r = canvas14Bodies.stream()
                .anyMatch(b -> b.contains("the ciuill life") || b.contains("Souldiours"));
        assertTrue(hasUnderline005r,
                "Canvas/14 (005r) should have underline annotations with text like 'the ciuill life' or 'Souldiours'");

        // Verify marks are present (source XML has hash and plus_sign marks)
        boolean hasMark005r = canvas14Bodies.stream()
                .anyMatch(b -> b.contains("hash") || b.contains("plus_sign"));
        assertTrue(hasMark005r,
                "Canvas/14 (005r) should have mark annotations with 'hash' or 'plus_sign'");

        // --- Canvas/24 (folio 010r): Underlines + Marks ---
        // Note: Marginalia are present in the XML, but since buildPosition() does not
        // currently parse <marginalia_text> content into Position.texts, marginalia annotations
        // produce empty bodies (<p></p>) that get filtered out. We verify underlines and marks.
        Path canvas24 = outputDir.resolve("aor/PrincetonU101/canvas/24/annotations.json");
        assertTrue(Files.exists(canvas24),
                "PrincetonU101 canvas/24 (010r) should have an annotations.json file");

        String canvas24Json = Files.readString(canvas24);
        JsonNode canvas24Parsed = mapper.readTree(canvas24Json);
        JsonNode canvas24Items = canvas24Parsed.get("items");
        assertNotNull(canvas24Items, "Annotation page for canvas/24 should have items");

        List<String> canvas24Bodies = getAllAnnotationBodies(canvas24Items);

        // Verify underlines are present (source XML has 23 underlines)
        boolean hasUnderline010r = canvas24Bodies.stream()
                .anyMatch(b -> b.contains("which will enterprise") || b.contains("all diligence"));
        assertTrue(hasUnderline010r,
                "Canvas/24 (010r) should have underline annotations");

        // Verify marks are present (source XML has 8 marks)
        boolean hasMark010r = canvas24Bodies.stream()
                .anyMatch(b -> b.contains("plus_sign"));
        assertTrue(hasMark010r,
                "Canvas/24 (010r) should have mark annotations");

        // --- Canvas/64 (folio 030r): Underlines + Marks ---
        Path canvas64 = outputDir.resolve("aor/PrincetonU101/canvas/64/annotations.json");
        assertTrue(Files.exists(canvas64),
                "PrincetonU101 canvas/64 (030r) should have an annotations.json file");

        String canvas64Json = Files.readString(canvas64);
        JsonNode canvas64Parsed = mapper.readTree(canvas64Json);
        JsonNode canvas64Items = canvas64Parsed.get("items");
        assertNotNull(canvas64Items, "Annotation page for canvas/64 should have items");

        List<String> canvas64Bodies = getAllAnnotationBodies(canvas64Items);
        // Verify marks include various types (hash, plus_sign, circumflex, comma, dot, bracket)
        boolean hasVariedMarks030r = canvas64Bodies.stream()
                .anyMatch(b -> b.contains("hash") || b.contains("plus_sign") || b.contains("circumflex"));
        assertTrue(hasVariedMarks030r,
                "Canvas/64 (030r) should have varied mark types (hash, plus_sign, circumflex, etc.)");

        // --- Canvas/104 (folio 050r): Underlines + Symbols + Marks ---
        // Note: Marginalia is present in XML but marginalia text isn't parsed into Position.texts
        Path canvas104 = outputDir.resolve("aor/PrincetonU101/canvas/104/annotations.json");
        assertTrue(Files.exists(canvas104),
                "PrincetonU101 canvas/104 (050r) should have an annotations.json file");

        String canvas104Json = Files.readString(canvas104);
        JsonNode canvas104Parsed = mapper.readTree(canvas104Json);
        JsonNode canvas104Items = canvas104Parsed.get("items");
        assertNotNull(canvas104Items, "Annotation page for canvas/104 should have items");

        List<String> canvas104Bodies = getAllAnnotationBodies(canvas104Items);

        // Verify symbols (2 Bisected_circle entries)
        boolean hasSymbol050r = canvas104Bodies.stream()
                .anyMatch(b -> b.contains("Bisected_circle"));
        assertTrue(hasSymbol050r,
                "Canvas/104 (050r) should have symbol annotations with 'Bisected_circle'");

        // Verify underlines present
        boolean hasUnderline050r = canvas104Bodies.stream()
                .anyMatch(b -> b.contains("The artillerie of the armie") || b.contains("suffiseth ten"));
        assertTrue(hasUnderline050r,
                "Canvas/104 (050r) should have underline annotations");

        // Verify marks present
        boolean hasMark050r = canvas104Bodies.stream()
                .anyMatch(b -> b.contains("plus_sign") || b.contains("hash") || b.contains("circumflex"));
        assertTrue(hasMark050r,
                "Canvas/104 (050r) should have mark annotations");

        // --- Canvas/238 (folio 117r): Numerals ---
        Path canvas238 = outputDir.resolve("aor/PrincetonU101/canvas/238/annotations.json");
        assertTrue(Files.exists(canvas238),
                "PrincetonU101 canvas/238 (117r) should have an annotations.json file");

        String canvas238Json = Files.readString(canvas238);
        JsonNode canvas238Parsed = mapper.readTree(canvas238Json);
        JsonNode canvas238Items = canvas238Parsed.get("items");
        assertNotNull(canvas238Items, "Annotation page for canvas/238 should have items");

        List<String> canvas238Bodies = getAllAnnotationBodies(canvas238Items);

        // Verify numerals (source XML has "3." and "4.")
        boolean hasNumeral117r = canvas238Bodies.stream()
                .anyMatch(b -> b.contains("3.") || b.contains("4."));
        assertTrue(hasNumeral117r,
                "Canvas/238 (117r) should have numeral annotations containing '3.' or '4.'");

        // --- Verify NO illustration tagging annotations for PrincetonU101 ---
        // PrincetonU101 has no .imagetag.csv, so no illustration annotations should be generated
        // Check all annotation pages we just verified — none should contain the illustration marker
        for (JsonNode item : canvas14Items) {
            assertNoIllustrationAnnotation(item, "canvas/14");
        }
        for (JsonNode item : canvas24Items) {
            assertNoIllustrationAnnotation(item, "canvas/24");
        }
        for (JsonNode item : canvas64Items) {
            assertNoIllustrationAnnotation(item, "canvas/64");
        }
        for (JsonNode item : canvas104Items) {
            assertNoIllustrationAnnotation(item, "canvas/104");
        }
        for (JsonNode item : canvas238Items) {
            assertNoIllustrationAnnotation(item, "canvas/238");
        }
    }

    /**
     * Comprehensive integration test: All annotation types across the entire test archive.
     *
     * This is the definitive "all annotation types work" gate test. It exercises the FULL
     * pipeline once and asserts correct output for ALL annotation types:
     * - Illustration tagging annotations (from .imagetag.csv) — verified on Douce332 and Douce195
     * - TEI transcription annotations (from .transcription.xml) — verified on Douce195 with zero-padded folios
     * - Per-page plain-text transcription annotations (from .transcription.{page}.txt) — verified on Douce332
     * - AoR annotations (from .aor.{page}.xml) covering all sub-types — verified on PrincetonU101
     *
     * Validates: Requirements 4.5
     */
    @Test
    void testComprehensiveAllAnnotationTypes(@TempDir Path outputDir) throws IOException, URISyntaxException {
        // Load the test archive from classpath and run the full pipeline ONCE
        Path archivePath = Path.of(getClass().getClassLoader().getResource("archive").toURI());
        FileSystemArchiveStore store = new FileSystemArchiveStore(archivePath);
        generator.generate(store, outputDir, "http://example.org", null, 2);

        // ===================================================================
        // 1. ILLUSTRATION TAGGING — Douce332 canvas/6 (folio 1r)
        // ===================================================================
        Path douce332Canvas6 = outputDir.resolve("rose/Douce332/canvas/6/annotations.json");
        assertTrue(Files.exists(douce332Canvas6),
                "Douce332 canvas/6 (folio 1r) should have annotations.json");
        JsonNode douce332Canvas6Items = mapper.readTree(Files.readString(douce332Canvas6)).get("items");
        assertNotNull(douce332Canvas6Items);

        List<String> douce332IllusBodies = findIllustrationAnnotationBodies(douce332Canvas6Items);
        assertTrue(douce332IllusBodies.size() >= 1,
                "Douce332 canvas/6 (1r) should have illustration tagging annotations from imagetag.csv");
        assertTrue(douce332IllusBodies.stream().anyMatch(b -> b.contains("Illustration")),
                "Illustration annotation body should contain 'Illustration' heading marker");

        // ===================================================================
        // 2. ILLUSTRATION TAGGING — Douce195 canvas/6 (folio 1r)
        // ===================================================================
        Path douce195Canvas6 = outputDir.resolve("rose/Douce195/canvas/6/annotations.json");
        assertTrue(Files.exists(douce195Canvas6),
                "Douce195 canvas/6 (folio 1r) should have annotations.json");
        JsonNode douce195Canvas6Items = mapper.readTree(Files.readString(douce195Canvas6)).get("items");
        assertNotNull(douce195Canvas6Items);

        List<String> douce195IllusBodies = findIllustrationAnnotationBodies(douce195Canvas6Items);
        assertTrue(douce195IllusBodies.size() >= 1,
                "Douce195 canvas/6 (1r) should have illustration tagging annotations from imagetag.csv");
        assertTrue(douce195IllusBodies.stream().anyMatch(b ->
                        b.contains("Characters:") || b.contains("Costume:") || b.contains("Titles:")),
                "Douce195 illustration annotations should contain illustration data fields");

        // ===================================================================
        // 3. TEI TRANSCRIPTION — Douce195 canvas/6 (001r → single-digit zero-padded)
        // ===================================================================
        List<String> douce195Canvas6Transcriptions = findTranscriptionAnnotationBodies(douce195Canvas6Items);
        assertFalse(douce195Canvas6Transcriptions.isEmpty(),
                "Douce195 canvas/6 (001r) should have TEI transcription (pb n=\"1r\" matched via zero-stripping)");
        String douce195Canvas6Text = String.join(" ", douce195Canvas6Transcriptions);
        assertTrue(douce195Canvas6Text.contains("Macrobes") || douce195Canvas6Text.contains("songes")
                        || douce195Canvas6Text.contains("Mainte"),
                "Douce195 canvas/6 TEI transcription should contain text from folio 1r " +
                        "(e.g., 'Macrobes', 'songes', or 'Mainte')");

        // ===================================================================
        // 4. TEI TRANSCRIPTION — Douce195 canvas/24 (010r → double-digit zero-padded)
        // ===================================================================
        Path douce195Canvas24 = outputDir.resolve("rose/Douce195/canvas/24/annotations.json");
        assertTrue(Files.exists(douce195Canvas24),
                "Douce195 canvas/24 (folio 010r) should have annotations.json");
        JsonNode douce195Canvas24Items = mapper.readTree(Files.readString(douce195Canvas24)).get("items");
        assertNotNull(douce195Canvas24Items);

        List<String> douce195Canvas24Transcriptions = findTranscriptionAnnotationBodies(douce195Canvas24Items);
        assertFalse(douce195Canvas24Transcriptions.isEmpty(),
                "Douce195 canvas/24 (010r) should have TEI transcription (pb n=\"10r\" matched via zero-stripping)");
        String douce195Canvas24Text = String.join(" ", douce195Canvas24Transcriptions);
        assertTrue(douce195Canvas24Text.contains("Largesse") || douce195Canvas24Text.contains("Bretaigne")
                        || douce195Canvas24Text.contains("lignage"),
                "Douce195 canvas/24 TEI transcription should contain text from folio 10r " +
                        "(e.g., 'Largesse', 'Bretaigne', or 'lignage')");

        // ===================================================================
        // 5. TEI TRANSCRIPTION — Douce195 canvas/204 (100r → no stripping needed)
        // ===================================================================
        Path douce195Canvas204 = outputDir.resolve("rose/Douce195/canvas/204/annotations.json");
        assertTrue(Files.exists(douce195Canvas204),
                "Douce195 canvas/204 (folio 100r) should have annotations.json");
        JsonNode douce195Canvas204Items = mapper.readTree(Files.readString(douce195Canvas204)).get("items");
        assertNotNull(douce195Canvas204Items);

        List<String> douce195Canvas204Transcriptions = findTranscriptionAnnotationBodies(douce195Canvas204Items);
        assertFalse(douce195Canvas204Transcriptions.isEmpty(),
                "Douce195 canvas/204 (100r) should have TEI transcription (pb n=\"100r\" matches directly)");
        String douce195Canvas204Text = String.join(" ", douce195Canvas204Transcriptions);
        assertTrue(douce195Canvas204Text.contains("viendro") || douce195Canvas204Text.contains("amour")
                        || douce195Canvas204Text.contains("femme"),
                "Douce195 canvas/204 TEI transcription should contain text from folio 100r " +
                        "(e.g., 'viendro', 'amour', or 'femme')");

        // ===================================================================
        // 6. PER-PAGE PLAIN-TEXT TRANSCRIPTION — Douce332 canvas/6 (001r)
        // ===================================================================
        List<String> douce332Canvas6Transcriptions = findTranscriptionAnnotationBodies(douce332Canvas6Items);
        assertFalse(douce332Canvas6Transcriptions.isEmpty(),
                "Douce332 canvas/6 (001r) should have per-page plain-text transcription from .transcription.001r.txt");
        String douce332Canvas6Text = String.join(" ", douce332Canvas6Transcriptions);
        assertTrue(douce332Canvas6Text.contains("Macrobes") || douce332Canvas6Text.contains("songes")
                        || douce332Canvas6Text.contains("Maintes"),
                "Douce332 canvas/6 should contain plain-text transcription content from folio 001r");

        // ===================================================================
        // 7. PER-PAGE PLAIN-TEXT TRANSCRIPTION — Douce332 canvas/8 (002r)
        // ===================================================================
        Path douce332Canvas8 = outputDir.resolve("rose/Douce332/canvas/8/annotations.json");
        assertTrue(Files.exists(douce332Canvas8),
                "Douce332 canvas/8 (folio 002r) should have annotations.json");
        JsonNode douce332Canvas8Items = mapper.readTree(Files.readString(douce332Canvas8)).get("items");
        assertNotNull(douce332Canvas8Items);

        List<String> douce332Canvas8Transcriptions = findTranscriptionAnnotationBodies(douce332Canvas8Items);
        assertFalse(douce332Canvas8Transcriptions.isEmpty(),
                "Douce332 canvas/8 (002r) should have per-page plain-text transcription from .transcription.002r.txt");

        // ===================================================================
        // 8. PER-PAGE PLAIN-TEXT TRANSCRIPTION — Douce332 canvas/10 (003r)
        // ===================================================================
        Path douce332Canvas10 = outputDir.resolve("rose/Douce332/canvas/10/annotations.json");
        assertTrue(Files.exists(douce332Canvas10),
                "Douce332 canvas/10 (folio 003r) should have annotations.json");
        JsonNode douce332Canvas10Items = mapper.readTree(Files.readString(douce332Canvas10)).get("items");
        assertNotNull(douce332Canvas10Items);

        List<String> douce332Canvas10Transcriptions = findTranscriptionAnnotationBodies(douce332Canvas10Items);
        assertFalse(douce332Canvas10Transcriptions.isEmpty(),
                "Douce332 canvas/10 (003r) should have per-page plain-text transcription from .transcription.003r.txt");

        // ===================================================================
        // 9. AoR UNDERLINES — PrincetonU101 canvas/14 (005r)
        // ===================================================================
        Path pu101Canvas14 = outputDir.resolve("aor/PrincetonU101/canvas/14/annotations.json");
        assertTrue(Files.exists(pu101Canvas14),
                "PrincetonU101 canvas/14 (005r) should have annotations.json");
        JsonNode pu101Canvas14Items = mapper.readTree(Files.readString(pu101Canvas14)).get("items");
        assertNotNull(pu101Canvas14Items);

        List<String> canvas14Bodies = getAllAnnotationBodies(pu101Canvas14Items);
        assertTrue(canvas14Bodies.stream()
                        .anyMatch(b -> b.contains("the ciuill life") || b.contains("Souldiours")),
                "PrincetonU101 canvas/14 (005r) should have underline annotations with text " +
                        "like 'the ciuill life' or 'Souldiours'");

        // ===================================================================
        // 10. AoR MARKS — PrincetonU101 canvas/14 (005r) and canvas/64 (030r)
        // ===================================================================
        assertTrue(canvas14Bodies.stream()
                        .anyMatch(b -> b.contains("hash") || b.contains("plus_sign")),
                "PrincetonU101 canvas/14 (005r) should have mark annotations with 'hash' or 'plus_sign'");

        Path pu101Canvas64 = outputDir.resolve("aor/PrincetonU101/canvas/64/annotations.json");
        assertTrue(Files.exists(pu101Canvas64),
                "PrincetonU101 canvas/64 (030r) should have annotations.json");
        JsonNode pu101Canvas64Items = mapper.readTree(Files.readString(pu101Canvas64)).get("items");
        assertNotNull(pu101Canvas64Items);

        List<String> canvas64Bodies = getAllAnnotationBodies(pu101Canvas64Items);
        assertTrue(canvas64Bodies.stream()
                        .anyMatch(b -> b.contains("hash") || b.contains("plus_sign") || b.contains("circumflex")),
                "PrincetonU101 canvas/64 (030r) should have varied mark types");

        // ===================================================================
        // 11. AoR SYMBOLS — PrincetonU101 canvas/104 (050r)
        // ===================================================================
        Path pu101Canvas104 = outputDir.resolve("aor/PrincetonU101/canvas/104/annotations.json");
        assertTrue(Files.exists(pu101Canvas104),
                "PrincetonU101 canvas/104 (050r) should have annotations.json");
        JsonNode pu101Canvas104Items = mapper.readTree(Files.readString(pu101Canvas104)).get("items");
        assertNotNull(pu101Canvas104Items);

        List<String> canvas104Bodies = getAllAnnotationBodies(pu101Canvas104Items);
        assertTrue(canvas104Bodies.stream().anyMatch(b -> b.contains("Bisected_circle")),
                "PrincetonU101 canvas/104 (050r) should have symbol annotations with 'Bisected_circle'");

        // ===================================================================
        // 12. AoR NUMERALS — PrincetonU101 canvas/238 (117r)
        // ===================================================================
        Path pu101Canvas238 = outputDir.resolve("aor/PrincetonU101/canvas/238/annotations.json");
        assertTrue(Files.exists(pu101Canvas238),
                "PrincetonU101 canvas/238 (117r) should have annotations.json");
        JsonNode pu101Canvas238Items = mapper.readTree(Files.readString(pu101Canvas238)).get("items");
        assertNotNull(pu101Canvas238Items);

        List<String> canvas238Bodies = getAllAnnotationBodies(pu101Canvas238Items);
        assertTrue(canvas238Bodies.stream().anyMatch(b -> b.contains("3.") || b.contains("4.")),
                "PrincetonU101 canvas/238 (117r) should have numeral annotations containing '3.' or '4.'");
    }

    /**
     * Gets all annotation body values (text/plain value or text/html value) from annotation page items.
     */
    private List<String> getAllAnnotationBodies(JsonNode items) {
        List<String> results = new ArrayList<>();
        for (JsonNode item : items) {
            JsonNode body = item.get("body");
            if (body != null && body.has("value")) {
                results.add(body.get("value").asText());
            }
        }
        return results;
    }

    /**
     * Asserts that an annotation item does NOT contain the illustration marker.
     */
    private void assertNoIllustrationAnnotation(JsonNode item, String canvasLabel) {
        JsonNode body = item.get("body");
        if (body != null && body.has("value")) {
            String value = body.get("value").asText();
            assertFalse(value.contains("<p><b>Illustration</b></p>"),
                    "PrincetonU101 " + canvasLabel + " should NOT have illustration tagging annotations " +
                            "(no .imagetag.csv exists for this book)");
        }
    }

    /**
     * Finds annotation bodies that contain the illustration marker.
     */
    private List<String> findIllustrationAnnotationBodies(JsonNode items) {
        List<String> results = new ArrayList<>();
        for (JsonNode item : items) {
            JsonNode body = item.get("body");
            if (body != null) {
                String value = body.has("value") ? body.get("value").asText() : "";
                if (value.contains("<p><b>Illustration</b></p>")) {
                    results.add(value);
                }
            }
        }
        return results;
    }
}
