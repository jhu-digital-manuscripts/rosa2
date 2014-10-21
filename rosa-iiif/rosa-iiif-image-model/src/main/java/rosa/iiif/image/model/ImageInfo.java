package rosa.iiif.image.model;

import java.util.Arrays;


public class ImageInfo {
    private String id;
    private int width;
    private int height;
    private int[] scale_factors;
    private ImageFormat[] formats;
    private Quality[] qualities;
    private int tile_width;
    private int tile_height;

    public ImageInfo() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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

    public int[] getScaleFactors() {
        return scale_factors;
    }

    public void setScaleFactors(int... scale_factors) {
        this.scale_factors = scale_factors;
    }

    public ImageFormat[] getFormats() {
        return formats;
    }

    public void setFormats(ImageFormat... formats) {
        this.formats = formats;
    }

    public Quality[] getQualities() {
        return qualities;
    }

    public void setQualities(Quality... qualities) {
        this.qualities = qualities;
    }

    public int getTileWidth() {
        return tile_width;
    }

    public void setTileWidth(int tile_width) {
        this.tile_width = tile_width;
    }

    public int getTileHeight() {
        return tile_height;
    }

    public void setTileHeight(int tile_height) {
        this.tile_height = tile_height;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + Arrays.hashCode(formats);
        result = prime * result + height;
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        result = prime * result + Arrays.hashCode(qualities);
        result = prime * result + Arrays.hashCode(scale_factors);
        result = prime * result + tile_height;
        result = prime * result + tile_width;
        result = prime * result + width;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        ImageInfo other = (ImageInfo) obj;
        if (!Arrays.equals(formats, other.formats))
            return false;
        if (height != other.height)
            return false;
        if (id == null) {
            if (other.id != null)
                return false;
        } else if (!id.equals(other.id))
            return false;
        if (!Arrays.equals(qualities, other.qualities))
            return false;
        if (!Arrays.equals(scale_factors, other.scale_factors))
            return false;
        if (tile_height != other.tile_height)
            return false;
        if (tile_width != other.tile_width)
            return false;
        if (width != other.width)
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "ImageInfo [id=" + id + ", width=" + width + ", height="
                + height + ", scale_factors=" + Arrays.toString(scale_factors)
                + ", formats=" + Arrays.toString(formats) + ", qualities="
                + Arrays.toString(qualities) + ", tile_width=" + tile_width
                + ", tile_height=" + tile_height + "]";
    }
}
