package rosa.archive.model.aor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Calculation extends Annotation implements Serializable {
    private static final long serialVersionUID = 1L;

    private String type;
    private int orientation;
    private String method;
    private String content;

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

    public List<String> getData() {
        return data;
    }

    public void setData(List<String> data) {
        this.data = data;
    }

    public void addData(String moo) {
        if (this.data != null) {
            this.data.add(moo);
        }
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    @Override
    public String toPrettyString() {
        StringBuilder d = new StringBuilder();
        data.forEach(d::append);
        return (content != null ? "<p>" + content + "</p>" : "" ) +
                (!d.toString().isEmpty() ? "<p>" + d.toString() + "</p>" : "");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        Calculation that = (Calculation) o;
        return orientation == that.orientation &&
                Objects.equals(type, that.type) &&
                Objects.equals(method, that.method) &&
                Objects.equals(content, that.content) &&
                Objects.equals(data, that.data);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), type, orientation, method, content, data);
    }

    @Override
    public String toString() {
        return "Calculation{" +
                "type='" + type + '\'' +
                ", orientation=" + orientation +
                ", method='" + method + '\'' +
                ", content='" + content + '\'' +
                ", data=" + data +
                '}';
    }
}
