package rosa.iiif.presentation.model;

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
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((language == null) ? 0 : language.hashCode());
        result = prime * result + ((value == null) ? 0 : value.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (!(obj instanceof TextValue))
            return false;
        TextValue other = (TextValue) obj;
        if (language == null) {
            if (other.language != null)
                return false;
        } else if (!language.equals(other.language))
            return false;
        if (value == null) {
            if (other.value != null)
                return false;
        } else if (!value.equals(other.value))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "TextValue [value=" + value + ", language=" + language + "]";
    }
}
