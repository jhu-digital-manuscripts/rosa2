package rosa.iiif.image.model;

import java.io.Serializable;

/**
 * An image may be rotated and mirrored.
 * 
 * An image is mirrored by reflection on the vertical axis before any rotation
 * is applied. The angle is the number of degrees of clockwise rotation and may
 * range from 0 to 360.
 */
public class Rotation implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private double angle;
    private boolean mirrored;

    public Rotation() {
        this(0.0, false);
    }
    
    public Rotation(double angle) {
        this(angle, false);
    }
    
    public Rotation(double angle, boolean mirrored) {
        this.angle = angle;
        this.mirrored = mirrored;
    }
    
    public double getAngle() {
        return angle;
    }

    public void setAngle(double angle) {
        this.angle = angle;
    }

    public boolean isMirrored() {
        return mirrored;
    }

    public void setMirrored(boolean mirrored) {
        this.mirrored = mirrored;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        long temp;
        temp = Double.doubleToLongBits(angle);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        result = prime * result + (mirrored ? 1231 : 1237);
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
        if (!(obj instanceof Rotation)) {
            return false;
        }
        Rotation other = (Rotation) obj;
        if (Double.doubleToLongBits(angle) != Double.doubleToLongBits(other.angle)) {
            return false;
        }
        if (mirrored != other.mirrored) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "Rotation [angle=" + angle + ", mirrored=" + mirrored + "]";
    }

}
