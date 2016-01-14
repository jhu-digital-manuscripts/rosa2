package rosa.iiif.presentation.model.search;

import java.io.Serializable;
import java.util.Arrays;

public class IIIFSearchHit implements Serializable {
    private final static long serialVersionUID = 1L;
    private final static String type = "search:Hit";

    public final String[] annotations;

    // These three fields provide context for this hit. An interface can highlight
    // the 'matching' string while providing a coherent context.
    public final String matching;
    public final String before;
    public final String after;

    public IIIFSearchHit(String[] annotations, String matching, String before, String after) {
        this.annotations = annotations;
        this.matching = matching;
        this.before = before;
        this.after = after;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        IIIFSearchHit that = (IIIFSearchHit) o;

        if (!Arrays.deepEquals(annotations, that.annotations)) return false;
        if (matching != null ? !matching.equals(that.matching) : that.matching != null) return false;
        if (before != null ? !before.equals(that.before) : that.before != null) return false;
        return !(after != null ? !after.equals(that.after) : that.after != null);

    }

    @Override
    public int hashCode() {
        int result = annotations != null ? Arrays.deepHashCode(annotations) : 0;
        result = 31 * result + (matching != null ? matching.hashCode() : 0);
        result = 31 * result + (before != null ? before.hashCode() : 0);
        result = 31 * result + (after != null ? after.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "IIIFSearchHit{" +
                "annotations=" + Arrays.toString(annotations) +
                ", matching='" + matching + '\'' +
                ", before='" + before + '\'' +
                ", after='" + after + '\'' +
                '}';
    }
}
