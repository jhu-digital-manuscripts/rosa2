package rosa.archive.model;

import java.util.ArrayList;
import java.util.List;

/**
 * The people reference sheet ({@code people.csv}) of an AoR collection.
 *
 * <p>Columns: standard name, alternative names (1-5), ISNI, extra info. Only the
 * alternative-name columns are alternates; the ISNI (or its {@code -} placeholder) and the
 * free-text extra info are never names and must not be indexed as such.
 */
public final class PeopleReferenceSheet extends ReferenceSheet {

    /** Index of the first alternative-name column. */
    public static final int FIRST_ALTERNATE_COLUMN = 1;

    /** Index one past the last alternative-name column. */
    public static final int END_ALTERNATE_COLUMN = 6;

    /** Index of the ISNI column. */
    public static final int ISNI_COLUMN = 6;

    @Override
    public List<String> getAlternates(String key) {
        if (!hasAlternates(key)) {
            return null;
        }
        var result = new ArrayList<String>();
        List<String> line = getLine(key);
        for (int i = FIRST_ALTERNATE_COLUMN; i < line.size() && i < END_ALTERNATE_COLUMN; i++) {
            String val = getCell(key, i);
            if (val != null && !val.isEmpty()) {
                result.add(val);
            }
        }
        return result;
    }

    /**
     * Returns the ISNI of the given person.
     *
     * @param key the standard name
     * @return the ISNI, or {@code null} if the sheet has none for this person
     */
    public String getIsni(String key) {
        String isni = getCell(key, ISNI_COLUMN);
        return isni == null || isni.isBlank() || isni.equals("-") ? null : isni;
    }
}
