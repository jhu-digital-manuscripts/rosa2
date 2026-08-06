package rosa.archive.model.aor;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * A language section within a marginalia annotation, containing one or more
 * positional entries for text in a specific language.
 */
public final class MarginaliaLanguage {

    private String lang;
    private List<Position> positions;

    /**
     * Creates an empty marginalia language section.
     */
    public MarginaliaLanguage() {
        positions = new ArrayList<>();
    }

    /**
     * Returns the language code for this section.
     *
     * @return the language code, or null
     */
    public String getLang() {
        return lang;
    }

    /**
     * Sets the language code.
     *
     * @param lang the language code
     */
    public void setLang(String lang) {
        this.lang = lang;
    }

    /**
     * Returns the positions in this language section.
     *
     * @return the positions (never null)
     */
    public List<Position> getPositions() {
        return positions;
    }

    /**
     * Sets the positions.
     *
     * @param positions the positions
     */
    public void setPositions(List<Position> positions) {
        this.positions = positions;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MarginaliaLanguage that)) return false;
        return Objects.equals(lang, that.lang) &&
                Objects.equals(positions, that.positions);
    }

    @Override
    public int hashCode() {
        return Objects.hash(lang, positions);
    }

    @Override
    public String toString() {
        return "MarginaliaLanguage{lang='" + lang + "', positions=" + positions + '}';
    }
}
