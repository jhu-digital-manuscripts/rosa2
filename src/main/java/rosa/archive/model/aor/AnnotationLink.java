package rosa.archive.model.aor;

/**
 * A link between annotations, used in graphs and physical links.
 *
 * @param nodeId       the node identifier for this link
 * @param source       the source annotation identifier
 * @param target       the target annotation identifier
 * @param relationship the type of relationship between source and target
 */
public record AnnotationLink(String nodeId, String source, String target, String relationship) {

    /**
     * Creates an annotation link with only source and target.
     *
     * @param source the source annotation identifier
     * @param target the target annotation identifier
     */
    public AnnotationLink(String source, String target) {
        this(null, source, target, null);
    }
}
