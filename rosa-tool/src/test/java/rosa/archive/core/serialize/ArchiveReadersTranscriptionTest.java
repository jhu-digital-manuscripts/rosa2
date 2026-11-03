package rosa.archive.core.serialize;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import rosa.archive.model.Transcription;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link ArchiveReaders#readTranscription(Path)}.
 */
class ArchiveReadersTranscriptionTest {

    @Test
    @DisplayName("Keeps the file's line breaks, so text that runs across lines keeps its word breaks")
    void keepsLineBreaks(@TempDir Path dir) throws IOException {
        String xml = "<TEI><text><body><div>\n"
                + "  <pb n=\"5r\"/>\n"
                + "  <l>\n"
                + "    <hi rend=\"rubric\"><hi rend=\"init\">C</hi>Cy est le Rommant de la rose\n"
                + "ou tout l'art d'amours est enclose</hi>\n"
                + "  </l>\n"
                + "</div></body></text></TEI>\n";
        Path file = dir.resolve("Morgan948.transcription.xml");
        Files.writeString(file, xml, StandardCharsets.UTF_8);

        Transcription transcription = ArchiveReaders.readTranscription(file);

        assertNotNull(transcription);
        assertEquals(xml, transcription.getXML());
        assertFalse(transcription.getXML().contains("roseou"));
    }

    @Test
    @DisplayName("Returns null for a missing file")
    void missingFile(@TempDir Path dir) throws IOException {
        assertNull(ArchiveReaders.readTranscription(dir.resolve("none.transcription.xml")));
    }
}
