package rosa.archive.aor;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import rosa.archive.core.ResourceUtil;
import rosa.archive.core.util.CSV;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class GitStatsCollectorTest {
    private static final int BOOKS_COLUMNS = 23;
    private static final int COMMITS_COLUMNS = 12;

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();
    protected Path basePath;

    private GitStatCollector collector;

    @Before
    public void setup() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        collector = new GitStatCollector();
        collector.setReport(new PrintStream(out));
        collector.setError(new PrintStream(out));

        Path temp = tempFolder.newFolder().toPath();
        ResourceUtil.copyResource(getClass(), "/archive", temp);

        basePath = temp.resolve("archive");
    }

    /**
     * Gather statistics for only the most recent commit.
     *
     * books.csv should have 4 rows (3 books + header)
     * commits.csv should have 2 rows (1 commit + header)
     *
     * @throws Exception
     */
    @Test
    public void collectGitStatsMostRecentTest() throws Exception {
        Path output = basePath.getParent();

        collector.setOutputDirectory(output.toString());
        collector.collectGitStats("https://github.com/jabrah/test-transcriptions.git", true);

        assertTrue("books.csv not found.", Files.exists(output.resolve("books.csv")));
        assertTrue("commits.csv not found.", Files.exists(output.resolve("commits.csv")));

        try (InputStreamReader reader = new InputStreamReader(Files.newInputStream(output.resolve("books.csv")))) {
            String[][] table = CSV.parseTable(reader);

            assertEquals("Unexpected number of rows.", 4, table.length);
            assertEquals("Unexpected number of columns.", BOOKS_COLUMNS, table[0].length);

            String[] row = table[1];
            /*
commit_id,book,total,total_words,marginalia,marginalia_words,underlines,underline_words,marks,mark_words,symbols,symbol_words,drawings,numerals,books,people,locations,added,modified,deleted,renamed,copied
,,7,39,7,39,0,0,0,0,0,0,0,0,0,2,0,0,0,0,0,0
             */
            assertEquals("Unexpected ID found.", "734231ce89165661a4ad4c103a0f3b33ef0b61dc", row[0]);
            assertEquals("Unexpected book name found.", "Castiglione", row[1]);
            assertEquals("Unexpected number of annotations found.", "7", row[2]);
            assertEquals("Unexpected number of annotated words found.", "39", row[3]);

        }

        try (InputStreamReader reader = new InputStreamReader(Files.newInputStream(output.resolve("commits.csv")))) {
            String[][] table = CSV.parseTable(reader);

            assertEquals("Unexpected number of rows.", 2, table.length);
            assertEquals("Unexpected number of columns.", COMMITS_COLUMNS, table[0].length);

            String[] row = table[1];

            assertEquals("Unexpected ID found.", "734231ce89165661a4ad4c103a0f3b33ef0b61dc", row[0]);
            assertEquals("Unexpected parent ID found.", "e3cfd1f281de07530ad26c7fdb0a4e53aadead4c", row[1]);
            assertEquals("Unexpected date found.", "2015-07-30T03:24:47-0400", row[2]);
            assertEquals("Unexpected name found.", "jabrah", row[3]);
            assertEquals("Unexpected email found.", "jabrah23@gmail.com", row[4]);
            assertEquals("Unexpected message found.", "Delete Ha2.001r.xml", row[5]);
            assertEquals("Unexpected number of added files found.", "0", row[6]);
            assertEquals("Unexpected number of modified files found.", "0", row[7]);
            assertEquals("Unexpected number of deleted files found.", "1", row[8]);
            assertEquals("Unexpected number of renamed files found.", "0", row[9]);
            assertEquals("Unexpected number of copied files found.", "0", row[10]);
        }
    }

    @Test
    public void collectGitStatsTest() throws Exception {
        Path output = basePath.getParent();

        // Do stats collection on test repository
        collector.setOutputDirectory(output.toString());
        collector.collectGitStats("https://github.com/jabrah/test-transcriptions.git", false);

        // Check output files
        assertTrue("books.csv not found.", Files.exists(output.resolve("books.csv")));
        assertTrue("commits.csv not found.", Files.exists(output.resolve("commits.csv")));

        checkCommitsCsv(output.resolve("commits.csv"));
        checkBooksCsv(output.resolve("books.csv"));
    }

    /**
     * Init both output files with dummy data, then run {@link #collectGitStatsTest()}.
     * This will generate new and valid output that should overwrite all initialized
     * data.
     *
     * @throws Exception
     */
    @Test
    public void collectGitStatsTwiceTest() throws Exception {
        String init_data = "stuff to initialize initialize a data file.";
        Path output = basePath.getParent();

        Files.write(output.resolve("books.csv"), init_data.getBytes());
        Files.write(output.resolve("commits.csv"), init_data.getBytes());

        collectGitStatsTest();
    }

    private void checkCommitsCsv(Path pathToCsv) throws IOException {
        assertTrue("File not found.", Files.exists(pathToCsv));

        try (InputStreamReader reader = new InputStreamReader(Files.newInputStream(pathToCsv))) {
            String[][] table = CSV.parseTable(reader);
//print(table);
            assertNotNull(table);
            assertEquals("Unexpected number of rows.", 8, table.length);
            assertEquals("Unexpected number of columns", COMMITS_COLUMNS, table[0].length);

            String[] row = table[1];        // Get first data row

            assertEquals("Unexpected ID found.", "734231ce89165661a4ad4c103a0f3b33ef0b61dc", row[0]);
            assertEquals("Unexpected parent ID found.", "e3cfd1f281de07530ad26c7fdb0a4e53aadead4c", row[1]);
            assertEquals("Unexpected date found.", "2015-07-30T03:24:47-0400", row[2]);
            assertEquals("Unexpected name found.", "jabrah", row[3]);
            assertEquals("Unexpected email found.", "jabrah23@gmail.com", row[4]);
            assertEquals("Unexpected message found.", "Delete Ha2.001r.xml", row[5]);
            assertEquals("Unexpected number of added files found.", "0", row[6]);
            assertEquals("Unexpected number of modified files found.", "0", row[7]);
            assertEquals("Unexpected number of deleted files found.", "1", row[8]);
            assertEquals("Unexpected number of renamed files found.", "0", row[9]);
            assertEquals("Unexpected number of copied files found.", "0", row[10]);
        }
    }

    private void checkBooksCsv(Path pathToCsv) throws IOException {
        assertTrue("File not found.", Files.exists(pathToCsv));

        try (InputStreamReader reader = new InputStreamReader(Files.newInputStream(pathToCsv))) {
            String[][] table = CSV.parseTable(reader);

            assertNotNull(table);
            assertEquals("Unexpected number of rows.", 15, table.length);
            assertEquals("Unexpected number of columns", BOOKS_COLUMNS, table[0].length);
        }
    }

    private void print(String[][] table) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            CSV.write(new OutputStreamWriter(out), table);
            System.out.println(out);
        } catch (IOException e) {
            System.err.println("Failed to write table.");
        }
    }

    /**
     * Gather stats for the AoR repository in an early state.
     *
     * In resources, there exists a copy of the AoR repository in an early state.
     * The stats gathering should show that there exist 3 books with a set number
     * of annotations in each. Since this repository in test/resources is not a
     * real git repository, no git commands can be done on it.
     *
     * @throws Exception
     */
    @Test
    public void collectBookStatsTest() throws Exception {
        BookStats stats = collector.collectBookStats(basePath);

        assertNotNull(stats);
        assertEquals("There should be exactly 3 books counted.", 3, stats.statsMap.size());

        assertNotNull("Book Domenichi not found.", stats.statsMap.get("Domenichi"));
        assertNotNull("Book Castiglione not found.", stats.statsMap.get("Castiglione"));
        assertNotNull("Book Livy not found.", stats.statsMap.get("Livy"));

        assertEquals("Unexpected number of annotations found for Domenichi.",
                1449, stats.statsMap.get("Domenichi").totalAnnotations());
        assertEquals("Unexpected number of annotions found for Castiglione.",
                17, stats.statsMap.get("Castiglione").totalAnnotations());
        assertEquals("Unexpected number of annotations found for Livy.",
                1587, stats.statsMap.get("Livy").totalAnnotations());

        assertEquals("Unexpected number of unreadable pages.", 33, stats.getNumberOfUnreadablePages("Domenichi"));
        assertEquals("Unexpected number of unreadable pages.", 9, stats.getNumberOfUnreadablePages("Livy"));
        assertEquals("Unexpected number of unreadable pages.", 1, stats.getNumberOfUnreadablePages("Castiglione"));
    }

}
