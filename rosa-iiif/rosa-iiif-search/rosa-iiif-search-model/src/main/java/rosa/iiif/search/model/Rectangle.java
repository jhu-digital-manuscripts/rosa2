package rosa.iiif.search.model;

import java.io.Serializable;

/**
 * A rectangle defined by the top-left position and its width and height
 * (x,y,w,h)
 *
 * Intended to be in PX
 */
public class Rectangle implements Serializable {
    private static final long serialVersionUID = 1L;

    public final int x;
    public final int y;
    public final int width;
    public final int height;

    public Rectangle(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Rectangle rectangle = (Rectangle) o;

        if (x != rectangle.x) return false;
        if (y != rectangle.y) return false;
        if (width != rectangle.width) return false;
        return height == rectangle.height;

    }

    @Override
    public int hashCode() {
        int result = x;
        result = 31 * result + y;
        result = 31 * result + width;
        result = 31 * result + height;
        return result;
    }

    @Override
    public String toString() {
        return "Rectangle{" +
                "x=" + x +
                ", y=" + y +
                ", w=" + width +
                ", h=" + height +
                '}';
    }
}
