package rosa.archive.model;

/**
 *
 */
public class Heading extends Item {

    private String text;

    public Heading() {  }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Heading)) return false;
        if (!super.equals(o)) return false;

        Heading heading = (Heading) o;

        if (text != null ? !text.equals(heading.text) : heading.text != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (text != null ? text.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "Heading{" +
                "text='" + text + '\'' +
                ", lines='" + getLines() + '\'' +
                '}';
    }
}
