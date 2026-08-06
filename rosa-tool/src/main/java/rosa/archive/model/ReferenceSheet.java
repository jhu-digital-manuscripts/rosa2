package rosa.archive.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * A reference sheet mapping keys (standard names) to their alternate forms and associated data.
 * Used for people, locations, and book reference lookups.
 */
public class ReferenceSheet implements HasId {

    protected String id;
    protected Map<String, List<String>> lines;

    /**
     * Creates an empty ReferenceSheet.
     */
    public ReferenceSheet() {
        lines = new HashMap<>();
    }

    /**
     * Parses and sets lines from raw CSV-style strings.
     * Each line is split by comma, with the first element used as the key.
     *
     * @param lines the raw lines to parse
     */
    public void setLines(List<String> lines) {
        this.lines = new HashMap<>();
        for (String line : lines) {
            String[] lineArr = line.split(",");
            if (lineArr.length > 0) {
                this.lines.put(lineArr[0], Arrays.asList(lineArr));
            }
        }
    }

    /**
     * Adds values for the specified key. If the key already exists,
     * appends the new values to the existing ones.
     *
     * @param key    the key to add
     * @param values the values to associate with the key
     */
    public void addValues(String key, String... values) {
        if (key == null) {
            return;
        }
        if (containsKey(key)) {
            this.lines.get(key).addAll(Arrays.asList(values));
        } else {
            var vals = new ArrayList<>(Arrays.asList(values));
            this.lines.put(key, vals);
        }
    }

    /**
     * Returns alternate names for the given key (all values after the first).
     *
     * @param key the reference key
     * @return the list of alternates, or {@code null} if none
     */
    public List<String> getAlternates(String key) {
        if (!hasAlternates(key)) {
            return null;
        }
        var result = new ArrayList<String>();
        List<String> line = lines.get(key);
        for (int i = 1; i < line.size(); i++) {
            String val = getCell(key, i);
            if (val != null && !val.isEmpty()) {
                result.add(val);
            }
        }
        return result;
    }

    /**
     * Returns the full line (all values) for the given key.
     *
     * @param key the reference key
     * @return the line values, or {@code null} if the key doesn't exist
     */
    public List<String> getLine(String key) {
        return lines.get(key);
    }

    /**
     * Returns all keys in this reference sheet.
     *
     * @return the set of keys
     */
    public Set<String> getKeys() {
        return lines.keySet();
    }

    /**
     * Checks whether there are alternate values for this key.
     *
     * @param key the reference key
     * @return {@code true} if alternate values exist
     */
    public boolean hasAlternates(String key) {
        return containsKey(key) && lines.get(key) != null && lines.get(key).size() > 1;
    }

    /**
     * Checks whether this reference sheet contains the given key.
     *
     * @param key the reference key
     * @return {@code true} if the key exists
     */
    public boolean containsKey(String key) {
        return lines.containsKey(key);
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Returns the value at a specific column index for the given key.
     *
     * @param key   the reference key
     * @param index the column index
     * @return the cell value, or {@code null} if not available
     */
    protected String getCell(String key, int index) {
        List<String> line = getLine(key);
        return line != null && line.size() > index ? line.get(index) : null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ReferenceSheet that)) return false;
        return Objects.equals(id, that.id) &&
                Objects.equals(lines, that.lines);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, lines);
    }

    @Override
    public String toString() {
        return "ReferenceSheet{" +
                "id='" + id + '\'' +
                ", lines=" + lines +
                '}';
    }
}
