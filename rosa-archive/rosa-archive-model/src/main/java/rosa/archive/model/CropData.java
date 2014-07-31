package rosa.archive.model;

import com.google.gwt.user.client.rpc.IsSerializable;

/**
 *
 */
public class CropData implements IsSerializable {

    private String id;
    private double left;
    private double right;
    private double top;
    private double bottom;

    public CropData() {  }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public double getLeft() {
        return left;
    }

    public void setLeft(double left) {
        this.left = left;
    }

    public double getRight() {
        return right;
    }

    public void setRight(double right) {
        this.right = right;
    }

    public double getTop() {
        return top;
    }

    public void setTop(double top) {
        this.top = top;
    }

    public double getBottom() {
        return bottom;
    }

    public void setBottom(double bottom) {
        this.bottom = bottom;
    }

    public double[] asArray() {
        return new double[] {left, right, top, bottom};
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CropData)) return false;

        CropData cropData = (CropData) o;

        if (Double.compare(cropData.left, left) != 0) return false;
        if (Double.compare(cropData.right, right) != 0) return false;
        if (Double.compare(cropData.top, top) != 0) return false;
        if (Double.compare(cropData.bottom, bottom) != 0) return false;
        if (id != null ? !id.equals(cropData.id) : cropData.id != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result;
        long temp;
        result = id != null ? id.hashCode() : 0;
        temp = Double.doubleToLongBits(left);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(right);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(top);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(bottom);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        return result;
    }

    @Override
    public String toString() {
        return "CropData{" +
                "id='" + id + '\'' +
                ", left=" + left +
                ", right=" + right +
                ", top=" + top +
                ", bottom=" + bottom +
                '}';
    }
}
