package rosa.archive.core.serialize;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import rosa.archive.model.BookDescription;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link ArchiveReaders#readBookDescription(Path, List)}.
 */
class ArchiveReadersBookDescriptionTest {

    private static final Path TEST_RESOURCES = Path.of("src/test/resources/archive/rose");

    @Test
    @DisplayName("Parses Douce195 English description")
    void parsesDouce195EnglishDescription() throws IOException {
        Path path = TEST_RESOURCES.resolve("Douce195/Douce195.description_en.xml");
        List<String> errors = new ArrayList<>();

        BookDescription description = ArchiveReaders.readBookDescription(path, errors);

        assertNotNull(description);
        assertTrue(errors.isEmpty(), "Should have no errors: " + errors);

        // Check that notes were parsed
        assertFalse(description.getNotes().isEmpty(), "Should have parsed notes");

        // Check IDENTIFICATION note
        String identification = description.getNote("IDENTIFICATION");
        assertNotNull(identification, "Should have IDENTIFICATION note");
        assertTrue(identification.contains("MS Douce 195"));
        assertTrue(identification.contains("Bodleian Library"));
        assertTrue(identification.contains("Roman de la Rose"));

        // Check BASIC INFORMATION note
        String basicInfo = description.getNote("BASIC INFORMATION");
        assertNotNull(basicInfo, "Should have BASIC INFORMATION note");
        assertTrue(basicInfo.contains("Parchment"));
        assertTrue(basicInfo.contains("345 x 235 mm"));
        assertTrue(basicInfo.contains("156 folios"));

        // Check MATERIAL note
        String material = description.getNote("MATERIAL");
        assertNotNull(material, "Should have MATERIAL note");
        assertTrue(material.contains("Parchment"));

        // Check DECORATION note
        String decoration = description.getNote("DECORATION");
        assertNotNull(decoration, "Should have DECORATION note");
        assertTrue(decoration.contains("125 miniatures"));

        // Check HISTORY note
        String history = description.getNote("HISTORY");
        assertNotNull(history, "Should have HISTORY note");
        assertTrue(history.contains("Francis Douce"));
    }

    @Test
    @DisplayName("getFullText returns concatenated notes")
    void getFullTextReturnsConcatenatedNotes() throws IOException {
        Path path = TEST_RESOURCES.resolve("Douce195/Douce195.description_en.xml");
        List<String> errors = new ArrayList<>();

        BookDescription description = ArchiveReaders.readBookDescription(path, errors);

        assertNotNull(description);
        String fullText = description.getFullText();
        assertNotNull(fullText);
        assertFalse(fullText.isBlank(), "Full text should not be blank");

        // Full text should contain content from various notes
        assertTrue(fullText.contains("MS Douce 195"));
        assertTrue(fullText.contains("Parchment"));
        assertTrue(fullText.contains("125 miniatures"));
    }

    @Test
    @DisplayName("Returns null for non-existent file")
    void returnsNullForMissingFile() throws IOException {
        List<String> errors = new ArrayList<>();
        BookDescription description = ArchiveReaders.readBookDescription(
                Path.of("non-existent.description_en.xml"), errors);
        assertNull(description);
    }

    @Test
    @DisplayName("Notes without rend attribute are stored as OTHER")
    void notesWithoutRendAttributeStoredAsOther() throws IOException {
        Path path = TEST_RESOURCES.resolve("Douce195/Douce195.description_en.xml");
        List<String> errors = new ArrayList<>();

        BookDescription description = ArchiveReaders.readBookDescription(path, errors);

        assertNotNull(description);
        // The file has a note without @rend at the end: "Description by Timothy L. Stinson"
        String other = description.getNote("OTHER");
        assertNotNull(other, "Should have OTHER note for notes without @rend");
        assertTrue(other.contains("Timothy L. Stinson"));
    }
}
