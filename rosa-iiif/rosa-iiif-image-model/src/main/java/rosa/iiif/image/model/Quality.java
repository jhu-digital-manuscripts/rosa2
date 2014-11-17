package rosa.iiif.image.model;

public enum Quality {
    DEFAULT("default"), COLOR("color"), GRAY("gray"), BITONAL("bitonal");

    private String keyword;

    private Quality(String s) {
        this.keyword = s;
    }

    public String getKeyword() {
        return keyword;
    }
}
