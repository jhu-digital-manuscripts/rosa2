package rosa.archive.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
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
        assertEquals(6, data.numberOfByteStreams());
    }

    @Test
    public void dataFolderBSIdsTest() throws IOException {
        ByteStreamGroup data = base.getByteStreamGroup("valid");
        assertNotNull(data);

        List<String> ids = data.listByteStreamIds();
        assertNotNull(ids);
        assertEquals(6, ids.size());
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
