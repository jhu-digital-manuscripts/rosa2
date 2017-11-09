package rosa.archive.model.aor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * &lt;physical_link&gt;
 *   &lt;marginal_annotation idref /&gt;
 *   &lt;relation from to type /&gt;
 * &lt;/physical_link&gt;
 *
 * No attributes.
 *
 * Contains elements:
 * <ul>
 *   <li>marginal_annotation : has attribute 'idref' that identifies an annotation involved in this link</li>
 *   <li>relation : has attributes 'to', 'from', 'type' describing links between annotations</li>
 * </ul>
 *
 * This is represented as simply a list of links. The ids involved in these links can be
 * inferred from the source and target of each link.
 * It is possible for annotations involved in a link to be on different pages.
 */
public class PhysicalLink extends Annotation implements Serializable {
    private static final long serialVersionUID = 1L;

    private final List<AnnotationLink> links;

    public PhysicalLink() {
        links = new ArrayList<>();
    }

    public List<AnnotationLink> getLinks() {
        return links;
    }

    /**
     * @return a list of all IDs involved in this link
     */
    public Set<String> getAllIds() {
        Set<String> ids = new HashSet<>();

        links.parallelStream().forEach(link -> {
            if (link.getSource() != null) {
                ids.add(link.getSource());
            }
            if (link.getTarget() != null) {
                ids.add(link.getTarget());
            }
        });

        return ids;
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

        PhysicalLink that = (PhysicalLink) o;

        return links != null ? links.equals(that.links) : that.links == null;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (links != null ? links.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "PhysicalLink{" +
                "links=" + links +
                '}';
    }
}
