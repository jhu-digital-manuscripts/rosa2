package rosa.archive.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.junit.Test;

/**
 * Test against data in src/test/resources/archive.
 * 
 * TODO Test all methods
 */
public class FSByteStreamGroupTest extends BaseArchiveTest {

    @Test
    public void resolveNameTest() throws IOException {
        String[] test = {"valid", "character_names.csv", "LudwigXV7.001r.tif", "LudwigXV7.description_en.xml",
                         "fakeName"};
        // Test at collection level
        checkResolvedName(base.resolveName(test[0]), base.id(), test[0]);

        // Test in a collection
        ByteStreamGroup collectionGroup = base.getByteStreamGroup(VALID_COLLECTION);
        String[] expected = {null, "character_names.csv", null, null, null};
        for (int i = 0; i < test.length; i++) {
            String t = test[i];
            String expect = expected[i];

            checkResolvedName(collectionGroup.resolveName(t), collectionGroup.id(), expect);
        }

        // Test in a book
        ByteStreamGroup bookGroup = collectionGroup.getByteStreamGroup(VALID_BOOK_LUDWIGXV7);
        String[] expected1 = {null, null, "LudwigXV7.001r.tif", "LudwigXV7.description_en.xml", null};
        for (int i = 0; i < test.length; i++) {
            String t = test[i];
            String expect = expected1[i];

            checkResolvedName(bookGroup.resolveName(t), bookGroup.id(), expect);
        }

    }

    private void checkResolvedName(String result, String expectedPrefix, String expectedName) {
        if (expectedName != null) {
            assertNotNull(result);
            assertTrue("Unexpected prefix.", result.startsWith(expectedPrefix));
            assertTrue("Unexpected file name.", result.endsWith(expectedName));
        } else {
            assertNull(result);
        }
    }
  
    @Test
    public void zeroByteStreamsAtTop() throws IOException {
        int numberOfByteStreams = base.numberOfByteStreams();
        assertEquals(0, numberOfByteStreams);
    }

    @Test
    public void twoBSGAtTop() throws IOException {
        assertEquals(1, base.numberOfByteStreamGroups());
    }

    @Test
    public void getsDataByteStreamGroup() throws IOException {
        ByteStreamGroup data = base.getByteStreamGroup("valid");
        assertNotNull(data);
    }

    @Test
    public void getsCharNamesBS() throws IOException {
        ByteStreamGroup data = base.getByteStreamGroup("valid");
        assertNotNull(data);

        InputStream in = data.getByteStream("character_names.csv");
        assertNotNull(in);
    }

    @Test
    public void dataFolderHasSixByteStreams() throws IOException {
        ByteStreamGroup data = base.getByteStreamGroup("valid");
        assertNotNull(data);
        assertEquals(10, data.numberOfByteStreams());
    }

    @Test
    public void dataFolderBSIdsTest() throws IOException {
        ByteStreamGroup data = base.getByteStreamGroup("valid");
        assertNotNull(data);

        List<String> ids = data.listByteStreamIds();
        assertNotNull(ids);
        assertEquals(10, ids.size());
    }

    @Test
    public void dataFolderByteStreamNamesTest() throws IOException {
        ByteStreamGroup data = base.getByteStreamGroup("valid");
        assertNotNull(data);

        List<String> names = data.listByteStreamNames();
        assertNotNull(names);
        assertTrue(names.contains("character_names.csv"));
        assertTrue(names.contains("illustration_titles.csv"));
        assertTrue(names.contains("narrative_sections.csv"));
    }

    @Test
    public void dataFolderBSGNamesTest() throws IOException {
        ByteStreamGroup data = base.getByteStreamGroup("valid");
        assertNotNull(data);

        List<String> names = data.listByteStreamGroupNames();
        assertNotNull(names);
        assertTrue(names.contains("FolgersHa2"));
        assertTrue(names.contains("LudwigXV7"));
    }

    @Test
    public void dataFolderBSGIdsTest() throws IOException {
        ByteStreamGroup data = base.getByteStreamGroup("valid");
        assertNotNull(data);

        List<String> ids = data.listByteStreamGroupIds();
        assertNotNull(ids);
        assertEquals(2, ids.size());
    }

    @Test
    public void hasAllBSGInData() throws IOException {
        ByteStreamGroup data = base.getByteStreamGroup("valid");
        assertNotNull(data);

        List<ByteStreamGroup> bsgs = data.listByteStreamGroups();
        assertNotNull(bsgs);
        assertEquals(2, bsgs.size());
    }

    @Test
    public void hasBSGTest() throws IOException {
        assertTrue(base.hasByteStreamGroup("valid"));
    }

    @Test
    public void hasBSTest() throws IOException {
        ByteStreamGroup data = base.getByteStreamGroup("valid");
        assertNotNull(data);

        assertTrue(data.hasByteStream("character_names.csv"));
        assertTrue(data.hasByteStream("illustration_titles.csv"));
        assertTrue(data.hasByteStream("narrative_sections.csv"));
    }
}
