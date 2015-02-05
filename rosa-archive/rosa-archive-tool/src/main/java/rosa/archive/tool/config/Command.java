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
    RENAME_IMAGES("rename-images");

    private String display;

    public String display() {
        return display;
    }

    Command(String display) {
        this.display = display;
    }

}
