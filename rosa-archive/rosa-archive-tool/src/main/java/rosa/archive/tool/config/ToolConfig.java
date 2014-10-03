package rosa.archive.tool.config;

import com.google.inject.Inject;
import com.google.inject.name.Named;

/**
 *
 */
public class ToolConfig {

    @Inject @Named("archive.path")
    private String archivePath;

    @Inject @Named("CMD_LIST")
    private String cmdList;

    @Inject @Named("CMD_CHECK")
    private String cmdCheck;

    @Inject @Named("CMD_UPDATE")
    private String cmdUpdate;

    @Inject @Named("FLAG_CHECK_BITS")
    private String flagCheckBits;

    @Inject @Named("FLAG_SHOW_ERRORS")
    private String flagShowErrors;

// -----------------------------------------------------------------------------------
// ------ Getters --------------------------------------------------------------------
// -----------------------------------------------------------------------------------


    public String getCmdList() {
        return cmdList;
    }

    public String getCmdCheck() {
        return cmdCheck;
    }

    public String getFlagCheckBits() {
        return flagCheckBits;
    }

    public String getArchivePath() {
        return archivePath;
    }

    public void setArchivePath(String archivePath) {
        this.archivePath = archivePath;
    }

    public String getFlagShowErrors() {
        return flagShowErrors;
    }

    public String getCmdUpdate() {
        return cmdUpdate;
    }
}
