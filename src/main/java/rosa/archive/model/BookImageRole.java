package rosa.archive.model;

/**
 * The role or function of an image within the book structure.
 */
public enum BookImageRole {
    /** Front cover image. */
    FRONT_COVER("frontcover", "front cover"),
    /** Back cover image. */
    BACK_COVER("backcover", "back cover"),
    /** Pastedown image. */
    PASTEDOWN("pastedown", "pastedown"),
    /** Insert page image. */
    INSERT("insert", "insert"),
    /** Spine image. */
    SPINE("spine", "spine"),
    /** Gutter image. */
    GUTTER("gutter", "gutter"),
    /** Tail edge image. */
    TAIL("tail", "tail"),
    /** Head edge image. */
    HEAD("head", "head"),
    /** Color calibration bar image. */
    COLOR_BAR("colorbar", "color bar"),
    /** Miscellaneous image. */
    MISC("misc", "misc");

    private final String archiveName;
    private final String display;

    BookImageRole(String archiveName, String display) {
        this.archiveName = archiveName;
        this.display = display;
    }

    /**
     * Returns the name used to identify this role in archive data files.
     *
     * @return the archive name
     */
    public String getArchiveName() {
        return archiveName;
    }

    /**
     * Returns the human-readable display label for this role.
     *
     * @return the display label
     */
    public String getDisplay() {
        return display;
    }
}
