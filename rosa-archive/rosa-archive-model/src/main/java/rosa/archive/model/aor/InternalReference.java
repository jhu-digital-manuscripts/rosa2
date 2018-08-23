package rosa.archive.model.aor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * &lt;internal_ref text="..." anchor_text="" anchor_prefix="" anchor_suffix&gt;
 *   &lt;target filename="" book_id="" text="" /&gt;
 * &lt;/internal_ref&gt;
 *
 * <h3>internal_ref</h3>
 * Attributes:
 * <ul>
 *   <li>text (optional) - source text</li>
 *   <li>anchor_text (optional) - text in printed book that this element references. Could be printed text or another annotation.</li>
 *   <li>anchor_prefix (optional) - prefix of anchor_text for disambiguation</li>
 *   <li>anchor_suffix (optional) - suffix of anchor_text for disambiguation</li>
 * </ul>
 * <p>
 * Contains elements:
 * <ul>
 * <li>target : list at least one {@link ReferenceTarget}</li>
 * </ul>
 *
 */
public class InternalReference implements Serializable {
    private static final long serialVersionUID = 1L;

    private String text;
    private String anchor;
    private String anchorPrefix;
    private String anchorSuffix;

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

    public String getAnchor() {
        return anchor;
    }

    public void setAnchor(String anchor) {
        this.anchor = anchor;
    }

    public String getAnchorPrefix() {
        return anchorPrefix;
    }

    public void setAnchorPrefix(String anchorPrefix) {
        this.anchorPrefix = anchorPrefix;
    }

    public String getAnchorSuffix() {
        return anchorSuffix;
    }

    public void setAnchorSuffix(String anchorSuffix) {
        this.anchorSuffix = anchorSuffix;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        InternalReference that = (InternalReference) o;

        if (text != null ? !text.equals(that.text) : that.text != null) return false;
        if (anchor != null ? !anchor.equals(that.anchor) : that.anchor != null) return false;
        if (anchorPrefix != null ? !anchorPrefix.equals(that.anchorPrefix) : that.anchorPrefix != null) return false;
        if (anchorSuffix != null ? !anchorSuffix.equals(that.anchorSuffix) : that.anchorSuffix != null) return false;
        return targets != null ? targets.equals(that.targets) : that.targets == null;
    }

    @Override
    public int hashCode() {
        int result = text != null ? text.hashCode() : 0;
        result = 31 * result + (anchor != null ? anchor.hashCode() : 0);
        result = 31 * result + (anchorPrefix != null ? anchorPrefix.hashCode() : 0);
        result = 31 * result + (anchorSuffix != null ? anchorSuffix.hashCode() : 0);
        result = 31 * result + (targets != null ? targets.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "InternalReference{" +
                "text='" + text + '\'' +
                ", anchor='" + anchor + '\'' +
                ", anchorPrefix='" + anchorPrefix + '\'' +
                ", anchorSuffix='" + anchorSuffix + '\'' +
                ", targets=" + targets +
                '}';
    }
}
