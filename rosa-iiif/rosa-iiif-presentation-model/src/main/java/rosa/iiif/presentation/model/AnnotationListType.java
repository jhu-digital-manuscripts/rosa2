package rosa.iiif.presentation.model;

public enum AnnotationListType {
    ALL,
    ILLUSTRATION,
    UNDERLINE,
    SYMBOL,
    MARGINALIA,
    NUMBERAL,
    ERRATA,
    MARK;

    public static AnnotationListType getType(String name) {
        for (AnnotationListType type : AnnotationListType.values()) {
            if (type.toString().equals(name.toUpperCase())) {
                return type;
            }
        }
        return null;
    }
}
