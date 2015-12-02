package rosa.archive.model.aor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * &lt;internal_ref text="..."&gt;
 *   &lt;target filename="" book_id="" text="" /&gt;
 * &lt;/internal_ref&gt;
 *
 * <h3>internal_ref</h3>
 * <p>
 * Attributes:
 * <ul>
 * <li>text (required) - source text</li>
 * </ul>
 * </p>
 * <p>
 * Contains elements:
 * <ul>
 * <li>target : list at least one {@link ReferenceTarget}</li>
 * </ul>
 * </p>
 *
 */
public class InternalReference implements Serializable {
    private static final long serialVersionUID = 1L;

    private String text;
    private List<ReferenceTarget> targets;

    public InternalReference() {
        this.targets = new ArrayList<>();
    }

    public InternalReference(String text, List<ReferenceTarget> targets) {
        this.text = text;
        this.targets = targets;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public List<ReferenceTarget> getTargets() {
        return targets;
    }

    public void setTargets(List<ReferenceTarget> targets) {
        this.targets = targets;
    }

    public void addTargets(ReferenceTarget ... targets) {
        if (targets != null) {
            this.targets.addAll(Arrays.asList(targets));
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        InternalReference that = (InternalReference) o;

        if (text != null ? !text.equals(that.text) : that.text != null) return false;
        return !(targets != null ? !targets.equals(that.targets) : that.targets != null);

    }

    @Override
    public int hashCode() {
        int result = text != null ? text.hashCode() : 0;
        result = 31 * result + (targets != null ? targets.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "InternalReference{" +
                "text='" + text + '\'' +
                ", targets=" + targets +
                '}';
    }
}
