package rosa.iiif.image.model;

/**
 * http://iiif.io/api/image/2.0/compliance.html
 */

public enum ComplianceLevel {
    LEVEL_0("http://iiif.io/api/image/2/level0.json"), LEVEL_1("http://iiif.io/api/image/2/level1.json"), LEVEL_2(
            "http://iiif.io/api/image/2/level2.json");

    private String uri;

    private ComplianceLevel(String uri) {
        this.uri = uri;
    }

    public String getUri() {
        return uri;
    }
}
