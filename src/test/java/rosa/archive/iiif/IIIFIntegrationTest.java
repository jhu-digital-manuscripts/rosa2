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

        // Find illustration annotations (body contains "<p><strong>Illustration</strong></p>")
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
     * Finds annotation bodies that contain the illustration marker.
     */
    private List<String> findIllustrationAnnotationBodies(JsonNode items) {
        List<String> results = new ArrayList<>();
        for (JsonNode item : items) {
            JsonNode body = item.get("body");
            if (body != null) {
                String value = body.has("value") ? body.get("value").asText() : "";
                if (value.contains("<p><strong>Illustration</strong></p>")) {
                    results.add(value);
                }
            }
        }
        return results;
    }
}
