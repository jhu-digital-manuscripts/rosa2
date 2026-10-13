package rosa.archive.iiif;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import rosa.archive.core.FileSystemArchiveStore;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests the annotation body structure (IIIF cookbook recipes 0019 and 0045) and the
 * annotation stylesheet option of {@link IIIFPresentationGenerator}.
 */
class AnnotationBodyGenerationTest {

    private final ObjectMapper mapper = new ObjectMapper();

    private Path generate(Path outputDir, String stylesheetTemplate) throws IOException, URISyntaxException {
        Path archivePath = Path.of(getClass().getClassLoader().getResource("archive").toURI());
        IIIFPresentationGenerator generator = new IIIFPresentationGenerator(new IIIFJsonWriter());
        generator.setAnnotationStylesheetUrl(stylesheetTemplate);
        generator.generate(new FileSystemArchiveStore(archivePath), outputDir, "http://example.org", null, 2);
        return outputDir;
    }

    private JsonNode items(Path outputDir, String canvasPath) throws IOException {
        return mapper.readTree(Files.readString(outputDir.resolve(canvasPath).resolve("annotations.json"))).get("items");
    }

    @Test
    void bodiesAreSpecificResourcesWithStylesheet(@TempDir Path outputDir) throws Exception {
        generate(outputDir, "https://example.org/styles/{collection}.css");

        // aor -> aor.css; rose (parent dlmm) -> dlmm.css
        JsonNode aor = items(outputDir, "aor/PrincetonU101/canvas/14");
        JsonNode rose = items(outputDir, "rose/Douce332/canvas/6");
        for (JsonNode item : aor) {
            assertEquals("https://example.org/styles/aor.css", item.path("stylesheet").asText());
        }
        for (JsonNode item : rose) {
            assertEquals("https://example.org/styles/dlmm.css", item.path("stylesheet").asText());
        }

        JsonNode body = aor.get(1).get("body");
        assertEquals("SpecificResource", body.path("type").asText());
        assertTrue(body.path("styleClass").asText().matches("rosa-anno aor-\\w+( aor-place-\\w+)?"),
                body.path("styleClass").asText());
        JsonNode source = body.get("source");
        assertEquals("TextualBody", source.path("type").asText());
        assertEquals("text/html", source.path("format").asText());
    }

    @Test
    void noStylesheetWhenNotConfigured(@TempDir Path outputDir) throws Exception {
        generate(outputDir, null);
        for (JsonNode item : items(outputDir, "aor/PrincetonU101/canvas/14")) {
            assertFalse(item.has("stylesheet"));
        }
    }

    @Test
    void languagesAreLowercaseOrOmitted(@TempDir Path outputDir) throws Exception {
        generate(outputDir, null);
        boolean sawMissing = false;
        for (JsonNode item : items(outputDir, "aor/PrincetonU101/canvas/14")) {
            JsonNode source = item.get("body").get("source");
            if (source.has("language")) {
                String lang = source.get("language").asText();
                assertFalse(lang.isEmpty());
                assertEquals(lang.toLowerCase(), lang);
            } else {
                sawMissing = true;
            }
        }
        assertTrue(sawMissing, "Marks without a language should omit the language property");
    }

    @Test
    void transcriptionIsSplitByColumn(@TempDir Path outputDir) throws Exception {
        generate(outputDir, null);
        JsonNode items = items(outputDir, "rose/Douce332/canvas/6");
        int columns = 0;
        for (JsonNode item : items) {
            String cls = item.get("body").path("styleClass").asText();
            if (cls.contains("dlmm-transcription")) {
                columns++;
                String html = item.get("body").get("source").path("value").asText();
                assertNull(AnnotationHtml.findViolation(html), html);
                assertTrue(cls.contains("dlmm-column-"), cls);
            }
        }
        assertEquals(2, columns, "Douce332 001r has two columns");
    }

    @Test
    void commentaryHtmlIsCompliant() throws Exception {
        Path archivePath = Path.of(getClass().getClassLoader().getResource("archive").toURI());
        JsonNode annos = mapper.readTree(Files.readString(archivePath.resolve("rose/annos.jsonld")));
        assertTrue(annos.size() > 0);
        for (JsonNode anno : annos) {
            String html = anno.path("body").path("value").asText();
            assertNull(AnnotationHtml.findViolation(html), anno.path("id").asText() + ": " + html);
        }

        // The loaded collection exposes the same bodies to the generator
        rosa.archive.model.BookCollection rose = new FileSystemArchiveStore(archivePath).loadCollection("rose");
        String html = rose.getHTMLAnnotations().getAnnotation("Francais12595.069v.tif");
        assertNotNull(html);
        assertTrue(html.contains("<p><b>Scene depicted:</b> Jealous husband beats his wife.</p>"), html);
    }

    @Test
    void stylesheetResolvesParentCollection() {
        IIIFPresentationGenerator generator = new IIIFPresentationGenerator(new IIIFJsonWriter());
        rosa.archive.model.BookCollection rose = collection("rose", "dlmm");
        rosa.archive.model.BookCollection aor = collection("aor");

        assertNull(generator.resolveAnnotationStylesheet(aor));
        generator.setAnnotationStylesheetUrl("/styles/{collection}.css");
        assertEquals("/styles/dlmm.css", generator.resolveAnnotationStylesheet(rose));
        assertEquals("/styles/aor.css", generator.resolveAnnotationStylesheet(aor));
    }

    private static rosa.archive.model.BookCollection collection(String id, String... parents) {
        rosa.archive.model.BookCollection c = new rosa.archive.model.BookCollection();
        c.setId(id);
        rosa.archive.model.CollectionMetadata md = new rosa.archive.model.CollectionMetadata();
        md.setParents(parents);
        c.setMetadata(md);
        return c;
    }
}
