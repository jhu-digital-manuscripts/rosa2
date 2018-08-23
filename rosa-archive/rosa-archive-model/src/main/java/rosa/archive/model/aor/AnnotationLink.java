package rosa.archive.model.aor;

import java.io.Serializable;

public class AnnotationLink implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String nodeId;
    private final String source;
    private final String target;
    private final String relationship;

    public AnnotationLink(String source, String target) {
        this(null, source, target, null);
    }

    public AnnotationLink(String nodeId, String source, String target, String relationship) {
        this.nodeId = nodeId;
        this.source = source;
        this.target = target;
        this.relationship = relationship;
    }

    public String getNodeId() {
        return nodeId;
    }

    public String getSource() {
        return source;
    }

    public String getTarget() {
        return target;
    }

    public String getRelationship() {
        return relationship;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        AnnotationLink annotationLink = (AnnotationLink) o;

        if (nodeId != null ? !nodeId.equals(annotationLink.nodeId) : annotationLink.nodeId != null) return false;
        if (source != null ? !source.equals(annotationLink.source) : annotationLink.source != null) return false;
        if (target != null ? !target.equals(annotationLink.target) : annotationLink.target != null) return false;
        return relationship != null ? relationship.equals(annotationLink.relationship) : annotationLink.relationship == null;
    }

    @Override
    public int hashCode() {
        int result = nodeId != null ? nodeId.hashCode() : 0;
        result = 31 * result + (source != null ? source.hashCode() : 0);
        result = 31 * result + (target != null ? target.hashCode() : 0);
        result = 31 * result + (relationship != null ? relationship.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "GraphLink{" +
                "nodeId='" + nodeId + '\'' +
                ", source='" + source + '\'' +
                ", target='" + target + '\'' +
                ", relationship='" + relationship + '\'' +
                '}';
    }
}
