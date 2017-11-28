package rosa.archive.model.aor;

import java.io.Serializable;

/**
 *
 */
public class Numeral extends Annotation implements Serializable {
    private static final long serialVersionUID = 1L;
    private String numeral;
    
    public Numeral() {}

    public Numeral(String id, String referringText, String numeral, String language, Location location, String imageId) {
        super(id, referringText, language, imageId, location);
        this.numeral = numeral;
    }

    @Override
    public String toPrettyString() {
        return "Numeral: (" + getReferencedText() + " "  + numeral + ")";
    }
    
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((numeral == null) ? 0 : numeral.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!super.equals(obj))
            return false;
        if (!(obj instanceof Numeral))
            return false;
        Numeral other = (Numeral) obj;
        if (numeral == null) {
            if (other.numeral != null)
                return false;
        } else if (!numeral.equals(other.numeral))
            return false;
        return true;
    }

    public String getNumeral() {
        return numeral;
    }
    
    public void setNumeral(String numeral) {
        this.numeral = numeral;
    }
}
