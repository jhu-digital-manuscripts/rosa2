package rosa.archive.model;

/**
 * Location of an image within the physical structure of a book.
 */
public enum BookImageLocation {
    /** Binding images (covers, spine). */
    BINDING("binding", "binding"),
    /** Front matter pages (flyleaves, title pages). */
    FRONT_MATTER("frontmatter", "front matter"),
    /** End matter pages (colophon, back flyleaves). */
    END_MATTER("endmatter", "end matter"),
    /** Body matter pages (the main text block). */
    BODY_MATTER("", ""),
    /** Miscellaneous images (inserts, loose leaves). */
    MISC("misc", "misc");

    private final String inArchiveName;
    private final String display;

    BookImageLocation(String inArchiveName, String display) {
        this.inArchiveName = inArchiveName;
        this.display = display;
    }

    /**
     * Returns the name used to identify this location in archive data files.
     *
     * @return the archive name
     */
    public String getInArchiveName() {
        return inArchiveName;
    }

    /**
     * Returns the human-readable display label for this location.
     *
     * @return the display label
     */
    public String getDisplay() {
        return display;
    }
}
