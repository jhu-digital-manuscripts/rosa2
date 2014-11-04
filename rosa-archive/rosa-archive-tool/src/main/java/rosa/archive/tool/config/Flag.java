package rosa.archive.tool.config;

/**
 *
 */
public enum Flag {

    CHECK_BITS("checkBits"),
    SHOW_ERRORS("showErrors"),
    FORCE("force");

    private String display;

    public String display() {
        return display;
    }

    Flag(String display) {
        this.display = display;
    }
}
