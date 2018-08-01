package rosa.iiif.presentation.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Within represents a reference to parent objects as a tree.
 *
 * IIIF 'within' property can be:
 *   - A single string
 *   - A single object with properties: @id, @type, label, within
 *   - An array of strings
 *   - An array of objects
 *
 * Example in JSON:
 *   within: [
 *      { "@id": "some-object-id", "@type": "a-thing" },
 *      { "@id": "some-object-id", "@type": "a-thing", "label": "MooMoo" },
 *      {
 *          "@id": "some-object-id",
 *          "@type": "a-thing",
 *          "label": "MooToo",
 *          within: [
 *              "some-object-id",
 *              { "@id": "some-object-id", "@type": "a-thing" }
 *          ]
 *      }
 *   ]
 */
public class Within implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String id;
    private final String type;
    private final String label;
    private final List<Within> withins;

    public Within(String id) {
        this(id, null, null, (Within[]) null);
    }

    public Within(String id, String type, String label) {
        this(id, type, label, (Within[]) null);
    }

    public Within(String id, String type, String label, Within ... withins) {
        this.id = id;
        this.type = type;
        this.label = label;
        this.withins = new ArrayList<>();

        if (withins != null) {
            this.withins.addAll(Arrays.asList(withins));
        }
    }

    public boolean onlyId() {
        return type == null && label == null && (withins == null || withins.size() == 0);
    }

    public String getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    public String getLabel() {
        return label;
    }

    public List<Within> getWithins() {
        return withins;
    }

    public void addParentRef(Within ... within) {
        if (within != null) {
            this.withins.addAll(Arrays.asList(within));
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Within)) return false;

        Within within = (Within) o;

        if (id != null ? !id.equals(within.id) : within.id != null) return false;
        if (type != null ? !type.equals(within.type) : within.type != null) return false;
        if (label != null ? !label.equals(within.label) : within.label != null) return false;
        return withins != null ? withins.equals(within.withins) : within.withins == null;
    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (type != null ? type.hashCode() : 0);
        result = 31 * result + (label != null ? label.hashCode() : 0);
        result = 31 * result + (withins != null ? withins.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "Within{" + "id='" + id + '\'' + ", type='" + type + '\'' +
                ", label='" + label + '\'' + ", withins=" + withins + '}';
    }
}
