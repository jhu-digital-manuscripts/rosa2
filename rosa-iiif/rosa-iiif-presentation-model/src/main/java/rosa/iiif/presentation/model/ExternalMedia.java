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
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ExternalMedia that = (ExternalMedia) o;

        if (format != null ? !format.equals(that.format) : that.format != null) return false;
        if (uri != null ? !uri.equals(that.uri) : that.uri != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = uri != null ? uri.hashCode() : 0;
        result = 31 * result + (format != null ? format.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "ExternalMedia{" +
                "uri='" + uri + '\'' +
                ", format='" + format + '\'' +
                '}';
    }
}
