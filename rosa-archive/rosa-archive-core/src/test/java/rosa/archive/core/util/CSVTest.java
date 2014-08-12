package rosa.archive.core.util;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Testing the {@link rosa.archive.core.util.CSV} class, copied from rosa1 project.
 */
public class CSVTest {

    @Test
    public void parseValidInputString() {
        final String csv = "G 6c,1-50,935-984,\"Douz Regart’s arrows of love, 5 positive, 5 negative. The courtly: Biautez, Simpleice, Franchise, Compaignie, Bel Samblant; the anti-courtly: Orguelz, Vilanie, Felonie, Honte, Desesperance.\"";
        String[] parsed = CSV.parse(csv);

        assertNotNull(parsed);
        assertEquals(4, parsed.length);

        assertEquals("G 6c", parsed[0]);
        assertEquals("1-50", parsed[1]);
        assertEquals("935-984", parsed[2]);
        assertEquals(
                "Douz Regart’s arrows of love, 5 positive, 5 negative. The courtly: Biautez, Simpleice, Franchise, Compaignie, Bel Samblant; the anti-courtly: Orguelz, Vilanie, Felonie, Honte, Desesperance.",
                parsed[3]
        );

    }

    @Test
    public void escapesWhenCommaIsPresent() {
        String input = "This is and input string, or just a regular string!";
        String result = CSV.escape(input);

        assertTrue("Result should start with [\"]", result.startsWith("\""));
        assertTrue("Result should end with [\"]", result.endsWith("\""));
    }

    @Test
    public void parsesInputFileToArray() {

        try (InputStreamReader reader = new InputStreamReader(
                getClass().getClassLoader().getResourceAsStream("sample_csv.csv"))) {

            String[][] table = CSV.parseTable(reader);

            assertEquals(67, table.length);
            for (String[] row : table) {
                assertEquals("Unexpected row length: [" + row.length + "], should be [4].", 4, row.length);
            }

        } catch (IOException e) {
            fail("Could not open file.");
        }

    }

    @Test
    public void shouldTrimWhiteSpacesAndNormalize() {
        final String[][] input = {
                {"G 6c", "1-50,935-984", "\" Douz Regart’s arrows of love, 5 positive, 5 negative.\""},
                {"G \t6c", "1-50,935-984\t", "\"\tDouz Regart’s arrows of love, 5 positive, 5 negative.\""},
                {"G       6c", "1-50,935-984       ", "\"      Douz Regart’s arrows of love, 5 positive, 5 negative.\""}
        };
        int[] expected_length = { 4, 12, 56 };

        CSV.normalizeWhiteSpaceAndCharacters(input);

        assertNotNull(input);
        assertEquals(3, input.length);
        for (String[] row : input) {
            assertEquals(3, row.length);
            for (int j = 0; j < expected_length.length; j++) {
                assertEquals(expected_length[j], row[j].length());
            }
        }
    }

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    @Test
    public void writesToFile() throws IOException {
        final String[][] data = {
                {"G 6c", "1-50,935-984", "\"Douz Regart’s arrows of love, 5 positive, 5 negative.\""},
                {"G 6c", "1-50,935-984", "\"Douz Regart’s arrows of love, 5 positive, 5 negative.\""},
                {"G 6c", "1-50,935-984", "\"Douz Regart’s arrows of love, 5 positive, 5 negative.\""}
        };

        File testFile = tmp.newFile("test");
        Writer writer = Files.newBufferedWriter(Paths.get(testFile.toURI()));

        CSV.write(writer, data);

        assertTrue(testFile.canRead());
        assertTrue(testFile.isFile());
        assertTrue(testFile.length() > 0);

        tmp.delete();

    }

}
