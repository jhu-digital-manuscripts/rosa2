package rosa.iiif.presentation.model.util;

public class TextValue {

    protected final String value;
    protected final String language;

    public TextValue(String value, String language) {
        this.language = language;
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public String getLanguage() {
        return language;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TextValue textValue = (TextValue) o;

        if (language != null ? !language.equals(textValue.language) : textValue.language != null) return false;
        if (value != null ? !value.equals(textValue.value) : textValue.value != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = value != null ? value.hashCode() : 0;
        result = 31 * result + (language != null ? language.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "TextValue{" +
                "value='" + value + '\'' +
                ", language='" + language + '\'' +
                '}';
    }
}
