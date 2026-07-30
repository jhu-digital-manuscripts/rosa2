package rosa.archive.model.aor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * An internal reference within an annotation, linking to other targets in the archive.
 *
 * <p>Contains source text and anchor information, along with a list of
 * {@link ReferenceTarget} entries identifying the referenced locations.</p>
 */
public final class InternalReference {

    private String text;
    private String anchor;
    private String anchorPrefix;
    private String anchorSuffix;
    private List<ReferenceTarget> targets;

    /**
     * Creates an empty internal reference.
     */
    public InternalReference() {
        this.targets = new ArrayList<>();
    }

    /**
     * Creates an internal reference with text and targets.
     *
     * @param text    the source text
     * @param targets the reference targets
     */
    public InternalReference(String text, List<ReferenceTarget> targets) {
        this.text = text;
        this.targets = targets != null ? new ArrayList<>(targets) : new ArrayList<>();
    }

    /**
     * Returns the source text.
     *
     * @return the source text, or null
     */
    public String getText() {
        return text;
    }

    /**
     * Sets the source text.
     *
     * @param text the source text
     */
    public void setText(String text) {
        this.text = text;
    }

    /**
     * Returns the anchor text.
     *
     * @return the anchor text, or null
     */
    public String getAnchor() {
        return anchor;
    }

    /**
     * Sets the anchor text.
     *
     * @param anchor the anchor text
     */
    public void setAnchor(String anchor) {
        this.anchor = anchor;
    }

    /**
     * Returns the anchor prefix for disambiguation.
     *
     * @return the anchor prefix, or null
     */
    public String getAnchorPrefix() {
        return anchorPrefix;
    }

    /**
     * Sets the anchor prefix.
     *
     * @param anchorPrefix the anchor prefix
     */
    public void setAnchorPrefix(String anchorPrefix) {
        this.anchorPrefix = anchorPrefix;
    }

    /**
     * Returns the anchor suffix for disambiguation.
     *
     * @return the anchor suffix, or null
     */
    public String getAnchorSuffix() {
        return anchorSuffix;
    }

    /**
     * Sets the anchor suffix.
     *
     * @param anchorSuffix the anchor suffix
     */
    public void setAnchorSuffix(String anchorSuffix) {
        this.anchorSuffix = anchorSuffix;
    }

    /**
     * Returns the list of reference targets.
     *
     * @return the targets list (never null)
     */
    public List<ReferenceTarget> getTargets() {
        return targets;
    }

    /**
     * Sets the list of reference targets.
     *
     * @param targets the targets list
     */
    public void setTargets(List<ReferenceTarget> targets) {
        this.targets = targets;
    }

    /**
     * Adds one or more targets to this reference.
     *
     * @param targets the targets to add
     */
    public void addTargets(ReferenceTarget... targets) {
        if (targets != null) {
            this.targets.addAll(Arrays.asList(targets));
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof InternalReference that)) return false;
        return Objects.equals(text, that.text) &&
                Objects.equals(anchor, that.anchor) &&
                Objects.equals(anchorPrefix, that.anchorPrefix) &&
                Objects.equals(anchorSuffix, that.anchorSuffix) &&
                Objects.equals(targets, that.targets);
    }

    @Override
    public int hashCode() {
        return Objects.hash(text, anchor, anchorPrefix, anchorSuffix, targets);
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
