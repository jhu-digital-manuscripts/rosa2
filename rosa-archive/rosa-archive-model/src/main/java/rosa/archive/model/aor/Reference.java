package rosa.archive.model.aor;

import java.io.Serializable;

/**
 * A reference that points from one object to another.
 * Both source and target can refer to objects internal or external to the archive.
 *
 * <reference>
 *     <source external url label text prefix suffix >
 *         <description></description>
 *     </source>
 *     <target /> <!-- Repeat source tag -->
 * </reference>
 */
public class Reference extends Annotation implements Serializable {
    private static final long serialVersionUID = 1L;

    private Endpoint source;
    private Endpoint target;

    public Reference(Endpoint source, Endpoint target) {
        this.source = source;
        this.target = target;
    }

    public boolean isExternal() {
        return source.isExternal() && target.isExternal();
    }

    public Endpoint getSource() {
        return source;
    }

    public void setSource(Endpoint source) {
        this.source = source;
    }

    public Endpoint getTarget() {
        return target;
    }

    public void setTarget(Endpoint target) {
        this.target = target;
    }

    @Override
    public String toPrettyString() {
        return null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        Reference reference = (Reference) o;

        if (source != null ? !source.equals(reference.source) : reference.source != null) return false;
        return target != null ? target.equals(reference.target) : reference.target == null;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (source != null ? source.hashCode() : 0);
        result = 31 * result + (target != null ? target.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "Reference{" +
                super.toString() +
                ", source=" + source +
                ", target=" + target +
                '}';
    }
}
