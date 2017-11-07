package rosa.archive.model.aor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Calculation extends Annotation implements Serializable {
    private static final long serialVersionUID = 1L;

    private String type;
    private int orientation;
    private String method;

    private List<String> data;

    public Calculation(String id, String type, int orientation, Location location) {
        this(id, type, orientation, location, null, null);
    }

    public Calculation(String id, String type, int orientation, Location location, String method, String internalRef) {
        super(id, null, null, location);
        setInternalRef(internalRef);
        this.type = type;
        this.orientation = orientation;
        this.method = method;
        this.data = new ArrayList<>();
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public int getOrientation() {
        return orientation;
    }

    public void setOrientation(int orientation) {
        this.orientation = orientation;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
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

        Calculation that = (Calculation) o;

        if (orientation != that.orientation) return false;
        if (type != null ? !type.equals(that.type) : that.type != null) return false;
        if (method != null ? !method.equals(that.method) : that.method != null) return false;
        return data != null ? data.equals(that.data) : that.data == null;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (type != null ? type.hashCode() : 0);
        result = 31 * result + orientation;
        result = 31 * result + (method != null ? method.hashCode() : 0);
        result = 31 * result + (data != null ? data.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "Calculation{" +
                "type='" + type + '\'' +
                ", orientation=" + orientation +
                ", method='" + method + '\'' +
                ", data=" + data +
                '}';
    }
}
