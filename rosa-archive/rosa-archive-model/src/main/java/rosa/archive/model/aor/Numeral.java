package rosa.archive.model.aor;

import java.io.Serializable;

/**
 *
 */
public class Numeral extends Annotation implements Serializable {
    private static final long serialVersionUID = 1L;

    public Numeral() {}

    public Numeral(String id, String referringText, String language, Location location) {
        super(id, referringText, language, location);
    }

    @Override
    public String toPrettyString() {
        return "Numeral: (" + getReferringText() + ")";
    }
}
