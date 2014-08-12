package rosa.archive.core.util;

import org.junit.Before;
import org.junit.Test;

import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertNotNull;

/**
 * @see rosa.archive.core.util.CSVSpreadSheet
 */
public class CSVSpreadSheetTest {

    private CSVSpreadSheet fromStream;
    private CSVSpreadSheet fromFile;

    @Before
    public void setup() throws Exception {
        Reader input = new InputStreamReader(
                getClass().getClassLoader().getResourceAsStream("sample_csv.csv")
        );
        List<String> errors = new ArrayList<>();

        URL url = getClass().getClassLoader().getResource("sample_csv.csv");
        assertNotNull(url);
        Path path = Paths.get(url.toURI());
        assertNotNull(path);

        this.fromStream = new CSVSpreadSheet(input, 4, 4, errors);
        this.fromFile = new CSVSpreadSheet(path.toFile(), 4, 4, errors);
    }

    @Test
    public void csvTest() {

    }

}
