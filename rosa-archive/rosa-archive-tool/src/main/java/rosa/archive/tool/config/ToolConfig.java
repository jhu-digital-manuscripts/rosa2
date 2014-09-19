package rosa.archive.tool.config;

import com.google.inject.Inject;
import com.google.inject.name.Named;

/**
 *
 */
public class ToolConfig {

    @Inject @Named("archive.path")
    private String ARCHIVE_PATH;

    @Inject @Named("CMD_LIST")
    private String CMD_LIST;

    @Inject @Named("CMD_CHECK")
    private String CMD_CHECK;

    @Inject @Named("FLAG_CHECK_BITS")
    private String FLAG_CHECK_BITS;

// -----------------------------------------------------------------------------------
// ------ Getters --------------------------------------------------------------------
// -----------------------------------------------------------------------------------


    public String getCMD_LIST() {
        return CMD_LIST;
    }

    public String getCMD_CHECK() {
        return CMD_CHECK;
    }

    public String getFLAG_CHECK_BITS() {
        return FLAG_CHECK_BITS;
    }

    public String getARCHIVE_PATH() {
        return ARCHIVE_PATH;
    }
}
