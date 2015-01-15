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

    public boolean isImage() {
        // TODO
        return type.equals("dcterms:Image");
    }

    public boolean isEmbeddedText() {
        return embeddedText != null && !embeddedText.isEmpty();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((embeddedLanguage == null) ? 0 : embeddedLanguage.hashCode());
        result = prime * result + ((embeddedText == null) ? 0 : embeddedText.hashCode());
        result = prime * result + ((format == null) ? 0 : format.hashCode());
        result = prime * result + ((selector == null) ? 0 : selector.hashCode());
        result = prime * result + ((service == null) ? 0 : service.hashCode());
        result = prime * result + ((type == null) ? 0 : type.hashCode());
        result = prime * result + ((uri == null) ? 0 : uri.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (!(obj instanceof AnnotationSource))
            return false;
        AnnotationSource other = (AnnotationSource) obj;
        if (embeddedLanguage == null) {
            if (other.embeddedLanguage != null)
                return false;
        } else if (!embeddedLanguage.equals(other.embeddedLanguage))
            return false;
        if (embeddedText == null) {
            if (other.embeddedText != null)
                return false;
        } else if (!embeddedText.equals(other.embeddedText))
            return false;
        if (format == null) {
            if (other.format != null)
                return false;
        } else if (!format.equals(other.format))
            return false;
        if (selector == null) {
            if (other.selector != null)
                return false;
        } else if (!selector.equals(other.selector))
            return false;
        if (service == null) {
            if (other.service != null)
                return false;
        } else if (!service.equals(other.service))
            return false;
        if (type == null) {
            if (other.type != null)
                return false;
        } else if (!type.equals(other.type))
            return false;
        if (uri == null) {
            if (other.uri != null)
                return false;
        } else if (!uri.equals(other.uri))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "AnnotationSource [uri=" + uri + ", type=" + type + ", format=" + format + ", service=" + service
                + ", embeddedText=" + embeddedText + ", embeddedLanguage=" + embeddedLanguage + ", selector="
                + selector + "]";
    }
}
