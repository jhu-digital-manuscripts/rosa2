package rosa.archive.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class MissingList implements HasId, Serializable {
    private static final long serialVersionUID = 1L;

    private String id;
    private List<String> missing;

    public MissingList() {
        this.missing = new ArrayList<>();
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    public List<String> getMissing() {
        return missing;
    }

    public void setMissing(List<String> missing) {
        this.missing = missing;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MissingList)) return false;

        MissingList that = (MissingList) o;

        if (id != null ? !id.equals(that.id) : that.id != null) return false;
        if (missing != null ? !missing.equals(that.missing) : that.missing != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (missing != null ? missing.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "MissingList{" +
                "id='" + id + '\'' +
                ", missing=" + missing +
                '}';
    }
}
