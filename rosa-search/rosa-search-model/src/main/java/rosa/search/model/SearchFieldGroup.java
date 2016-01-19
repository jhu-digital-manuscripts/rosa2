package rosa.search.model;

/**
 * Search fields are collected into groups. There is one group for a book, an
 * image, and an annotation.
 * 
 * In addition, annotations are broken into more fine groups.
 */

public enum SearchFieldGroup {
    BOOK, IMAGE, ANNOTATION, ANNOTATION_SYMBOL, ANNOTATION_MARK, ANNOTATION_UNDERLINE, ANNOTATION_MARGINALIA, ANNOTATION_DRAWING, ANNOTATION_ERRATA, ANNOTATION_NUMERAL
}
