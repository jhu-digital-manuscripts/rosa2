package rosa.archive.model.aor;

/**
 * Physical location of an annotation on a page.
 */
public enum Location {
    /** At the top of the page. */
    HEAD,
    /** At the bottom of the page. */
    TAIL,
    /** In the left margin. */
    LEFT_MARGIN,
    /** In the right margin. */
    RIGHT_MARGIN,
    /** Inline with the printed text. */
    INTEXT,
    /** Spanning the full page. */
    FULL_PAGE
}
