package rosa.archive.tool;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import rosa.archive.tool.config.ToolConfig;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 *
 */
public class ArchiveToolTest {

    private ArchiveTool tool;

    @Before
    public void setup() {
        ToolConfig config = mock(ToolConfig.class);

        when(config.getArchivePath()).thenReturn("");
    }

    @Test
    @Ignore
    public void testTest() {

    }

}
