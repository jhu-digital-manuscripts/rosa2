package rosa.archive.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A specialized reference sheet for book references in the AoR corpus.
 * Columns: Standard Name, Alternate Names (1-5), Author, Full Title, USTC, EEBO,
 * Digitale Sammlungen, Perseus, Other.
 */
public final class BookReferenceSheet extends ReferenceSheet {

    /**
     * External link types associated with book references.
     */
    public enum Link {
        /** Universal Short Title Catalogue. */
        USTC(8, "USTC"),
        /** Early English Books Online. */
        EEBO(9, "EEBO"),
        /** Digitale Sammlungen (Bayerische Staatsbibliothek). */
        DIGITALE_SAMMLUNGEN(10, "Digitale Sammlungen"),
        /** Perseus Digital Library. */
        PERSEUS(11, "Perseus"),
        /** Other links. */
        OTHER(12, "Other");

        /** The display label for this link type. */
        public final String label;
        /** The column index in the reference sheet. */
        public final int index;

        Link(int index, String label) {
            this.index = index;
            this.label = label;
        }

        /**
         * Returns the Link enum for the given column index.
         *
         * @param i the column index
         * @return the corresponding Link, or {@code null} if no match
         */
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
        var result = new ArrayList<String>();
        List<String> line = getLine(key);
        for (int i = 1; i < line.size() && i < 6; i++) {
            String val = getCell(key, i);
            if (val != null && !val.isEmpty()) {
                result.add(val);
            }
        }
        return result;
    }

    /**
     * Returns the authors for the given book reference.
     *
     * @param key the standard book name
     * @return the list of authors, or {@code null} if not available
     */
    public List<String> getAuthors(String key) {
        String authors = getCell(key, 6);
        if (authors == null || authors.isEmpty()) {
            return null;
        }
        return Arrays.asList(authors.split(","));
    }

    /**
     * Returns the full title for the given book reference.
     *
     * @param key the standard book name
     * @return the full title, or {@code null} if not available
     */
    public String getFullTitle(String key) {
        String title = getCell(key, 7);
        if (title == null || title.isEmpty()) {
            return null;
        }
        return title;
    }

    /**
     * Returns external links related to a referenced book.
     *
     * @param key the standard book name
     * @return a map of link label to URI, or {@code null} if the key doesn't exist
     */
    public Map<String, String> getExternalLinks(String key) {
        if (!containsKey(key)) {
            return null;
        }
        List<String> line = getLine(key);
        var map = new HashMap<String, String>();
        for (int i = 8; i < line.size(); i++) {
            Link l = Link.getFromIndex(i);
            String val = line.get(i);
            if (l != null && val != null && !val.isEmpty()) {
                map.put(l.label, val);
            }
        }
        return map;
    }
}
