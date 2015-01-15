package rosa.iiif.presentation.model;

import java.io.Serializable;

public class ExternalMedia implements Serializable {
    private static final long serialVersionUID = 1L;

    private String uri;
    private String format;

    public ExternalMedia() {}

    public ExternalMedia(String uri, String format) {
        this.uri = uri;
        this.format = format;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((format == null) ? 0 : format.hashCode());
        result = prime * result + ((uri == null) ? 0 : uri.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (!(obj instanceof ExternalMedia))
            return false;
        ExternalMedia other = (ExternalMedia) obj;
        if (format == null) {
            if (other.format != null)
                return false;
        } else if (!format.equals(other.format))
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
        return "ExternalMedia [uri=" + uri + ", format=" + format + "]";
    }
}
