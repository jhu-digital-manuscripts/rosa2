package rosa.archive.tool;

import org.junit.Before;
import org.junit.Test;
import rosa.archive.core.store.Store;
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

        when(config.getARCHIVE_PATH()).thenReturn("");
        when(config.getCMD_LIST()).thenReturn("list");
        when(config.getCMD_CHECK()).thenReturn("check");
        when(config.getFLAG_CHECK_BITS()).thenReturn("-checkBits");
        when(config.getFLAG_SHOW_ERRORS()).thenReturn("-showErrors");
    }

    @Test
    public void testTest() {

    }

}
