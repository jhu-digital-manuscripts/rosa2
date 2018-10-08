package rosa.archive.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Columns:
 * Standard Name, Alternate Name 1, Alternate Name 2, Alternate Name 3, Alternate Name 4, Alternate Name 5, Bibl. Information: author, Bibl. Information: full title, USTC, EEBO, Digitale Sammlungen, Perseus, Other
 */
public final class BookReferenceSheet extends ReferenceSheet implements HasId, Serializable {
    private static final long serialVersionUID = 1L;

    public enum Link {
        USTC(8, "USTC"),
        EEBO(9, "EEBO"),
        DIGITALE_SAMMLUNGEN(10, "Digitale Sammlungen"),
        PERSEUS(11, "Perseus"),
        OTHER(12, "Other");

        public final String label;
        public final int index;

        Link(int index, String label) {
            this.index = index;
            this.label = label;
        }

        public static Link getFromIndex(int i) {
            for (Link l : Link.values()) {
                if (i == l.index) {
                    return l;
                }
            }
            return null;
        }
    }

    @Override
    public List<String> getAlternates(String key) {
        if (!hasAlternates(key)) {
            return null;
        }

        List<String> result = new ArrayList<>();
        int len = getLine(key).size();
        for (int i = 1; i < len; i++) {
            if (i >= 6) {
                continue;
            }

            String val = getCell(key, i);
            if (val != null && !val.isEmpty()) {
                result.add(val);
            }
        }

        return result;
    }

    public List<String> getAuthors(String key) {
        String authors = getCell(key, 6);
        if (authors == null || authors.isEmpty()) {
            return null;
        }

        return Arrays.asList(authors.split(","));
    }

    public String getFullTitle(String key) {
        String title = getCell(key, 7);
        if (title == null || title.isEmpty()) {
            return null;
        }

        return title;
    }

    /**
     * Get external links related to a book that has been referenced from the AOR corpus.
     * Map: label -> URI
     *
     * @param key standard book name
     * @return map
     */
    public Map<String, String> getExternalLinks(String key) {
        if (!containsKey(key)) {
            return null;
        }

        List<String> line = getLine(key);
        Map<String, String> map = new HashMap<>();
        for (int i = 8; i < line.size(); i++) {
            Link l = Link.getFromIndex(i);
            String val = line.get(i);

            if (l != null && (val != null && !val.isEmpty())) {
                map.put(l.label, line.get(i));
            }
        }
        return map;
    }
}
