package rosa.archive.tool.config;

/**
 *
 */
public enum Flag {

    CHECK_BITS("b", "checkBits"),
    FORCE("f", "force"),
    CHANGE_ID("I", "change-id"),
    REVERSE("R", "reverse");

    private String longName;
    private String shortName;

    public String longName() {
        return longName;
    }
    public String shortName() {
        return shortName;
    }

    Flag(String shortName, String longName) {
        this.longName = longName;
        this.shortName = shortName;
    }
}
