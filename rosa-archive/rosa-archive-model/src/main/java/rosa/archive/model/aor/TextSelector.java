package rosa.archive.model.aor;

import java.io.Serializable;

/**
 * Object that specifies a block of text within a larger body.
 * {@link #text} is required to specify the exact text to select.
 * {@link #prefix} and {@link #suffix} are both optional, used
 * to help disambiguation of the text.
 */
public class TextSelector implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String text;
    private final String prefix;
    private final String suffix;

    public TextSelector(String text) {
        this(text, null, null);
    }

    public TextSelector(String text, String prefix, String suffix) {
        this.text = text;
        this.prefix = prefix;
        this.suffix = suffix;
    }

    public String getText() {
        return text;
    }

    public String getPrefix() {
        return prefix;
    }

    public String getSuffix() {
        return suffix;
    }

    public boolean hasContent() {
        return text != null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TextSelector that = (TextSelector) o;

        if (text != null ? !text.equals(that.text) : that.text != null) return false;
        if (prefix != null ? !prefix.equals(that.prefix) : that.prefix != null) return false;
        return suffix != null ? suffix.equals(that.suffix) : that.suffix == null;
    }

    @Override
    public int hashCode() {
        int result = text != null ? text.hashCode() : 0;
        result = 31 * result + (prefix != null ? prefix.hashCode() : 0);
        result = 31 * result + (suffix != null ? suffix.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "TextSelector{" +
                "text='" + text + '\'' +
                ", prefix='" + prefix + '\'' +
                ", suffix='" + suffix + '\'' +
                '}';
    }
}
