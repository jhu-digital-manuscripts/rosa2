package rosa.archive.core.serialize;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import rosa.archive.model.aor.IdLocations;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link ArchiveReaders#readIdLocations(Path, List)}.
 */
class ArchiveReadersIdLocationsTest {

    @Test
    void readsTheFixtureSheet() throws Exception {
        Path archive = Path.of(getClass().getClassLoader().getResource("archive").toURI());
        List<String> errors = new ArrayList<>();
        IdLocations locations = ArchiveReaders.readIdLocations(archive.resolve("aor/id_locations.csv"), errors);

        assertNotNull(locations);
        // One row of the real sheet is an id containing commas and cannot be parsed
        assertEquals(1, errors.size(), errors.toString());
        assertTrue(errors.get(0).startsWith("id_locations.csv line 5880: expected 5 columns"), errors.get(0));
        assertEquals(38119, locations.size());

        IdLocations.IdLocation annotation = locations.get("BL615d7:615_d_7_f035r:m0001");
        assertEquals(new IdLocations.IdLocation("aor", "BL615d7", "35r", "BL615d7:615_d_7_f035r:m0001"), annotation);
        assertEquals(new IdLocations.IdLocation("aor", "PrincetonU101", "14v", ""), locations.get("PrincetonU101:00000034"));
        assertEquals(new IdLocations.IdLocation("aor", "RCP7134", "177v", ""), locations.get("RCP7134:360"));
        assertNull(locations.get("nope"));
        assertFalse(locations.contains(null));
    }

    @Test
    void parsesPageFormsAndSkipsBadRows(@TempDir Path dir) throws Exception {
        Path csv = dir.resolve("id_locations.csv");
        Files.writeString(csv, """
                X:1,aor,X,1r,
                X:2,aor,X,X.002r.tif,

                X:3,aor,X,front matter 5r,
                X:4,aor,X
                "a, b":5,aor,X,5r,"a, b":5
                """);
        List<String> errors = new ArrayList<>();
        IdLocations locations = ArchiveReaders.readIdLocations(csv, errors);
        assertEquals(4, locations.size());
        assertEquals("1r", locations.get("X:1").page());
        assertEquals("X.002r.tif", locations.get("X:2").page());
        assertEquals("front matter 5r", locations.get("X:3").page());
        assertEquals("a, b:5", locations.get("a, b:5").annotationId());
        assertEquals(1, errors.size());
        assertTrue(errors.get(0).contains("line 5"), errors.get(0));
    }

    @Test
    void missingFileIsNull() throws Exception {
        assertNull(ArchiveReaders.readIdLocations(Path.of("/nonexistent/id_locations.csv"), new ArrayList<>()));
    }
}
