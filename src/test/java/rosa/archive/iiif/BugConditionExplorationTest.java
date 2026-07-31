package rosa.archive.iiif;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import rosa.archive.core.FileSystemArchiveStore;
import rosa.archive.model.Book;
import rosa.archive.model.BookCollection;
import rosa.archive.model.CollectionMetadata;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Bug Condition Exploration Tests.
 *
 * These tests exercise the four known bugs in the IIIF generation code.
 * They are EXPECTED TO FAIL on unfixed code — failure confirms the bugs exist.
 *
 * <p><b>Validates: Requirements 1.1, 1.2, 1.3, 1.4</b>
 */
class BugConditionExplorationTest {

    private IIIFPresentationGenerator generator;
    private IIIFJsonWriter writer;
    private ObjectMapper mapper;

    @BeforeEach
    void setUp() {
        writer = new IIIFJsonWriter();
        generator = new IIIFPresentationGenerator(writer);
        mapper = writer.getObjectMapper();
    }

    /**
     * Test 1a - Hierarchy Bug.
     *
     * Call generateTopCollection() with collections where dlmm has children=rose,pizan.
     * Assert that the top-level collection.json only includes root collections (aor, dlmm)
     * as items and does NOT include child collections (rose, pizan) as independent top-level items.
     *
     * Also assert that dlmm/collection.json lists rose and pizan as Collection-type items.
     *
     * Expected failure on unfixed code: all collections appear flat in JSON.
     *
     * <p><b>Validates: Requirements 1.1</b>
     */
    @Test
    void testHierarchy_topCollectionShouldOnlyIncludeRootCollections() throws IOException {
        // Setup: 4 collections where dlmm has children rose and pizan
        // rose and pizan are child collections (they have parents)
        List<String> allCollectionIds = List.of("aor", "dlmm", "rose", "pizan");

        // Build the set of child collection IDs (those with non-empty parents)
        // In this scenario, rose and pizan are children of dlmm
        java.util.Set<String> childCollectionIds = java.util.Set.of("rose", "pizan");

        ObjectNode topCollection = generator.generateTopCollection(allCollectionIds, "http://example.org", childCollectionIds);
        String json = writer.writeToString(topCollection);
        JsonNode parsed = mapper.readTree(json);

        JsonNode items = parsed.get("items");
        assertNotNull(items, "Top collection should have items");

        // Extract the collection IDs from items
        List<String> topLevelIds = new java.util.ArrayList<>();
        for (JsonNode item : items) {
            String id = item.get("id").asText();
            // Extract collection name from URL like "http://example.org/aor/collection"
            String collectionName = id.replace("http://example.org/", "").replace("/collection", "");
            topLevelIds.add(collectionName);
        }

        // ASSERTION: Only root collections should appear at top level
        // Root collections are: aor (standalone), dlmm (parent with no parents itself)
        // Child collections rose and pizan should NOT appear as top-level items
        assertFalse(topLevelIds.contains("rose"),
                "Child collection 'rose' should NOT appear as a top-level item");
        assertFalse(topLevelIds.contains("pizan"),
                "Child collection 'pizan' should NOT appear as a top-level item");
        assertTrue(topLevelIds.contains("aor"),
                "Standalone collection 'aor' should appear as a top-level item");
        assertTrue(topLevelIds.contains("dlmm"),
                "Parent collection 'dlmm' should appear as a top-level item");
        assertEquals(2, items.size(),
                "Top collection should have exactly 2 items (aor and dlmm), not 4");
    }

    /**
     * Test 1a continued - Verify parent sub-collection lists children.
     *
     * Assert that dlmm/collection.json lists rose and pizan as Collection-type items.
     *
     * Expected failure: generateSubCollection() only produces Manifest items, not Collection items.
     */
    @Test
    void testHierarchy_parentSubCollectionShouldListChildrenAsCollectionItems() throws IOException {
        // Create dlmm collection with children=rose,pizan
        BookCollection dlmm = new BookCollection();
        dlmm.setId("dlmm");
        CollectionMetadata dlmmMeta = new CollectionMetadata();
        dlmmMeta.setLabel("Digital Library of Medieval Manuscripts");
        dlmmMeta.setChildren(new String[]{"rose", "pizan"});
        dlmm.setMetadata(dlmmMeta);

        // Book labels for manifests in dlmm (may have some direct books too)
        Map<String, String> bookLabels = new LinkedHashMap<>();
        bookLabels.put("MS100", "MS100");

        ObjectNode subCollection = generator.generateSubCollection(dlmm, bookLabels, "http://example.org");
        String json = writer.writeToString(subCollection);
        JsonNode parsed = mapper.readTree(json);

        JsonNode items = parsed.get("items");
        assertNotNull(items, "Sub-collection should have items");

        // Look for Collection-type items referencing rose and pizan
        boolean hasRoseCollection = false;
        boolean hasPizanCollection = false;
        for (JsonNode item : items) {
            String type = item.get("type").asText();
            String id = item.get("id").asText();
            if ("Collection".equals(type) && id.contains("rose")) {
                hasRoseCollection = true;
            }
            if ("Collection".equals(type) && id.contains("pizan")) {
                hasPizanCollection = true;
            }
        }

        assertTrue(hasRoseCollection,
                "dlmm sub-collection should list 'rose' as a Collection-type item");
        assertTrue(hasPizanCollection,
                "dlmm sub-collection should list 'pizan' as a Collection-type item");
    }

    /**
     * Test 1b - Thumbnails Bug.
     *
     * Call generateSubCollection() with book labels and assert each manifest item
     * has a thumbnail property with {imageServiceId}/full/80,/0/default.jpg.
     *
     * Expected failure on unfixed code: thumbnail property is absent.
     *
     * <p><b>Validates: Requirements 1.2</b>
     */
    @Test
    void testThumbnails_subCollectionManifestItemsShouldHaveThumbnails() throws IOException {
        BookCollection collection = new BookCollection();
        collection.setId("rose");
        CollectionMetadata metadata = new CollectionMetadata();
        metadata.setLabel("Roman de la Rose");
        collection.setMetadata(metadata);

        Map<String, String> bookLabels = new LinkedHashMap<>();
        bookLabels.put("LudwigXV7", "Ludwig XV 7");
        bookLabels.put("Walters143", "Walters 143");

        Map<String, String> bookFirstImages = new LinkedHashMap<>();
        bookFirstImages.put("LudwigXV7", "LudwigXV7.001r.tif");
        bookFirstImages.put("Walters143", "Walters143.001r.tif");

        ObjectNode subCollection = generator.generateSubCollection(collection, bookLabels, bookFirstImages, "http://example.org", null);
        String json = writer.writeToString(subCollection);
        JsonNode parsed = mapper.readTree(json);

        JsonNode items = parsed.get("items");
        assertNotNull(items, "Sub-collection should have items");
        assertTrue(items.size() > 0, "Sub-collection should have at least one item");

        // ASSERTION: Each manifest item should have a thumbnail property
        for (int i = 0; i < items.size(); i++) {
            JsonNode item = items.get(i);
            assertTrue(item.has("thumbnail"),
                    "Manifest item at index " + i + " should have a 'thumbnail' property");

            JsonNode thumbnail = item.get("thumbnail");
            assertTrue(thumbnail.isArray() && thumbnail.size() > 0,
                    "Thumbnail should be a non-empty array");

            String thumbnailId = thumbnail.get(0).get("id").asText();
            assertTrue(thumbnailId.contains("/full/80,/0/default.jpg"),
                    "Thumbnail id should contain '/full/80,/0/default.jpg', got: " + thumbnailId);
        }
    }

    /**
     * Test 1c - IllustrationTagging Bug.
     *
     * Call loadBook() for a book with a .imagetag.csv file on disk.
     * Assert book.getIllustrationTagging() is non-null.
     *
     * Expected failure on unfixed code: returns null because loadBook() never reads
     * the .imagetag.csv file.
     *
     * <p><b>Validates: Requirements 1.3</b>
     */
    @Test
    void testIllustrationTagging_loadBookShouldPopulateIllustrationTagging(@TempDir Path tempDir) throws IOException {
        // Create a minimal archive structure with a .imagetag.csv file
        Path archiveDir = tempDir.resolve("archive");
        Path collectionDir = archiveDir.resolve("rose");
        Path bookDir = collectionDir.resolve("Douce195");
        Files.createDirectories(bookDir);

        // Create minimal image list
        Files.writeString(bookDir.resolve("Douce195.images.csv"),
                "Douce195.001r.tif,3000,4000,false\n" +
                "Douce195.001v.tif,3000,4000,false\n");

        // Create illustration tagging CSV file
        String imagetagCsv = "id,Folio #,Illustration title,Textual elements,Initials,Characters,Costume,Objects,Landscape,Architecture,Other\n" +
                "1,1r,Garden of Delight,\"Rose, Garden\",A,Lover,Medieval,Mirror,Garden,Castle,\n" +
                "2,1v,The Dreamer,Bed,B,Dreamer,Night,Bed,None,Chamber,Dream\n";
        Files.writeString(bookDir.resolve("Douce195.imagetag.csv"), imagetagCsv);

        // Load the book
        FileSystemArchiveStore store = new FileSystemArchiveStore(archiveDir);
        BookCollection collection = store.loadCollection("rose");
        Book book = store.loadBook(collection, "Douce195");

        // ASSERTION: IllustrationTagging should be non-null when CSV exists
        assertNotNull(book.getIllustrationTagging(),
                "book.getIllustrationTagging() should be non-null when .imagetag.csv exists on disk");
    }

    /**
     * Test 1d - TEI Page Match Bug.
     *
     * Call extractPageTranscription() with XML containing {@code <pb n="1r"/>}
     * and imageId Book.001r.tif.
     * Assert result is non-null transcription content.
     *
     * Expected failure on unfixed code: returns null because '001r' does not match '1r'.
     *
     * <p><b>Validates: Requirements 1.4</b>
     */
    @Test
    void testTeiPageMatch_zeroPaddedImageShouldMatchUnpaddedPbMarker() throws Exception {
        // TEI XML with unpadded page break marker <pb n="1r"/>
        String teiXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <TEI xmlns="http://www.tei-c.org/ns/1.0">
                  <text>
                    <body>
                      <pb n="1r"/>
                      <p>This is the transcription content for folio 1r.</p>
                      <pb n="1v"/>
                      <p>This is folio 1v content.</p>
                      <pb n="2r"/>
                      <p>This is folio 2r content.</p>
                    </body>
                  </text>
                </TEI>
                """;

        // Image ID uses zero-padded folio: Book.001r.tif
        String imageId = "Book.001r.tif";

        // Use reflection to access the private extractPageTranscription method
        Method method = IIIFPresentationGenerator.class.getDeclaredMethod(
                "extractPageTranscription", String.class, String.class);
        method.setAccessible(true);

        String result = (String) method.invoke(generator, teiXml, imageId);

        // ASSERTION: Should find transcription for folio 1r despite zero-padding mismatch
        assertNotNull(result,
                "extractPageTranscription() should return non-null for image 'Book.001r.tif' " +
                "matching TEI <pb n=\"1r\"/>. Got null because '001r' does not match '1r'.");
        assertFalse(result.isEmpty(),
                "Transcription content should not be empty");
        assertTrue(result.contains("transcription content for folio 1r"),
                "Should contain the actual transcription text for folio 1r");
    }

    /**
     * Additional TEI page match test for double-digit zero-padded folios.
     * Image Douce195.010v.tif should match <pb n="10v"/>.
     */
    @Test
    void testTeiPageMatch_doubleDigitZeroPaddedShouldMatchUnpadded() throws Exception {
        String teiXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <TEI xmlns="http://www.tei-c.org/ns/1.0">
                  <text>
                    <body>
                      <pb n="9v"/>
                      <p>Content for 9v.</p>
                      <pb n="10r"/>
                      <p>Content for 10r.</p>
                      <pb n="10v"/>
                      <p>Content for 10v.</p>
                      <pb n="11r"/>
                      <p>Content for 11r.</p>
                    </body>
                  </text>
                </TEI>
                """;

        String imageId = "Douce195.010v.tif";

        Method method = IIIFPresentationGenerator.class.getDeclaredMethod(
                "extractPageTranscription", String.class, String.class);
        method.setAccessible(true);

        String result = (String) method.invoke(generator, teiXml, imageId);

        assertNotNull(result,
                "extractPageTranscription() should return non-null for image 'Douce195.010v.tif' " +
                "matching TEI <pb n=\"10v\"/>. Got null because '010v' does not match '10v'.");
        assertTrue(result.contains("Content for 10v"),
                "Should contain the transcription text for folio 10v");
    }
}
