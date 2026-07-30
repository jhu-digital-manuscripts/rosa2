package rosa.archive.model.aor;

/**
 * A text element within a drawing, table, or other annotation.
 *
 * @param hand       the hand that wrote this text
 * @param language   the language code of this text
 * @param anchorText the anchor text in the printed book that this element references
 * @param text       the transcribed text content
 */
public record TextEl(String hand, String language, String anchorText, String text) {

    /**
     * Creates a text element with only hand and language.
     *
     * @param hand     the hand that wrote this text
     * @param language the language code
     */
    public TextEl(String hand, String language) {
        this(hand, language, null, null);
    }
}
