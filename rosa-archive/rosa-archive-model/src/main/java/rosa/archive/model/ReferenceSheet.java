package rosa.archive.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ReferenceSheet implements HasId, Serializable {
    private static final long serialVersionUID = 1L;

    protected String id;
    protected Map<String, List<String>> lines;

    public ReferenceSheet() {
        lines = new HashMap<>();
    }

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
     * Add values for the specified key/reference value. If the key already
     * exists, do not overwrite values already present. Instead, add new
     * values to those that already exist.
     *
     * @param key key to add
     * @param values values to add
     */
    public void addValues(String key, String ... values) {
        if (key == null) {
            return;
        }

        if (containsKey(key)) {
            this.lines.get(key).addAll(Arrays.asList(values));
        } else {
            List<String> vals = new ArrayList<>();
            vals.addAll(Arrays.asList(values));
            // Use new ArrayList because Arrays.asList(...) creates a fixed-sized list
            this.lines.put(key, vals);
        }
    }

    public List<String> getAlternates(String key) {
        if (!hasAlternates(key)) {
            return null;
        }

        List<String> result = new ArrayList<>();

        int len = lines.get(key).size();
        for (int i = 1; i < len; i++) {
            String val = getCell(key, i);
            if (val != null && !val.isEmpty()) {
                result.add(val);
            }
        }

        return result;
    }

    public List<String> getLine(String key) {
        return lines.get(key);
    }

    public Set<String> getKeys() {
        return lines.keySet();
    }

    /**
     * @param key key
     * @return are there any alternate values for this key?
     */
    public boolean hasAlternates(String key) {
        return containsKey(key) && lines.get(key) != null && lines.get(key).size() > 1;
    }

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

    protected String getCell(String key, int index) {
        return getLine(key) != null && getLine(key).size() > index ? getLine(key).get(index) : null;
    }

    public boolean canEqual(Object o) {
        return (o instanceof ReferenceSheet);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ReferenceSheet)) return false;

        ReferenceSheet that = (ReferenceSheet) o;

        if (id != null ? !id.equals(that.id) : that.id != null) return false;
        if (lines != null ? !lines.equals(that.lines) : that.lines != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (lines != null ? lines.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "ReferenceSheet{" +
                "id='" + id + '\'' +
                ", lines=" + lines +
                '}';
    }
}
