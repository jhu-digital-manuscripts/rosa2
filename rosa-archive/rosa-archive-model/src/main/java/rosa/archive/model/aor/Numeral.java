package rosa.archive.model.aor;

import java.io.Serializable;

/**
 *
 */
public class Numeral extends Annotation implements Serializable {

    public Numeral() {}

    public Numeral(String referringText, Location location) {
        super(referringText, location);
    }

    @Override
    public String toPrettyString() {
        return null;
    }
}
