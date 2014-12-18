package rosa.iiif.presentation.model;

public enum PresentationRequestType {
    MANIFEST("manifest"), SEQUENCE("sequence"), CANVAS("canvas"), ANNOTATION("annotation"), ANNOTATION_LIST("list"), RANGE(
            "range"), LAYER("layer"), COLLECTION("collection"), CONTENT("res");

    private final String keyword;

    private PresentationRequestType(String keyword) {
        this.keyword = keyword;
    }

    public String getKeyword() {
        return keyword;
    }
}
