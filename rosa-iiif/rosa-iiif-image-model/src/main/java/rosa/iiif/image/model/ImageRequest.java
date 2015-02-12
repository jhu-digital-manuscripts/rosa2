package rosa.iiif.image.model;

import java.io.Serializable;

/**
 * Request to perform a transform on an image and return the result.
 * 
 * Order of operations: Region THEN Size THEN Rotation THEN Quality THEN Format
 */
public class ImageRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private String image_id;
    private ImageFormat format;
    private Size size;
    private Region region;
    private Quality quality;
    private Rotation rotation;

    /**
     * Create a new empty ImageRequest
     */
    public ImageRequest() {
    }

    public String getImageId() {
        return image_id;
    }

    public void setImageId(String image_id) {
        this.image_id = image_id;
    }

    public ImageFormat getFormat() {
        return format;
    }

    public void setFormat(ImageFormat format) {
        this.format = format;
    }

    public Size getSize() {
        return size;
    }

    public void setSize(Size scale) {
        this.size = scale;
    }

    public Region getRegion() {
        return region;
    }

    public void setRegion(Region region) {
        this.region = region;
    }

    public Quality getQuality() {
        return quality;
    }

    public void setQuality(Quality quality) {
        this.quality = quality;
    }

    public Rotation getRotation() {
        return rotation;
    }

    public void setRotation(Rotation rotation) {
        this.rotation = rotation;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((format == null) ? 0 : format.hashCode());
        result = prime * result + ((image_id == null) ? 0 : image_id.hashCode());
        result = prime * result + ((quality == null) ? 0 : quality.hashCode());
        result = prime * result + ((region == null) ? 0 : region.hashCode());
        result = prime * result + ((rotation == null) ? 0 : rotation.hashCode());
        result = prime * result + ((size == null) ? 0 : size.hashCode());
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
        if (!(obj instanceof ImageRequest)) {
            return false;
        }
        ImageRequest other = (ImageRequest) obj;
        if (format != other.format) {
            return false;
        }
        if (image_id == null) {
            if (other.image_id != null) {
                return false;
            }
        } else if (!image_id.equals(other.image_id)) {
            return false;
        }
        if (quality != other.quality) {
            return false;
        }
        if (region == null) {
            if (other.region != null) {
                return false;
            }
        } else if (!region.equals(other.region)) {
            return false;
        }
        if (rotation == null) {
            if (other.rotation != null) {
                return false;
            }
        } else if (!rotation.equals(other.rotation)) {
            return false;
        }
        if (size == null) {
            if (other.size != null) {
                return false;
            }
        } else if (!size.equals(other.size)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "ImageRequest [image_id=" + image_id + ", format=" + format + ", size=" + size + ", region=" + region
                + ", quality=" + quality + ", rotation=" + rotation + "]";
    }

}
