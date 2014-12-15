package rosa.iiif.presentation.model.selector;

import rosa.iiif.presentation.model.IIIFNames;

import java.io.Serializable;
import java.util.Arrays;

public class SvgSelector implements Selector, Serializable {
    private static final long serialVersionUID = 1L;

    private SvgType type;
    private int[] points;

    public SvgSelector() {}

    public SvgSelector(SvgType type, int ... points) {
        this.type = type;
        this.points = points;
    }

    public int[] getPoints() {
        return points;
    }

    public void setPoints(int[] points) {
        this.points = points;
    }

    public SvgType getType() {
        return type;
    }

    public void setType(SvgType type) {
        this.type = type;
    }

    @Override
    public String context() {
        return "";
    }

    @Override
    public String type() {
        return IIIFNames.OA_SVG_SELECTOR;
    }

    @Override
    public String content() {
        StringBuilder sb = new StringBuilder("<");
        sb.append(type.label());

        for (int i = 0; i < points.length; i++) {
            int p = points[i];
            sb.append(" p");
            sb.append(i);
            sb.append("=\"");
            sb.append(p);
            sb.append('"');
        }

        sb.append("/>");
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SvgSelector that = (SvgSelector) o;

        if (!Arrays.equals(points, that.points)) return false;
        if (type != that.type) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = type != null ? type.hashCode() : 0;
        result = 31 * result + (points != null ? Arrays.hashCode(points) : 0);
        return result;
    }

    @Override
    public String toString() {
        return "SvgSelector{" +
                "type=" + type +
                ", points=" + Arrays.toString(points) +
                '}';
    }
}
