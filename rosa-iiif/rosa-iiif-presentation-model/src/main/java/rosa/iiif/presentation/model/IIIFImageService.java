package rosa.iiif.presentation.model;

import java.util.Arrays;

public class IIIFImageService extends Service {
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
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + height;
        result = prime * result + Arrays.hashCode(scaleFactors);
        result = prime * result + tileHeight;
        result = prime * result + tileWidth;
        result = prime * result + width;
        return result;
    }
    
    @Override
    protected boolean canEqual(Object obj) {
        return obj instanceof IIIFImageService;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!super.equals(obj))
            return false;
        if (!(obj instanceof IIIFImageService))
            return false;
        IIIFImageService other = (IIIFImageService) obj;
        
        if (!other.canEqual(this)) {
            return false;
        }
        
        if (height != other.height)
            return false;
        if (!Arrays.equals(scaleFactors, other.scaleFactors))
            return false;
        if (tileHeight != other.tileHeight)
            return false;
        if (tileWidth != other.tileWidth)
            return false;
        if (width != other.width)
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "IIIFImageService [width=" + width + ", height=" + height + ", tileWidth=" + tileWidth + ", tileHeight="
                + tileHeight + ", scaleFactors=" + Arrays.toString(scaleFactors) + "]";
    }
}
