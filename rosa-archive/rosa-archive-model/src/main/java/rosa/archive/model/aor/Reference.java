package rosa.archive.model.aor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

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

    private List<Endpoint> sources;
    private List<Endpoint> targets;

    public Reference() {
        this(null, null);
    }

    public Reference(Endpoint source, Endpoint target) {
        sources = new ArrayList<>();
        targets = new ArrayList<>();

        if (source != null) {
            sources.add(source);
        }
        if (target != null) {
            targets.add(target);
        }
    }

    public List<Endpoint> getSources() {
        return sources;
    }

    public void setSources(List<Endpoint> sources) {
        this.sources = sources;
    }

    public List<Endpoint> getTargets() {
        return targets;
    }

    public void setTargets(List<Endpoint> targets) {
        this.targets = targets;
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

        if (sources != null ? !sources.equals(reference.sources) : reference.sources != null) return false;
        return targets != null ? targets.equals(reference.targets) : reference.targets == null;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (sources != null ? sources.hashCode() : 0);
        result = 31 * result + (targets != null ? targets.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "Reference{" +
                "sources=" + sources +
                ", targets=" + targets +
                '}';
    }
}
