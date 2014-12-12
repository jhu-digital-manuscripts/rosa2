package rosa.archive.model.aor;

import java.io.Serializable;

/**
 *
 */
public class Underline extends Annotation implements Serializable {

    private String method;
    private String type;
    private String language;

    public Underline() {}

    @Override
    public String toPrettyString() {
        return null;
    }

    public Underline(String referringText, String method, String type, String language) {
        super(referringText, Location.INTEXT);
        this.method = method;
        this.type = type;
        this.language = language;
    }

    public Underline(String referringText, String method, String type, String language, Location location) {
        super(referringText, location);
        this.method = method;
        this.type = type;
        this.language = language;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        Underline underline = (Underline) o;

        if (language != null ? !language.equals(underline.language) : underline.language != null) return false;
        if (method != null ? !method.equals(underline.method) : underline.method != null) return false;
        if (type != null ? !type.equals(underline.type) : underline.type != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (method != null ? method.hashCode() : 0);
        result = 31 * result + (type != null ? type.hashCode() : 0);
        result = 31 * result + (language != null ? language.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "Underline{" +
                "method='" + method + '\'' +
                ", type='" + type + '\'' +
                ", language='" + language + '\'' +
                ", text='" + getReferringText() + '\'' +
                '}';
    }
}
