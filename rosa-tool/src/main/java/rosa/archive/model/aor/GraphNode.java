package rosa.archive.model.aor;

/**
 * A node in a graph annotation.
 *
 * @param id      the node identifier
 * @param person  a person associated with this node
 * @param text    text label for this node
 * @param content the content within this node
 */
public record GraphNode(String id, String person, String text, String content) {

    /**
     * Creates a graph node with only an identifier.
     *
     * @param id the node identifier
     */
    public GraphNode(String id) {
        this(id, null, null, null);
    }
}
