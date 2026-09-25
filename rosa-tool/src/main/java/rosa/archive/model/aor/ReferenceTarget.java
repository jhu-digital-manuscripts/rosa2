package rosa.archive.model.aor;

/**
 * A target within an internal reference, pointing to a specific location in the archive.
 *
 * @param targetId   the referenced object identifier
 * @param text       text being linked
 * @param textPrefix prefix of text for disambiguation
 * @param textSuffix suffix of text for disambiguation
 * @param filename   deprecated target filename
 * @param bookId     deprecated book identifier
 */
public record ReferenceTarget(String targetId, String text, String textPrefix, String textSuffix, String filename, String bookId) {

    /**
     * Creates a reference target with only a target ID and text.
     *
     * @param targetId the referenced object identifier
     * @param text     text being linked
     */
    public ReferenceTarget(String targetId, String text) {
        this(targetId, text, null, null, null, null);
    }

    /**
     * Creates a reference target with target ID, text, prefix, and suffix.
     *
     * @param targetId   the referenced object identifier
     * @param text       text being linked
     * @param textPrefix prefix for disambiguation
     * @param textSuffix suffix for disambiguation
     */
    public ReferenceTarget(String targetId, String text, String textPrefix, String textSuffix) {
        this(targetId, text, textPrefix, textSuffix, null, null);
    }
}
