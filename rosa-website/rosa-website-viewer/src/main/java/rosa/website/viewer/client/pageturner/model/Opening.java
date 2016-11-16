package rosa.website.viewer.client.pageturner.model;

public class Opening {

    public final Page verso;
    public final Page recto;

    public final String label;
    public final int position;

    public Opening(Page verso, Page recto, String label, int position) {
        this.verso = verso;
        this.recto = recto;
        this.label = label;
        this.position = position;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Opening)) return false;

        Opening opening = (Opening) o;

        if (position != opening.position) return false;
        if (verso != null ? !verso.equals(opening.verso) : opening.verso != null) return false;
        if (recto != null ? !recto.equals(opening.recto) : opening.recto != null) return false;
        return label != null ? label.equals(opening.label) : opening.label == null;

    }

    @Override
    public int hashCode() {
        int result = verso != null ? verso.hashCode() : 0;
        result = 31 * result + (recto != null ? recto.hashCode() : 0);
        result = 31 * result + (label != null ? label.hashCode() : 0);
        result = 31 * result + position;
        return result;
    }

    @Override
    public String toString() {
        return "Opening{" +
                "verso=" + verso +
                ", recto=" + recto +
                ", label='" + label + '\'' +
                ", position=" + position +
                '}';
    }
}
