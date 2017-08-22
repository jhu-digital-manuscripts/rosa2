package rosa.archive.model;

import java.io.Serializable;
import java.util.Arrays;

public class CollectionMetadata implements Serializable {
    private static final long serialVersionUID = 1L;

    private String label;
    private String description;
    private String logoUrl;

    private String[] parents;
    private String[] children;

    private String[] languages;

    public CollectionMetadata() {}

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getLogoUrl() {
        return logoUrl;
    }

    public void setLogoUrl(String logoUrl) {
        this.logoUrl = logoUrl;
    }

    public String[] getParents() {
        return parents;
    }

    public void setParents(String[] parents) {
        this.parents = parents;
    }

    public String[] getChildren() {
        return children;
    }

    public void setChildren(String[] children) {
        this.children = children;
    }

    public String[] getLanguages() {
        return languages;
    }

    public void setLanguages(String[] languages) {
        this.languages = languages;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        CollectionMetadata that = (CollectionMetadata) o;

        if (label != null ? !label.equals(that.label) : that.label != null) return false;
        if (description != null ? !description.equals(that.description) : that.description != null) return false;
        if (logoUrl != null ? !logoUrl.equals(that.logoUrl) : that.logoUrl != null) return false;
        if (!Arrays.equals(parents, that.parents)) return false;
        if (!Arrays.equals(languages, that.languages)) return false;
        return Arrays.equals(children, that.children);
    }

    @Override
    public int hashCode() {
        int result = label != null ? label.hashCode() : 0;
        result = 31 * result + (description != null ? description.hashCode() : 0);
        result = 31 * result + (logoUrl != null ? logoUrl.hashCode() : 0);
        result = 31 * result + Arrays.hashCode(parents);
        result = 31 * result + Arrays.hashCode(children);
        result = 31 * result + Arrays.hashCode(languages);
        return result;
    }
}
