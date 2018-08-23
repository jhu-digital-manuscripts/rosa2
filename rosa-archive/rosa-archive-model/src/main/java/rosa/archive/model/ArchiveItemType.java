package rosa.archive.model;

public enum ArchiveItemType {
    IMAGE("", "tif"),
    CROPPED_IMAGE_LIST("images.crop.csv", "csv"),
    IMAGE_LIST("images.csv", "csv"),
    CROPPING("crop", "txt"),
    DEPRECATED_DESCRIPTION("description_", "xml"),
    METADATA("description.xml", "xml"),
    FILE_MAP("filemap", "csv"),
    SHA1SUM("", "SHA1SUM"),
    NARRATIVE_TAGGING("nartag", "csv"),
    REDUCED_TAGGING_CSV("redtag", "csv"),
    REDUCED_TAGGING_TXT("redtag", "txt"),
    IMAGE_TAGGING("imagetag", "csv"),
    TRANSCRIPTION_ROSE("transcription", "xml"),
    TRANSCRIPTION_ROSE_TEXT("transcription", "txt"),
    TRANSCRIPTION_AOR("aor", "xml"),
    PERMISSION("permission_", "html");

    private String identifier;
    private String fileExtension;

    ArchiveItemType(String identifier, String fileExtension) {
        this.identifier = identifier;
        this.fileExtension = fileExtension;
    }

    public String getIdentifier() {
        return identifier;
    }

    public String getFileExtension() {
        return fileExtension;
    }
}
