package rosa.iiif.image.model;

public enum ImageFormat {
    JPG("jpg", "image/jpeg"), TIF(".tif", "image/tiff"), PNG(".png", "image/png"), GIF(".gif", "image/gif"), 
        JP2(".jp2", "image/jp2"), PDF(".pdf", "application/pdf"), WEBP("webp", "image/webp");

    private String file_ext;
    private String mime_type;

    ImageFormat(String file_ext, String mime_type) {
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
