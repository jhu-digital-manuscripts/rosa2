package rosa.archive.model.aor;

import java.io.Serializable;

/**
 *
 */
public class Numeral extends Annotation implements Serializable {

    private String place;

    public Numeral() {}

    public Numeral(String referringText, String place) {
        super(referringText);
        this.place = place;
    }

    public String getPlace() {
        return place;
    }

    public void setPlace(String place) {
        this.place = place;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        Numeral numeral = (Numeral) o;

        if (place != null ? !place.equals(numeral.place) : numeral.place != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (place != null ? place.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "Numeral{" +
                "place='" + place + '\'' +
                super.toString() +
                '}';
    }
}
