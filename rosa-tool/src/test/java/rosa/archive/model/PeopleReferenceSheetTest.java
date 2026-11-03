package rosa.archive.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import rosa.archive.core.serialize.ArchiveReaders;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PeopleReferenceSheetTest {

    private static final String PEOPLE_CSV = """
            sci,Alternative name (1),Alternative name (2),Alternative name (3),Alternative name (4),,ISNI,Extra info,
            Aaron,,,,,,-,,
            Marcus Tullius Cicero,Cicero,Tully,,,,0000 0001 2095 0591,,
            Angelus Furius,,,,,,-,This is one of Harvey's personae.,
            Mary the Jewess,Maria,,,,the alchemist,-,,
            """;

    @Test
    void alternatesComeFromTheAlternativeNameColumnsOnly() {
        PeopleReferenceSheet sheet = new PeopleReferenceSheet();
        sheet.addValues("Angelus Furius", "Angelus Furius", "", "", "", "", "", "-", "This is one of Harvey's personae.", "");
        sheet.addValues("Marcus Tullius Cicero", "Marcus Tullius Cicero", "Cicero", "Tully", "", "", "", "0000 0001 2095 0591", "", "");
        sheet.addValues("Mary the Jewess", "Mary the Jewess", "Maria", "", "", "", "the alchemist", "-", "", "");

        assertEquals(List.of(), sheet.getAlternates("Angelus Furius"));
        assertEquals(List.of("Cicero", "Tully"), sheet.getAlternates("Marcus Tullius Cicero"));
        assertEquals(List.of("Maria", "the alchemist"), sheet.getAlternates("Mary the Jewess"));
    }

    @Test
    void isniIgnoresThePlaceholder() {
        PeopleReferenceSheet sheet = new PeopleReferenceSheet();
        sheet.addValues("Aaron", "Aaron", "", "", "", "", "", "-", "", "");
        sheet.addValues("Marcus Tullius Cicero", "Marcus Tullius Cicero", "Cicero", "Tully", "", "", "", "0000 0001 2095 0591", "", "");

        assertNull(sheet.getIsni("Aaron"));
        assertEquals("0000 0001 2095 0591", sheet.getIsni("Marcus Tullius Cicero"));
        assertEquals("Marcus Tullius Cicero",
                sheet.findKeyByColumnValue("0000 0001 2095 0591", PeopleReferenceSheet.ISNI_COLUMN));
    }

    @Test
    void readerSkipsTheHeaderRow(@TempDir Path dir) throws Exception {
        Path csv = dir.resolve("people.csv");
        Files.writeString(csv, PEOPLE_CSV, StandardCharsets.UTF_8);
        List<String> errors = new ArrayList<>();

        PeopleReferenceSheet sheet = ArchiveReaders.readPeopleReferenceSheet(csv, errors);

        assertTrue(errors.isEmpty(), errors.toString());
        assertFalse(sheet.containsKey("sci"));
        assertTrue(sheet.containsKey("Aaron"));
        assertEquals(List.of("Cicero", "Tully"), sheet.getAlternates("Marcus Tullius Cicero"));
        assertEquals(List.of(), sheet.getAlternates("Angelus Furius"));
        assertEquals(List.of("Maria", "the alchemist"), sheet.getAlternates("Mary the Jewess"));
    }

    @Test
    void readerReturnsNullForAMissingFile(@TempDir Path dir) throws Exception {
        assertNull(ArchiveReaders.readPeopleReferenceSheet(dir.resolve("people.csv"), new ArrayList<>()));
    }
}
