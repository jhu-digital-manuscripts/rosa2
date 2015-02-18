package rosa.archive.model;

public enum BookImageRole {
    // Matters
    FRONT_COVER("frontcover", "front cover"),
    BACK_COVER("backcover", "back cover"),
    PASTEDOWN("pastedown", "pastedown"),
    // Bindings
    SPINE("spine", "spine"),
    GUTTER("gutter", "gutter"),
    TAIL("tail", "tail"),
    HEAD("head", "head"),
    // Misc
    COLOR_BAR("colorbar", "color bar"),
    MISC("misc", "misc");

    private String archiveName;
    private String display;

    BookImageRole(String archiveName, String display) {
        this.archiveName = archiveName;
        this.display = display;
    }

    public String getArchiveName() {
        return archiveName;
    }

    public String getDisplay() {
        return display;
    }
}
