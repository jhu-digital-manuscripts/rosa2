package rosa.iiif.presentation.model.annotation;

import rosa.iiif.presentation.model.Service;
import rosa.iiif.presentation.model.selector.Selector;

import java.io.Serializable;

public class AnnotationSource implements Serializable {
    private static final long serialVersionUID = 1L;

    protected String uri;
    protected String type;
    protected String format;
    protected Service service;
    protected String embeddedText;
    protected String embeddedLanguage;
    protected String label;

    /**
     * Can be NULL if source is the full content of the URI, thus no selector
     * is needed.
     */
    protected Selector selector;

    public AnnotationSource() {}

    /**
     * Suggested for text based annotations.
     *
     * @param uri URI ID
     * @param type rdf type
     * @param format MIME type
     * @param embeddedText text content
     * @param embeddedLanguage content language
     */
    public AnnotationSource(String uri, String type, String format, String embeddedText, String embeddedLanguage) {
        this(uri, type, format);
        this.embeddedText = embeddedText;
        this.embeddedLanguage = embeddedLanguage;
    }

    /**
     * @param uri URI ID
     * @param type rdf type
     * @param format MIME type
     */
    public AnnotationSource(String uri, String type, String format) {
        this.uri = uri;
        this.type = type;
        this.format = format;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public Service getService() {
        return service;
    }

    public void setService(Service service) {
        this.service = service;
    }

    public boolean isSpecificResource() {
        return selector != null;
    }

    public Selector getSelector() {
        return selector;
    }

    public void setSelector(Selector selector) {
        this.selector = selector;
    }

    public String getEmbeddedText() {
        return embeddedText;
    }

    public void setEmbeddedText(String embeddedText) {
        this.embeddedText = embeddedText;
    }

    public String getEmbeddedLanguage() {
        return embeddedLanguage;
    }

    public void setEmbeddedLanguage(String embeddedLanguage) {
        this.embeddedLanguage = embeddedLanguage;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public boolean isImage() {
        // TODO
        return type.equals("dcterms:Image");
    }

    public boolean isEmbeddedText() {
        return embeddedText != null && !embeddedText.isEmpty();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        AnnotationSource that = (AnnotationSource) o;

        if (uri != null ? !uri.equals(that.uri) : that.uri != null) return false;
        if (type != null ? !type.equals(that.type) : that.type != null) return false;
        if (format != null ? !format.equals(that.format) : that.format != null) return false;
        if (service != null ? !service.equals(that.service) : that.service != null) return false;
        if (embeddedText != null ? !embeddedText.equals(that.embeddedText) : that.embeddedText != null) return false;
        if (embeddedLanguage != null ? !embeddedLanguage.equals(that.embeddedLanguage) : that.embeddedLanguage != null)
            return false;
        if (label != null ? !label.equals(that.label) : that.label != null) return false;
        return selector != null ? selector.equals(that.selector) : that.selector == null;
    }

    @Override
    public int hashCode() {
        int result = uri != null ? uri.hashCode() : 0;
        result = 31 * result + (type != null ? type.hashCode() : 0);
        result = 31 * result + (format != null ? format.hashCode() : 0);
        result = 31 * result + (service != null ? service.hashCode() : 0);
        result = 31 * result + (embeddedText != null ? embeddedText.hashCode() : 0);
        result = 31 * result + (embeddedLanguage != null ? embeddedLanguage.hashCode() : 0);
        result = 31 * result + (label != null ? label.hashCode() : 0);
        result = 31 * result + (selector != null ? selector.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "AnnotationSource{" +
                "uri='" + uri + '\'' +
                ", type='" + type + '\'' +
                ", format='" + format + '\'' +
                ", service=" + service +
                ", embeddedText='" + embeddedText + '\'' +
                ", embeddedLanguage='" + embeddedLanguage + '\'' +
                ", label='" + label + '\'' +
                ", selector=" + selector +
                '}';
    }
}
