package rosa.iiif.image.model;

import java.io.Serializable;
import java.util.Arrays;

public class ImageInfo implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private String id;
    private int width;
    private int height;
    private ImageFormat[] formats;
    private Quality[] qualities;
    private int[] sizes;
    private ServiceReference[] services;

    public ImageInfo() {
    }

    public String getImageId() {
        return id;
    }

    public void setImageId(String id) {
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

    /**
     * @return width,height pairs
     */
    public int[] getSizes() {
        return sizes;
    }

    /**
     * @param sizes
     *            must be width,height pairs
     */
    public void setSizes(int... sizes) {
        this.sizes = sizes;
    }

    public ServiceReference[] getServices() {
        return services;
    }

    public void setServices(ServiceReference... services) {
        this.services = services;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + Arrays.hashCode(formats);
        result = prime * result + height;
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        result = prime * result + Arrays.hashCode(qualities);
        result = prime * result + Arrays.hashCode(services);
        result = prime * result + Arrays.hashCode(sizes);
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
        if (!(obj instanceof ImageInfo)) {
            return false;
        }
        ImageInfo other = (ImageInfo) obj;
        if (!Arrays.equals(formats, other.formats)) {
            return false;
        }
        if (height != other.height) {
            return false;
        }
        if (id == null) {
            if (other.id != null) {
                return false;
            }
        } else if (!id.equals(other.id)) {
            return false;
        }
        if (!Arrays.equals(qualities, other.qualities)) {
            return false;
        }
        if (!Arrays.equals(services, other.services)) {
            return false;
        }
        if (!Arrays.equals(sizes, other.sizes)) {
            return false;
        }
        if (width != other.width) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "ImageInfo [id=" + id + ", width=" + width + ", height=" + height + ", formats="
                + Arrays.toString(formats) + ", qualities=" + Arrays.toString(qualities) + ", sizes="
                + Arrays.toString(sizes) + ", services=" + Arrays.toString(services) + "]";
    }
}
