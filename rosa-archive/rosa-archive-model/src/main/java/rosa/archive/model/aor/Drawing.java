package rosa.archive.model.aor;

import java.io.Serializable;

public class Drawing extends Annotation implements Serializable {
    private static final long serialVersionUID = 1L;

    private String name;
    private String method;
    private String color;

    public Drawing() {}

    public Drawing(String id, String referringText, Location location, String name, String method, String language) {
        super(id, referringText, language, location);
        this.name = name;
        this.method = method;
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

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    @Override
    public String toPrettyString() {
        return null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        Drawing drawing = (Drawing) o;

        if (name != null ? !name.equals(drawing.name) : drawing.name != null) return false;
        if (method != null ? !method.equals(drawing.method) : drawing.method != null) return false;
        return color != null ? color.equals(drawing.color) : drawing.color == null;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + (method != null ? method.hashCode() : 0);
        result = 31 * result + (color != null ? color.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "Drawing{" +
                "name='" + name + '\'' +
                ", method='" + method + '\'' +
                ", color='" + color + '\'' +
                super.toString() + '}';
    }
}
