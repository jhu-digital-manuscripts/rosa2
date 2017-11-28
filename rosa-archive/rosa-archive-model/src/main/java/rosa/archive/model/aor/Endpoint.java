package rosa.archive.model.aor;

import java.io.Serializable;

/**
 * One end of a {@link Reference}. This end can point to an external URL, or an annotation
 * within the AOR corpus.
 *
 * Endpoints can specify URL that should be resolvable if {@link #isExternal} is TRUE. Either
 * internal or external links can use the {@link TextSelector} to further narrow the
 * intended object.
 *
 * An endpoint may not have a URL, in which case, assume the endpoint references the
 * object in which it is nested. Example:
 * marginalia:
 *      reference:
 *          source (endpoint): text=Ghost
 *          target (endpoint): url="..."
 * Here, a reference Source is an endpoint that points to the marginalia in which it is embedded.
 */
public class Endpoint implements Serializable {
    private static final long serialVersionUID = 1L;

    /** Does this endpoint point to an external link? (REQUIRED)*/
    private boolean isExternal;
    /** External URL or internal annotation ID (RECOMMENDED)*/
    private String url;
    /** Text selector, especially useful for linking to internal annotations (OPTIONAL)*/
    private TextSelector text;
    /** Human readable label (RECOMMENDED)*/
    private String label;
    /** Human readable long description (OPTIONAL) */
    private String description;

    /**
     * If an endpoint refers to an entire object, rather than some text in an object.
     * @param url .
     * @param isExternal .
     * @param label .
     */
    public Endpoint(String url, boolean isExternal, String label) {
        this(url, isExternal, label, null, null);
    }

    public Endpoint(String url, boolean isExternal, String label, String description) {
        this(url, isExternal, label, description, null);
    }

    public Endpoint(String url, boolean isExternal, String label, String description, TextSelector text) {
        this.isExternal = isExternal;
        this.url = url;
        this.label = label;
        this.description = description;
        this.text = text;
    }

    public boolean isExternal() {
        return isExternal;
    }

    public void setIsExternal(boolean link) {
        isExternal = link;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public TextSelector getTextSelector() {
        return text;
    }

    public void setTextSelector(TextSelector text) {
        this.text = text;
    }

    public String getText() {
        return text == null ? null : text.getText();
    }

    public String getTextPrefix() {
        return text == null ? null : text.getPrefix();
    }

    public String getTextSuffix() {
        return text == null ? null : text.getSuffix();
    }

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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Endpoint endpoint = (Endpoint) o;

        if (isExternal != endpoint.isExternal) return false;
        if (url != null ? !url.equals(endpoint.url) : endpoint.url != null) return false;
        if (text != null ? !text.equals(endpoint.text) : endpoint.text != null) return false;
        if (label != null ? !label.equals(endpoint.label) : endpoint.label != null) return false;
        return description != null ? description.equals(endpoint.description) : endpoint.description == null;
    }

    @Override
    public int hashCode() {
        int result = (isExternal ? 1 : 0);
        result = 31 * result + (url != null ? url.hashCode() : 0);
        result = 31 * result + (text != null ? text.hashCode() : 0);
        result = 31 * result + (label != null ? label.hashCode() : 0);
        result = 31 * result + (description != null ? description.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "Endpoint{" +
                "isExternal=" + isExternal +
                ", url='" + url + '\'' +
                ", text=" + text +
                ", label='" + label + '\'' +
                ", description='" + description + '\'' +
                '}';
    }
}
