package rosa.archive.model.aor;

/**
 *
 */
public class Underline extends Annotation {

    private String method;
    private String type;

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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        Underline underline = (Underline) o;

        if (method != null ? !method.equals(underline.method) : underline.method != null) return false;
        if (type != null ? !type.equals(underline.type) : underline.type != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (method != null ? method.hashCode() : 0);
        result = 31 * result + (type != null ? type.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "Underline{" +
                "method='" + method + '\'' +
                ", type='" + type + '\'' +
                '}';
    }
}
