package rosa.archive.model;

import java.io.Serializable;

public class CollectionEntry implements Serializable {
    private static final long serialVersionUID = 1L;

    public String id;
    public String name;
    public String origin;
    public String material;
    public int numFolios;
    public int height;
    public int width;
    public int leavesPerGathering;
    public int linesPerColumn;
    public int numIllus;
    public String dateStart;
    public String dateEnd;
    public int columnsPerFolio;
    public String texts;
    public int folios;
    public int foliosWithOneIllus;
    public int foliosWithMoreIllus;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CollectionEntry)) return false;

        CollectionEntry that = (CollectionEntry) o;

        if (columnsPerFolio != that.columnsPerFolio) return false;
        if (folios != that.folios) return false;
        if (foliosWithMoreIllus != that.foliosWithMoreIllus) return false;
        if (foliosWithOneIllus != that.foliosWithOneIllus) return false;
        if (height != that.height) return false;
        if (leavesPerGathering != that.leavesPerGathering) return false;
        if (linesPerColumn != that.linesPerColumn) return false;
        if (numFolios != that.numFolios) return false;
        if (numIllus != that.numIllus) return false;
        if (width != that.width) return false;
        if (dateEnd != null ? !dateEnd.equals(that.dateEnd) : that.dateEnd != null) return false;
        if (dateStart != null ? !dateStart.equals(that.dateStart) : that.dateStart != null) return false;
        if (id != null ? !id.equals(that.id) : that.id != null) return false;
        if (material != null ? !material.equals(that.material) : that.material != null) return false;
        if (name != null ? !name.equals(that.name) : that.name != null) return false;
        if (origin != null ? !origin.equals(that.origin) : that.origin != null) return false;
        if (texts != null ? !texts.equals(that.texts) : that.texts != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + (origin != null ? origin.hashCode() : 0);
        result = 31 * result + (material != null ? material.hashCode() : 0);
        result = 31 * result + numFolios;
        result = 31 * result + height;
        result = 31 * result + width;
        result = 31 * result + leavesPerGathering;
        result = 31 * result + linesPerColumn;
        result = 31 * result + numIllus;
        result = 31 * result + (dateStart != null ? dateStart.hashCode() : 0);
        result = 31 * result + (dateEnd != null ? dateEnd.hashCode() : 0);
        result = 31 * result + columnsPerFolio;
        result = 31 * result + (texts != null ? texts.hashCode() : 0);
        result = 31 * result + folios;
        result = 31 * result + foliosWithOneIllus;
        result = 31 * result + foliosWithMoreIllus;
        return result;
    }

    @Override
    public String toString() {
        return "CollectionEntry{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", origin='" + origin + '\'' +
                ", material='" + material + '\'' +
                ", numFolios=" + numFolios +
                ", height=" + height +
                ", width=" + width +
                ", leavesPerGathering=" + leavesPerGathering +
                ", linesPerColumn=" + linesPerColumn +
                ", numIllus=" + numIllus +
                ", dateStart='" + dateStart + '\'' +
                ", dateEnd='" + dateEnd + '\'' +
                ", columnsPerFolio=" + columnsPerFolio +
                ", texts='" + texts + '\'' +
                ", folios=" + folios +
                ", foliosWithOneIllus=" + foliosWithOneIllus +
                ", foliosWithMoreIllus=" + foliosWithMoreIllus +
                '}';
    }
}
