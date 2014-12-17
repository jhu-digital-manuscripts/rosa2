package rosa.iiif.presentation.model.selector;

import rosa.iiif.presentation.model.IIIFNames;

import java.io.Serializable;
import java.util.Arrays;

public class SvgSelector implements Selector, Serializable {
    private static final long serialVersionUID = 1L;

    private SvgType type;
    private int[][] points;

    public SvgSelector() {}

    public SvgSelector(SvgType type, int[][] points) {
        this.type = type;
        this.points = points;
    }

    public int[][] getPoints() {
        return points;
    }

    public void setPoints(int[][] points) {
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
        if (type == SvgType.POLYGON || type == SvgType.PATH || type == SvgType.POLYLINE) {
            StringBuilder sb = new StringBuilder("<");
            sb.append(type.label());
            sb.append(" points=\"");
            for (int[] p : points) {
                int x = p[0];
                int y = p[1];

                sb.append(x);
                sb.append(',');
                sb.append(y);
                sb.append(' ');
            }
            sb.append("\"/>");

            return sb.toString();
        } else if (type == SvgType.RECT) {
            return "<" + type.label()
                    + " x=\"" + points[0][0]
                    + "\" y=\"" + points[0][1]
                    + "\" w=\"" + points[1][0]
                    + "\" h=\"" + points[1][1]
                    + "\"/>";
        } else if (type == SvgType.CIRCLE) {
            return "<" + type.label()
                    + " cx=\"" + points[0][0]
                    + "\" cy=\"" + points[0][1]
                    + "\" r=\"" + points[1][0]
                    + "\"/>";
        } else if (type == SvgType.ELLIPSE) {
            return "<" + type.label()
                    + " cx=\"" + points[0][0]
                    + "\" cy=\"" + points[0][1]
                    + "\" rx=\"" + points[1][0]
                    + "\" ry=\"" + points[1][1]
                    + "\"/>";
        } else {
            return "";
        }
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
