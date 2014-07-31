package rosa.archive.model;

/**
 *
 */
public class Rubric extends Item {

    private String text;

    public Rubric() {  }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Rubric)) return false;
        if (!super.equals(o)) return false;

        Rubric rubric = (Rubric) o;

        if (text != null ? !text.equals(rubric.text) : rubric.text != null) return false;

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
        return "Rubric{" +
                "text='" + text + '\'' +
                ", lines='" + getLines() + '\'' +
                '}';
    }
}
