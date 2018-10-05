package rosa.iiif.presentation.model;

/**
 * The type of data being requested by a client. Each request type can be tied
 * to a IIIF presentation object.
 */
public enum PresentationRequestType {
    MANIFEST("manifest"), SEQUENCE("sequence"), CANVAS("canvas"), ANNOTATION("annotation"), ANNOTATION_LIST("annotations"), RANGE(
            "range"), COLLECTION("collection"), LAYER("layer");

    private final String keyword;

    private PresentationRequestType(String keyword) {
        this.keyword = keyword;
    }

    public String getKeyword() {
        return keyword;
    }
}
