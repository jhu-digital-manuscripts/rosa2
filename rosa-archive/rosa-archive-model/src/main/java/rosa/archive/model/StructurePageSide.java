package rosa.archive.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class StructurePageSide implements Serializable {
    private static final long serialVersionUID = 1L;

    private String parentPage;
    private List<Item> spanning;
    private List<StructureColumn> columns;

    public StructurePageSide() {
        this.spanning = new ArrayList<>();
        this.columns = new ArrayList<>();
    }

    public StructurePageSide(String name, int linesPerColumn) {
        this();
        this.parentPage = name;
        boolean endsInR = name.charAt(name.length() - 1) == 'r';

        char col1_letter = endsInR ? 'a' : 'c';
        char col2_letter = endsInR ? 'b' : 'd';

        columns.add(new StructureColumn(parentPage, linesPerColumn, col1_letter));
        columns.add(new StructureColumn(parentPage, linesPerColumn, col2_letter));
    }

    public List<Item> spanning() {
        return spanning;
    }

    public List<StructureColumn> columns() {
        return columns;
    }

    public String getParentPage() {
        return parentPage;
    }

    public void setParentPage(String parentPage) {
        this.parentPage = parentPage;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof StructurePageSide)) return false;

        StructurePageSide that = (StructurePageSide) o;

        if (columns != null ? !columns.equals(that.columns) : that.columns != null) return false;
        if (parentPage != null ? !parentPage.equals(that.parentPage) : that.parentPage != null) return false;
        if (spanning != null ? !spanning.equals(that.spanning) : that.spanning != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = parentPage != null ? parentPage.hashCode() : 0;
        result = 31 * result + (spanning != null ? spanning.hashCode() : 0);
        result = 31 * result + (columns != null ? columns.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "StructurePageSide{" +
                "parentPage='" + parentPage + '\'' +
                ", spanning=" + spanning +
                ", columns=" + columns +
                '}';
    }
}
