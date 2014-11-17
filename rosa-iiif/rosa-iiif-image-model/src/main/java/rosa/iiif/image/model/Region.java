package rosa.iiif.image.model;

import java.io.Serializable;

/**
 * The region parameter defines the rectangular portion of the full image to be
 * returned.
 * 
 * A region is either the full image or a sub-rectangle specified by absolute or
 * percentage coordinates. Percentage x and y coordinates are specified as a
 * percentage of the image width and height respectively.
 * 
 * The top left of an image is considered position 0,0.
 */
public class Region implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private int x, y, width, height;
    private double px, py, pwidth, pheight;
    private RegionType type;

    public Region(RegionType type) {
        this.type = type;
    }

    public Region() {
    }

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public double getPercentageX() {
        return px;
    }

    public void setPercentageX(double px) {
        this.px = px;
    }

    public double getPercentageY() {
        return py;
    }

    public void setPercentageY(double py) {
        this.py = py;
    }

    public double getPercentageWidth() {
        return pwidth;
    }

    public void setPercentageWidth(double pwidth) {
        this.pwidth = pwidth;
    }

    public double getPercentageHeight() {
        return pheight;
    }

    public void setPercentageHeight(double pheight) {
        this.pheight = pheight;
    }

    public RegionType getRegionType() {
        return type;
    }

    public void setRegionType(RegionType type) {
        this.type = type;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + height;
        long temp;
        temp = Double.doubleToLongBits(pheight);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(pwidth);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(px);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(py);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        result = prime * result + ((type == null) ? 0 : type.hashCode());
        result = prime * result + width;
        result = prime * result + x;
        result = prime * result + y;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof Region)) {
            return false;
        }
        Region other = (Region) obj;
        if (height != other.height) {
            return false;
        }
        if (Double.doubleToLongBits(pheight) != Double.doubleToLongBits(other.pheight)) {
            return false;
        }
        if (Double.doubleToLongBits(pwidth) != Double.doubleToLongBits(other.pwidth)) {
            return false;
        }
        if (Double.doubleToLongBits(px) != Double.doubleToLongBits(other.px)) {
            return false;
        }
        if (Double.doubleToLongBits(py) != Double.doubleToLongBits(other.py)) {
            return false;
        }
        if (type != other.type) {
            return false;
        }
        if (width != other.width) {
            return false;
        }
        if (x != other.x) {
            return false;
        }
        if (y != other.y) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "Region [x=" + x + ", y=" + y + ", width=" + width + ", height=" + height + ", px=" + px + ", py=" + py
                + ", pwidth=" + pwidth + ", pheight=" + pheight + ", type=" + type + "]";
    }

}
