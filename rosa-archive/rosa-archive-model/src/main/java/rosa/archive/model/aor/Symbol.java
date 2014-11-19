package rosa.archive.model.aor;

import java.io.Serializable;

/**
 *
 */
public class Symbol extends Annotation implements Serializable {

    private String name;
    private String place;

    public Symbol() {}

    public Symbol(String referringText, String name, String place) {
        super(referringText);
        this.name = name;
        this.place = place;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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

        Symbol symbol = (Symbol) o;

        if (name != null ? !name.equals(symbol.name) : symbol.name != null) return false;
        if (place != null ? !place.equals(symbol.place) : symbol.place != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + (place != null ? place.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "Symbol{" +
                "name='" + name + '\'' +
                ", place='" + place + '\'' +
                '}';
    }
}
