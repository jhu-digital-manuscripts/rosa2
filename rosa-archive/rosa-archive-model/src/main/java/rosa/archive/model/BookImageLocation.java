package rosa.archive.model;

public enum BookImageLocation {
    BINDING("binding", "binding"),
    FRONT_MATTER("frontmatter", "front matter"),
    END_MATTER("endmatter", "end matter"),
    BODY_MATTER("", ""),
    MISC("misc", "misc");

    private String inArchiveName;
    private String display;

    BookImageLocation(String inArchiveName, String display) {
        this.inArchiveName = inArchiveName;
        this.display = display;
    }

    public String getInArchiveName() {
        return inArchiveName;
    }

    public String getDisplay() {
        return display;
    }
}
