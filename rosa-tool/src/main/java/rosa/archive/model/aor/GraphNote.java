package rosa.archive.model.aor;

/**
 * A note within a graph text element.
 *
 * @param id           the note identifier
 * @param hand         the hand that wrote this note
 * @param language     the language code
 * @param internalLink an internal link reference
 * @param anchorText   the anchor text in the printed book
 * @param content      the note text content
 */
public record GraphNote(String id, String hand, String language, String internalLink, String anchorText, String content) {

    /**
     * Creates a graph note without internal link or anchor text.
     *
     * @param id       the note identifier
     * @param hand     the hand that wrote this note
     * @param language the language code
     * @param content  the note text content
     */
    public GraphNote(String id, String hand, String language, String content) {
        this(id, hand, language, null, null, content);
    }
}
