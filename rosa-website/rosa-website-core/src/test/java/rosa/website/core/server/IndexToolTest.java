package rosa.website.core.server;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import rosa.archive.core.BaseArchiveTest;
import rosa.search.core.LuceneSearchService;
import rosa.search.core.SearchService;
import rosa.search.tool.Tool;
import rosa.website.search.WebsiteLuceneMapper;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Path;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class IndexToolTest extends BaseArchiveTest {

    @Rule
    public final TemporaryFolder tmp = new TemporaryFolder();
    private Path targetIndexPath;

    @Before
    public void setup() throws IOException {
        File luceneDir = tmp.newFolder("lucene");
        this.targetIndexPath = luceneDir.toPath();
    }

    /**
     * Test to make sure the tool will create a Lucene index in the correct place.
     */
    @Test
    public void createIndexTest() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        SearchService service = new LuceneSearchService(targetIndexPath, new WebsiteLuceneMapper());
        Tool tool = new Tool(store, service, new PrintStream(out));

        tool.process(new String[] {VALID_COLLECTION});

        String[] content = targetIndexPath.toFile().list();
        assertNotNull("Could not list contents of target index directory.", content);
        assertTrue("Nothing found in target index directory.", content.length > 0);

        assertTrue("Unexpected output received from tool.", out.toString("UTF-8").isEmpty());
    }

    @Test
    public void doNothingWithBadInput() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        SearchService service = new LuceneSearchService(targetIndexPath, new WebsiteLuceneMapper());
        Tool tool = new Tool(store, service, new PrintStream(out));

        tool.process(new String[] {VALID_COLLECTION, "blah"});

        assertFalse("Output messages were expected from tool.", out.toString("UTF-8").isEmpty());
        assertTrue(out.toString("UTF-8").startsWith("Must provide one arguments. Usage: <tool> <collectionName>"));
    }

}
