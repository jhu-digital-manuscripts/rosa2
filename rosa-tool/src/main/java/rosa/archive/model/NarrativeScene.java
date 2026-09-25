package rosa.archive.model;

import java.util.Objects;

/**
 * A narrative scene within a text, identified by line numbers in the critical edition
 * and relative line positions.
 */
public class NarrativeScene {

    private String id;
    private String description;
    private int criticalEditionStart;
    private int criticalEditionEnd;
    private int relLineStart;
    private int relLineEnd;

    /**
     * Creates an empty NarrativeScene.
     */
    public NarrativeScene() {}

    /**
     * Returns the scene identifier.
     *
     * @return the scene id
     */
    public String getId() {
        return id;
    }

    /**
     * Sets the scene identifier.
     *
     * @param id the scene id
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Returns the description of this scene.
     *
     * @return the description, or {@code null} if not set
     */
    public String getDescription() {
        return description;
    }

    /**
     * Sets the description of this scene.
     *
     * @param description the description
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Returns the start line number in the critical edition.
     *
     * @return the start line number
     */
    public int getCriticalEditionStart() {
        return criticalEditionStart;
    }

    /**
     * Sets the start line number in the critical edition.
     *
     * @param criticalEditionStart the start line number
     */
    public void setCriticalEditionStart(int criticalEditionStart) {
        this.criticalEditionStart = criticalEditionStart;
    }

    /**
     * Returns the end line number in the critical edition.
     *
     * @return the end line number
     */
    public int getCriticalEditionEnd() {
        return criticalEditionEnd;
    }

    /**
     * Sets the end line number in the critical edition.
     *
     * @param criticalEditionEnd the end line number
     */
    public void setCriticalEditionEnd(int criticalEditionEnd) {
        this.criticalEditionEnd = criticalEditionEnd;
    }

    /**
     * Returns the relative start line within the page.
     *
     * @return the relative start line
     */
    public int getRelLineStart() {
        return relLineStart;
    }

    /**
     * Sets the relative start line within the page.
     *
     * @param relLineStart the relative start line
     */
    public void setRelLineStart(int relLineStart) {
        this.relLineStart = relLineStart;
    }

    /**
     * Returns the relative end line within the page.
     *
     * @return the relative end line
     */
    public int getRelLineEnd() {
        return relLineEnd;
    }

    /**
     * Sets the relative end line within the page.
     *
     * @param relLineEnd the relative end line
     */
    public void setRelLineEnd(int relLineEnd) {
        this.relLineEnd = relLineEnd;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof NarrativeScene that)) return false;
        return criticalEditionStart == that.criticalEditionStart &&
                criticalEditionEnd == that.criticalEditionEnd &&
                relLineStart == that.relLineStart &&
                relLineEnd == that.relLineEnd &&
                Objects.equals(id, that.id) &&
                Objects.equals(description, that.description);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, description, criticalEditionStart, criticalEditionEnd,
                relLineStart, relLineEnd);
    }

    @Override
    public String toString() {
        return "NarrativeScene{" +
                "id='" + id + '\'' +
                ", description='" + description + '\'' +
                ", criticalEditionStart=" + criticalEditionStart +
                ", criticalEditionEnd=" + criticalEditionEnd +
                ", relLineStart=" + relLineStart +
                ", relLineEnd=" + relLineEnd +
                '}';
    }
}
