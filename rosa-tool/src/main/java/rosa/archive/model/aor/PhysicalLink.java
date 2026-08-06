package rosa.archive.model.aor;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * An annotation representing a physical link between other annotations,
 * possibly across different pages.
 */
public final class PhysicalLink extends AbstractAnnotation {

    private final List<AnnotationLink> links;

    /**
     * Creates an empty physical link annotation.
     */
    public PhysicalLink() {
        links = new ArrayList<>();
    }

    /**
     * Returns the list of annotation links.
     *
     * @return the links (never null)
     */
    public List<AnnotationLink> getLinks() {
        return links;
    }

    /**
     * Returns all annotation IDs involved in this physical link.
     *
     * @return a set of all source and target IDs
     */
    public Set<String> getAllIds() {
        Set<String> ids = new HashSet<>();
        links.forEach(link -> {
            if (link.source() != null) {
                ids.add(link.source());
            }
            if (link.target() != null) {
                ids.add(link.target());
            }
        });
        return ids;
    }

    @Override
    public String toPrettyString() {
        return "PhysicalLink{links=" + links.size() + "}";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PhysicalLink that)) return false;
        if (!super.equals(o)) return false;
        return Objects.equals(links, that.links);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), links);
    }

    @Override
    public String toString() {
        return "PhysicalLink{links=" + links + '}';
    }
}
