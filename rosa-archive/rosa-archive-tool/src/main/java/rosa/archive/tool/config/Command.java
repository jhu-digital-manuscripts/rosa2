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
    RENAME_FILES("rename-files"),    
    RENAME_TRANSCRIPTIONS("rename-transcriptions"),
    GENERATE_TEI("generate-tei"),
    CHECK_AOR("check-aor"),
    GENERATE_ANNOTATION_MAP("generate-annotation-map"),
    MIGRATE_TEI_METADATA("migrate-tei-metadata"),
    DECORATE_IMAGE_LIST("decorate-image-list");

    private String display;

    public String display() {
        return display;
    }

    Command(String display) {
        this.display = display;
    }

}
