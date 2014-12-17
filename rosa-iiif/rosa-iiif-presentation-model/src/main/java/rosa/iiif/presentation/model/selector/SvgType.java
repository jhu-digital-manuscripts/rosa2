package rosa.iiif.presentation.model.selector;

public enum SvgType {
    PATH("path"),
    RECT("rect"),
    CIRCLE("circle"),
    ELLIPSE("ellipse"),
    POLYLINE("polyline"),
    POLYGON("polygon"),
    GROUP("g");

    private String label;

    SvgType(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    public SvgType fromString(String str) {
        for (SvgType t : SvgType.values()) {
            if (t.label.equals(str)) {
                return t;
            }
        }
        return null;
    }
}
