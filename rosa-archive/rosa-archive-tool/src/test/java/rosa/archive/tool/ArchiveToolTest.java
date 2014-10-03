package rosa.archive.tool;

import org.junit.Before;
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
        when(config.getCmdList()).thenReturn("list");
        when(config.getCmdCheck()).thenReturn("check");
        when(config.getFlagCheckBits()).thenReturn("-checkBits");
        when(config.getFlagShowErrors()).thenReturn("-showErrors");
    }

    @Test
    public void testTest() {

    }

}
