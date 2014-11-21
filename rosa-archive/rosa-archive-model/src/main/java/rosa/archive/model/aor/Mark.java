package rosa.archive.model.aor;

import java.io.Serializable;

/**
 *
 */
public class Mark extends Annotation implements Serializable {

    private String name;
    private String method;
    private String place;
    private String language;

    public Mark() {}

    public Mark(String referringText, String name, String method, String place, String language) {
        super(referringText);
        this.name = name;
        this.method = method;
        this.place = place;
        this.language = language;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getPlace() {
        return place;
    }

    public void setPlace(String place) {
        this.place = place;
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

        Mark mark = (Mark) o;

        if (language != null ? !language.equals(mark.language) : mark.language != null) return false;
        if (method != null ? !method.equals(mark.method) : mark.method != null) return false;
        if (name != null ? !name.equals(mark.name) : mark.name != null) return false;
        if (place != null ? !place.equals(mark.place) : mark.place != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + (method != null ? method.hashCode() : 0);
        result = 31 * result + (place != null ? place.hashCode() : 0);
        result = 31 * result + (language != null ? language.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "Mark{" +
                "name='" + name + '\'' +
                ", method='" + method + '\'' +
                ", place='" + place + '\'' +
                ", language='" + language + '\'' +
                '}';
    }
}
