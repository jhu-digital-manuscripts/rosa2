package rosa.archive.model;

import java.util.Objects;

/**
 * Crop data for high-resolution images. All values are percentages representing
 * the amount to crop from each edge.
 */
public class CropData {

    private String id;
    private double left;
    private double right;
    private double top;
    private double bottom;

    /**
     * Creates an empty CropData with default values (-1.0).
     */
    public CropData() {
        left = -1.0;
        right = -1.0;
        top = -1.0;
        bottom = -1.0;
    }

    /**
     * Returns the page identifier for this crop data.
     *
     * @return the page id
     */
    public String getId() {
        return id;
    }

    /**
     * Sets the page identifier.
     *
     * @param id the page id
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Returns the left crop percentage.
     *
     * @return the left crop value
     */
    public double getLeft() {
        return left;
    }

    /**
     * Sets the left crop percentage.
     *
     * @param left the left crop value
     */
    public void setLeft(double left) {
        this.left = left;
    }

    /**
     * Returns the right crop percentage.
     *
     * @return the right crop value
     */
    public double getRight() {
        return right;
    }

    /**
     * Sets the right crop percentage.
     *
     * @param right the right crop value
     */
    public void setRight(double right) {
        this.right = right;
    }

    /**
     * Returns the top crop percentage.
     *
     * @return the top crop value
     */
    public double getTop() {
        return top;
    }

    /**
     * Sets the top crop percentage.
     *
     * @param top the top crop value
     */
    public void setTop(double top) {
        this.top = top;
    }

    /**
     * Returns the bottom crop percentage.
     *
     * @return the bottom crop value
     */
    public double getBottom() {
        return bottom;
    }

    /**
     * Sets the bottom crop percentage.
     *
     * @param bottom the bottom crop value
     */
    public void setBottom(double bottom) {
        this.bottom = bottom;
    }

    /**
     * Returns the crop values as an array: [left, right, top, bottom].
     *
     * @return the crop values array
     */
    public double[] asArray() {
        return new double[]{left, right, top, bottom};
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CropData that)) return false;
        return Double.compare(left, that.left) == 0 &&
                Double.compare(right, that.right) == 0 &&
                Double.compare(top, that.top) == 0 &&
                Double.compare(bottom, that.bottom) == 0 &&
                Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, left, right, top, bottom);
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
