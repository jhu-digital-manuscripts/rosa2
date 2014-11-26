package rosa.iiif.presentation.model.selector;

import java.io.Serializable;

public class SvgSelector implements Selector, Serializable {
    private static final long serialVersionUID = 1L;

    private String context;
    private String type;
    private String chars;

    public SvgSelector() {}

    public SvgSelector(String context, String type, String chars) {
        this.context = context;
        this.type = type;
        this.chars = chars;
    }

    public void setContext(String context) {
        this.context = context;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setChars(String chars) {
        this.chars = chars;
    }

    @Override
    public String context() {
        return context;
    }

    @Override
    public String type() {
        return type;
    }

    @Override
    public String content() {
        return chars;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SvgSelector that = (SvgSelector) o;

        if (chars != null ? !chars.equals(that.chars) : that.chars != null) return false;
        if (context != null ? !context.equals(that.context) : that.context != null) return false;
        if (type != null ? !type.equals(that.type) : that.type != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = context != null ? context.hashCode() : 0;
        result = 31 * result + (type != null ? type.hashCode() : 0);
        result = 31 * result + (chars != null ? chars.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "SvgSelector{" +
                "context='" + context + '\'' +
                ", type='" + type + '\'' +
                ", chars='" + chars + '\'' +
                '}';
    }
}
