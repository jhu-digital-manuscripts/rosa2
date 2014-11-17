package rosa.iiif.image.model;

import java.io.Serializable;

/**
 * Request to return information about an image.
 */
public class InfoRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    private String image_id;
    private InfoFormat format;

    public InfoRequest() {
    }

    public String getImageId() {
        return image_id;
    }

    public void setImageId(String image_id) {
        this.image_id = image_id;
    }

    public InfoFormat getFormat() {
        return format;
    }

    public void setFormat(InfoFormat fmt) {
        this.format = fmt;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((format == null) ? 0 : format.hashCode());
        result = prime * result + ((image_id == null) ? 0 : image_id.hashCode());
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
        if (!(obj instanceof InfoRequest)) {
            return false;
        }
        InfoRequest other = (InfoRequest) obj;
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
        return true;
    }

    @Override
    public String toString() {
        return "InfoRequest [image_id=" + image_id + ", format=" + format + "]";
    }

}
