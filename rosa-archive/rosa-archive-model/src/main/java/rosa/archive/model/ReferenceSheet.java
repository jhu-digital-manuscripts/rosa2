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

    private String id;
    private Map<String, List<String>> values;

    public ReferenceSheet() {
        values = new HashMap<>();
    }

    public Map<String, List<String>> getValues() {
        return values;
    }

    public void setValues(Map<String, List<String>> values) {
        this.values = values;
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

        if (this.values.containsKey(key)) {
            this.values.get(key).addAll(Arrays.asList(values));
        } else {
            List<String> vals = new ArrayList<>();
            vals.addAll(Arrays.asList(values));
            // Use new ArrayList because Arrays.asList(...) creates a fixed-sized list
            this.values.put(key, vals);
        }
    }

    public List<String> getAlternates(String key) {
        return values.get(key);
    }

    public Set<String> getKeys() {
        return values.keySet();
    }

    public boolean containsAlternates(String key) {
        return values.containsKey(key) && values.get(key) != null && !values.get(key).isEmpty();
    }

    public boolean containsKey(String key) {
        return values.containsKey(key);
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    public boolean canEqual(Object o) {
        return (o instanceof ReferenceSheet);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ReferenceSheet)) return false;

        ReferenceSheet that = (ReferenceSheet) o;
        if (!that.canEqual(this)) return false;

        if (id != null ? !id.equals(that.id) : that.id != null) return false;
        if (values != null ? !values.equals(that.values) : that.values != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (values != null ? values.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "MultiValueReference{" +
                "id='" + id + '\'' +
                ", values=" + values +
                '}';
    }
}
