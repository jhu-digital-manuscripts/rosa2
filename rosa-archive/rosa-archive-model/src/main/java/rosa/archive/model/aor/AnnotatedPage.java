package rosa.archive.model.aor;

import rosa.archive.model.HasId;

import java.util.List;

/**
 *
 */
public class AnnotatedPage implements HasId {

    private String id;
    private String reader;
    private String pagination;
    private String signature;
    private List<Annotation> annotations;

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    public String getReader() {
        return reader;
    }

    public void setReader(String reader) {
        this.reader = reader;
    }

    public String getPagination() {
        return pagination;
    }

    public void setPagination(String pagination) {
        this.pagination = pagination;
    }

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }

    public List<Annotation> getAnnotations() {
        return annotations;
    }

    public void setAnnotations(List<Annotation> annotations) {
        this.annotations = annotations;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        AnnotatedPage that = (AnnotatedPage) o;

        if (annotations != null ? !annotations.equals(that.annotations) : that.annotations != null) return false;
        if (id != null ? !id.equals(that.id) : that.id != null) return false;
        if (pagination != null ? !pagination.equals(that.pagination) : that.pagination != null) return false;
        if (reader != null ? !reader.equals(that.reader) : that.reader != null) return false;
        if (signature != null ? !signature.equals(that.signature) : that.signature != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (reader != null ? reader.hashCode() : 0);
        result = 31 * result + (pagination != null ? pagination.hashCode() : 0);
        result = 31 * result + (signature != null ? signature.hashCode() : 0);
        result = 31 * result + (annotations != null ? annotations.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "AnnotatedPage{" +
                "id='" + id + '\'' +
                ", reader='" + reader + '\'' +
                ", pagination='" + pagination + '\'' +
                ", signature='" + signature + '\'' +
                ", annotations=" + annotations +
                '}';
    }
}
