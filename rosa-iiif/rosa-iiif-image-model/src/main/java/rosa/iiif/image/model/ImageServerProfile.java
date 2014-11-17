package rosa.iiif.image.model;

import java.io.Serializable;
import java.util.Arrays;

/**
 * Profile giving capabilities of an image server.
 */
public class ImageServerProfile implements Serializable {
    private static final long serialVersionUID = 1L;

    private ImageServerSupports[] supports;
    private ImageFormat[] formats;
    private Quality[] qualities;

    public ImageServerSupports[] getSupports() {
        return supports;
    }

    public void setSupports(ImageServerSupports... supports) {
        this.supports = supports;
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

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + Arrays.hashCode(formats);
        result = prime * result + Arrays.hashCode(qualities);
        result = prime * result + Arrays.hashCode(supports);
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
        if (!(obj instanceof ImageServerProfile)) {
            return false;
        }
        ImageServerProfile other = (ImageServerProfile) obj;
        if (!Arrays.equals(formats, other.formats)) {
            return false;
        }
        if (!Arrays.equals(qualities, other.qualities)) {
            return false;
        }
        if (!Arrays.equals(supports, other.supports)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "ImageServerProfile [supports=" + Arrays.toString(supports) + ", formats=" + Arrays.toString(formats)
                + ", qualities=" + Arrays.toString(qualities) + "]";
    }
}
