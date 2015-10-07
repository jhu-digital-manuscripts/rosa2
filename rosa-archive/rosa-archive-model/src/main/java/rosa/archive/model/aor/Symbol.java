package rosa.archive.model.aor;

import java.io.Serializable;

/**
 *
 */
public class Symbol extends Annotation implements Serializable {
    private static final long serialVersionUID = 1L;

    private String name;

    public Symbol() {}

    @Override
    public String toPrettyString() {
        return "Symbol: " + name + " (" + getReferringText() + ")";
    }

    public Symbol(String id, String referringText, String name, String language, Location location) {
        super(id, referringText, language, location);
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        Symbol symbol = (Symbol) o;

        if (name != null ? !name.equals(symbol.name) : symbol.name != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (name != null ? name.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "Symbol{" +
                "name='" + name + '\'' +
                '}';
    }
}
