package rosa.archive.model.redtag;

/**
 *
 */
public class Initial extends Item {
    private static final long serialVersionUID = 1L;

    private String character;
    private boolean isEmpty;

    public Initial() {  }

    public String getCharacter() {
        return character;
    }

    public void setCharacter(String character) {
        this.character = character;
    }

    public boolean isEmpty() {
        return isEmpty;
    }

    public void setEmpty(boolean isEmpty) {
        this.isEmpty = isEmpty;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Initial)) return false;
        if (!super.equals(o)) return false;

        Initial initial = (Initial) o;

        if (isEmpty != initial.isEmpty) return false;
        if (character != null ? !character.equals(initial.character) : initial.character != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (character != null ? character.hashCode() : 0);
        result = 31 * result + (isEmpty ? 1 : 0);
        return result;
    }

    @Override
    public String toString() {
        return "Initial{" +
                "character='" + character + '\'' +
                ", isEmpty=" + isEmpty +
                ", lines=" + getLines() + "'" +
                '}';
    }
}
