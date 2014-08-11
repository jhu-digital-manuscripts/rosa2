package rosa.archive.model;

import com.google.gwt.user.client.rpc.IsSerializable;

import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class MissingList implements IsSerializable {

    private List<String> missing;

    public MissingList() {
        this.missing = new ArrayList<>();
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

        if (missing != null ? !missing.equals(that.missing) : that.missing != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return missing != null ? missing.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "MissingList{" +
                "missing=" + missing +
                '}';
    }
}
