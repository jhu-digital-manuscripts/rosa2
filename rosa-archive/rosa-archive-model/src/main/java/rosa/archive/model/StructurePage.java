package rosa.archive.model;

import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * A page in a book. Used for determining the books structure
 */
public class StructurePage implements IsSerializable {

    private String id;
    private String name;
    private StructurePageSide recto;
    private StructurePageSide verso;

    public StructurePage() {  }

    public StructurePage(String name, int linesPerColumn) {
        this.id = name;
        this.name = name;
        this.recto = new StructurePageSide(name + "r", linesPerColumn);
        this.verso = new StructurePageSide(name + "v", linesPerColumn);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public StructurePageSide getRecto() {
        return recto;
    }

    public void setRecto(StructurePageSide recto) {
        this.recto = recto;
    }

    public StructurePageSide getVerso() {
        return verso;
    }

    public void setVerso(StructurePageSide verso) {
        this.verso = verso;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof StructurePage)) return false;

        StructurePage that = (StructurePage) o;

        if (id != null ? !id.equals(that.id) : that.id != null) return false;
        if (name != null ? !name.equals(that.name) : that.name != null) return false;
        if (recto != null ? !recto.equals(that.recto) : that.recto != null) return false;
        if (verso != null ? !verso.equals(that.verso) : that.verso != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + (recto != null ? recto.hashCode() : 0);
        result = 31 * result + (verso != null ? verso.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "StructurePage{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", recto=" + recto +
                ", verso=" + verso +
                '}';
    }
}
