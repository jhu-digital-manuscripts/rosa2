package rosa.iiif.presentation.model.util;

import java.util.HashMap;
import java.util.Map;

public final class MultiLangValue {

    private final Map<String, String> data;

    public MultiLangValue() {
        data = new HashMap<>();
    }

    public MultiLangValue(String initialValue, String language) {
        this();
        addValue(initialValue, language);
    }

    public String[] languages() {
        return data.keySet().toArray(new String[data.keySet().size()]);
    }

    public boolean hasLanguage(String language) {
        return data.containsKey(language);
    }

    public String getValue(String language) {
        if (hasLanguage(language)) {
            return data.get(language);
        } else {
            return "";
        }
    }

    public void addValue(String value, String language) {
        if (value == null || language == null) {
            return;
        }
        data.put(language, value);
    }

    public String removeForLanguage(String language) {
        return data.remove(language);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        MultiLangValue that = (MultiLangValue) o;

        if (!data.equals(that.data)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return data.hashCode();
    }

    @Override
    public String toString() {
        return "MultiLangValue{" +
                "data=" + data +
                '}';
    }
}
