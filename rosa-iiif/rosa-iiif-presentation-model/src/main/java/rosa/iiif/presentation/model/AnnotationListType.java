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

    /**
     * @param name name to parse
     * @return the annotation list type associated with the name
     */
    public static AnnotationListType getType(String name) {
        for (AnnotationListType type : AnnotationListType.values()) {
            if (type.toString().equals(name.toUpperCase())) {
                return type;
            }
        }
        return null;
    }
}
