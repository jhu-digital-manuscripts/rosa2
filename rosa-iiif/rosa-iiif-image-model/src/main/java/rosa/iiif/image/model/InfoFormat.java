package rosa.iiif.image.model;

public enum InfoFormat {
    JSON("json", "application/json"), JSON_LD("json", "application/ld+json");

    private String file_ext;
    private String mime_type;

    InfoFormat(String file_ext, String mime_type) {
        this.file_ext = file_ext;
        this.mime_type = mime_type;
    }

    public String getFileExtension() {
        return file_ext;
    }

    public String getMimeType() {
        return mime_type;
    }
}
