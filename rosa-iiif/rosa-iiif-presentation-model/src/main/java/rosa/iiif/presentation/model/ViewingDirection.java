package rosa.iiif.presentation.model;

/**
 * Direction for reading or navigation. Canvas or other content resources
 * cannot have a viewing direction, but other resources may.
 */
public enum  ViewingDirection {

    LEFT_TO_RIGHT("left-to-right"),
    RIGHT_TO_LEFT("right-to-left"),
    TOP_TO_BOTTOM("top-to-bottom"),
    BOTTOM_TO_TOP("bottom-to-top");

    private String keyword;

    private ViewingDirection(String keyword) {
        this.keyword = keyword;
    }

    public String getKeyword() {
        return keyword;
    }

}
