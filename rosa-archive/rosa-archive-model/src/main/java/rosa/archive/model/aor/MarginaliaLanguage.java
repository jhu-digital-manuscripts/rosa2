package rosa.archive.model.aor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class MarginaliaLanguage implements Serializable {
    private static final long serialVersionUID = 1L;

    String lang;
    List<Position> positions;

    public MarginaliaLanguage() {
        positions = new ArrayList<>();
    }

    public String getLang() {
        return lang;
    }

    public void setLang(String lang) {
        this.lang = lang;
    }

    public List<Position> getPositions() {
        return positions;
    }

    public void setPositions(List<Position> positions) {
        this.positions = positions;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        MarginaliaLanguage that = (MarginaliaLanguage) o;

        if (lang != null ? !lang.equals(that.lang) : that.lang != null) return false;
        if (positions != null ? !positions.equals(that.positions) : that.positions != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = lang != null ? lang.hashCode() : 0;
        result = 31 * result + (positions != null ? positions.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "MaginaliaLanguage{" +
                "lang='" + lang + '\'' +
                ", positions=" + positions +
                '}';
    }
}
