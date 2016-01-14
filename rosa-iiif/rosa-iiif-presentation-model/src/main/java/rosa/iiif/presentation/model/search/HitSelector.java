package rosa.iiif.presentation.model.search;

import java.io.Serializable;

public class HitSelector implements Serializable {
    private static final long serialVersionUID = 1L;

    public final String matching;
    public final String before;
    public final String after;

    public HitSelector(String matching, String before, String after) {
        this.matching = matching;
        this.before = before;
        this.after = after;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        HitSelector that = (HitSelector) o;

        if (matching != null ? !matching.equals(that.matching) : that.matching != null) return false;
        if (before != null ? !before.equals(that.before) : that.before != null) return false;
        return !(after != null ? !after.equals(that.after) : that.after != null);

    }

    @Override
    public int hashCode() {
        int result = matching != null ? matching.hashCode() : 0;
        result = 31 * result + (before != null ? before.hashCode() : 0);
        result = 31 * result + (after != null ? after.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "HitSelecetor{" +
                "matching='" + matching + '\'' +
                ", before='" + before + '\'' +
                ", after='" + after + '\'' +
                '}';
    }
}
