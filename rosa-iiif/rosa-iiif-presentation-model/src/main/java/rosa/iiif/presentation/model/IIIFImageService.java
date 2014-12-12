package rosa.iiif.presentation.model;

import java.io.Serializable;
import java.util.Arrays;

public class IIIFImageService extends Service implements Serializable {
    private static final long serialVersionUID = 1L;

    private int width;
    private int height;
    private int tileWidth;
    private int tileHeight;

    private int[] scaleFactors;

    public IIIFImageService() {
        super();
        scaleFactors = new int[0];
    }

    public IIIFImageService(String context, String id, String profile, int width, int height,
                            int tileWidth, int tileHeight, int[] scaleFactors) {
        super(context, id, profile);
        this.width = width;
        this.height = height;
        this.tileWidth = tileWidth;
        this.tileHeight = tileHeight;
        this.scaleFactors = scaleFactors;
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

    public int getTileWidth() {
        return tileWidth;
    }

    public void setTileWidth(int tileWidth) {
        this.tileWidth = tileWidth;
    }

    public int getTileHeight() {
        return tileHeight;
    }

    public void setTileHeight(int tileHeight) {
        this.tileHeight = tileHeight;
    }

    public int[] getScaleFactors() {
        return scaleFactors;
    }

    public void setScaleFactors(int[] scaleFactors) {
        this.scaleFactors = scaleFactors;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        IIIFImageService that = (IIIFImageService) o;

        if (height != that.height) return false;
        if (tileHeight != that.tileHeight) return false;
        if (tileWidth != that.tileWidth) return false;
        if (width != that.width) return false;
        if (!Arrays.equals(scaleFactors, that.scaleFactors)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + width;
        result = 31 * result + height;
        result = 31 * result + tileWidth;
        result = 31 * result + tileHeight;
        result = 31 * result + (scaleFactors != null ? Arrays.hashCode(scaleFactors) : 0);
        return result;
    }

    @Override
    public String toString() {
        return "IIIFImageService{" +
                super.toString() +
                "width=" + width +
                ", height=" + height +
                ", tileWidth=" + tileWidth +
                ", tileHeight=" + tileHeight +
                ", scaleFactors=" + Arrays.toString(scaleFactors) +
                '}';
    }
}
