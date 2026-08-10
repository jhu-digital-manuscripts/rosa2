package rosa.archive.core.serialize;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import rosa.archive.model.Illustration;
import rosa.archive.model.IllustrationTagging;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link ArchiveReaders#readIllustrationTagging(Path, List)}.
 */
class ArchiveReadersImagetagTest {

    private static final Path TEST_RESOURCES = Path.of("src/test/resources/archive/rose");

    @Test
    @DisplayName("Parses Douce195 format (with Initials column)")
    void parsesFormatWithInitialsColumn() throws IOException {
        Path path = TEST_RESOURCES.resolve("Douce195/Douce195.imagetag.csv");
        List<String> errors = new ArrayList<>();

        IllustrationTagging tagging = ArchiveReaders.readIllustrationTagging(path, errors);

        assertNotNull(tagging);
        assertTrue(errors.isEmpty(), "Should have no errors: " + errors);

        // Row 1: id=1, Folio=1r, title=1, Initials has content, Characters=49
        Illustration illus = tagging.getIllustrationData(0);
        assertEquals("1", illus.getId());
        assertEquals("1r", illus.getPage());
        assertArrayEquals(new String[]{"1"}, illus.getTitles());
        assertNotNull(illus.getInitials());
        assertTrue(illus.getInitials().contains("Blue M"));
        assertArrayEquals(new String[]{"49"}, illus.getCharacters());
        assertEquals("Blue sleeveless outer garment and hat", illus.getCostume());

        // Row 2: id=2, Characters=2, no costume (empty)
        Illustration illus2 = tagging.getIllustrationData(1);
        assertEquals("2", illus2.getId());
        assertArrayEquals(new String[]{"2"}, illus2.getCharacters());
    }

    @Test
    @DisplayName("Parses Douce332 format (with Initials column, multi-value titles)")
    void parsesDouce332WithMultiValueTitles() throws IOException {
        Path path = TEST_RESOURCES.resolve("Douce332/Douce332.imagetag.csv");
        List<String> errors = new ArrayList<>();

        IllustrationTagging tagging = ArchiveReaders.readIllustrationTagging(path, errors);

        assertNotNull(tagging);
        assertTrue(errors.isEmpty(), "Should have no errors: " + errors);

        // Row 1: id=1, titles="3,4,5", Characters=2
        Illustration illus = tagging.getIllustrationData(0);
        assertEquals("1", illus.getId());
        assertEquals("1r", illus.getPage());
        // Titles split by comma or semicolon
        assertArrayEquals(new String[]{"3", "4", "5"}, illus.getTitles());
        assertArrayEquals(new String[]{"2"}, illus.getCharacters());
        assertEquals("Ankle-length gown", illus.getCostume());
    }

    @Test
    @DisplayName("Parses format WITHOUT Initials column (AlbiRochegude-style)")
    void parsesFormatWithoutInitialsColumn() throws IOException {
        Path path = TEST_RESOURCES.resolve("NoInitialsCol/NoInitialsCol.imagetag.csv");
        List<String> errors = new ArrayList<>();

        IllustrationTagging tagging = ArchiveReaders.readIllustrationTagging(path, errors);

        assertNotNull(tagging);
        assertTrue(errors.isEmpty(), "Should have no errors: " + errors);
        assertEquals(3, tagging.size());

        // Row 1: id=1, Characters=2, no costume
        Illustration illus1 = tagging.getIllustrationData(0);
        assertEquals("1", illus1.getId());
        assertEquals("1r", illus1.getPage());
        assertArrayEquals(new String[]{"3"}, illus1.getTitles());
        assertArrayEquals(new String[]{"2"}, illus1.getCharacters());
        assertEquals("", illus1.getCostume());
        // Objects should be "Bed, Bed furniture" (quoted in CSV)
        assertEquals("Bed, Bed furniture", illus1.getObject());

        // Row 2: id=2, Characters=2, costume with commas (quoted)
        Illustration illus2 = tagging.getIllustrationData(1);
        assertEquals("2", illus2.getId());
        assertArrayEquals(new String[]{"2"}, illus2.getCharacters());
        assertEquals("blue hooded robe, red undersleeves", illus2.getCostume());
        // Costume should NOT bleed into characters
        assertFalse(java.util.Arrays.asList(illus2.getCharacters()).contains("blue hooded robe"),
                "Costume value should NOT be parsed as character ID");

        // Row 3: id=16, Characters="4 ; 2" → split by semicolons → ["4", "2"]
        Illustration illus3 = tagging.getIllustrationData(2);
        assertEquals("16", illus3.getId());
        assertArrayEquals(new String[]{"4", "2"}, illus3.getCharacters());
        assertEquals("blue hooded robe on L'Amans, red gown and burgundy mantle on Amors, crown on Amors",
                illus3.getCostume());
        // Initials should be empty since this format doesn't have the column
        assertEquals("", illus3.getInitials());
    }

    @Test
    @DisplayName("Returns null for non-existent file")
    void returnsNullForMissingFile() throws IOException {
        List<String> errors = new ArrayList<>();
        IllustrationTagging tagging = ArchiveReaders.readIllustrationTagging(
                Path.of("non-existent.imagetag.csv"), errors);
        assertNull(tagging);
    }
}
