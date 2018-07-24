package rosa.archive.tool.config;

/**
 *
 */
public enum Command {

    LIST("list"),
    CHECK("check"),
    UPDATE("update"),
    UPDATE_IMAGE_LIST("update-image-list"),
    CROP_IMAGES("crop-images"),
    FILE_MAP("file-map"),
    VALIDATE_XML("validate-xml"),
    RENAME_IMAGES("rename-images"),
    RENAME_TRANSCRIPTIONS("rename-transcriptions"),
    GENERATE_TEI("generate-tei"),
    CHECK_AOR("check-aor"),
    SEPARATE_TEI_METADATA("separate-tei-metadata"),
    GENERATE_ANNOTATION_MAP("generate-annotation-map");

    private String display;

    public String display() {
        return display;
    }

    Command(String display) {
        this.display = display;
    }

}
