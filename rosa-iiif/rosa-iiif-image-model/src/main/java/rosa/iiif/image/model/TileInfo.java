package rosa.iiif.image.model;

import java.io.Serializable;
import java.util.Arrays;

public class TileInfo implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private int width;
    private int height;
    private int[] scale_factors;

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

    public int[] getScaleFactors() {
        return scale_factors;
    }

    public void setScaleFactors(int... scale_factors) {
        this.scale_factors = scale_factors;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + height;
        result = prime * result + Arrays.hashCode(scale_factors);
        result = prime * result + width;
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
        if (!(obj instanceof TileInfo)) {
            return false;
        }
        TileInfo other = (TileInfo) obj;
        if (height != other.height) {
            return false;
        }
        if (!Arrays.equals(scale_factors, other.scale_factors)) {
            return false;
        }
        if (width != other.width) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "TileInfo [width=" + width + ", height=" + height + ", scale_factors=" + Arrays.toString(scale_factors)
                + "]";
    }
}
