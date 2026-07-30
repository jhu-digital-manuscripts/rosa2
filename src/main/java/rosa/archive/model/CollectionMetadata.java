package rosa.archive.model;

import java.util.Arrays;
import java.util.Objects;

/**
 * Metadata describing a book collection, including its label, description,
 * supported languages, and relationships to parent/child collections.
 */
public class CollectionMetadata {

    private String label;
    private String description;
    private String logoUrl;
    private String[] parents;
    private String[] children;
    private String[] languages;

    /**
     * Creates an empty CollectionMetadata with empty arrays.
     */
    public CollectionMetadata() {
        this.parents = new String[0];
        this.children = new String[0];
        this.languages = new String[0];
    }

    /**
     * Returns the display label for this collection.
     *
     * @return the label, or {@code null} if not set
     */
    public String getLabel() {
        return label;
    }

    /**
     * Sets the display label.
     *
     * @param label the label
     */
    public void setLabel(String label) {
        this.label = label;
    }

    /**
     * Returns the description of this collection.
     *
     * @return the description, or {@code null} if not set
     */
    public String getDescription() {
        return description;
    }

    /**
     * Sets the description.
     *
     * @param description the description
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Returns the logo URL for this collection.
     *
     * @return the logo URL, or {@code null} if not set
     */
    public String getLogoUrl() {
        return logoUrl;
    }

    /**
     * Sets the logo URL.
     *
     * @param logoUrl the logo URL
     */
    public void setLogoUrl(String logoUrl) {
        this.logoUrl = logoUrl;
    }

    /**
     * Returns the parent collection identifiers.
     *
     * @return the parent ids array
     */
    public String[] getParents() {
        return parents;
    }

    /**
     * Sets the parent collection identifiers.
     *
     * @param parents the parent ids
     */
    public void setParents(String[] parents) {
        this.parents = parents;
    }

    /**
     * Returns the child collection identifiers.
     *
     * @return the child ids array
     */
    public String[] getChildren() {
        return children;
    }

    /**
     * Sets the child collection identifiers.
     *
     * @param children the child ids
     */
    public void setChildren(String[] children) {
        this.children = children;
    }

    /**
     * Returns the supported language codes for this collection.
     *
     * @return the language codes array
     */
    public String[] getLanguages() {
        return languages;
    }

    /**
     * Sets the supported language codes.
     *
     * @param languages the language codes
     */
    public void setLanguages(String[] languages) {
        this.languages = languages;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CollectionMetadata that)) return false;
        return Objects.equals(label, that.label) &&
                Objects.equals(description, that.description) &&
                Objects.equals(logoUrl, that.logoUrl) &&
                Arrays.equals(parents, that.parents) &&
                Arrays.equals(children, that.children) &&
                Arrays.equals(languages, that.languages);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(label, description, logoUrl);
        result = 31 * result + Arrays.hashCode(parents);
        result = 31 * result + Arrays.hashCode(children);
        result = 31 * result + Arrays.hashCode(languages);
        return result;
    }

    @Override
    public String toString() {
        return "CollectionMetadata{" +
                "label='" + label + '\'' +
                ", description='" + description + '\'' +
                ", logoUrl='" + logoUrl + '\'' +
                ", parents=" + Arrays.toString(parents) +
                ", children=" + Arrays.toString(children) +
                ", languages=" + Arrays.toString(languages) +
                '}';
    }
}
