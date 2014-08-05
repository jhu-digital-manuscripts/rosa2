package rosa.archive.model;

import com.google.gwt.user.client.rpc.IsSerializable;

import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class StructurePageSide implements IsSerializable {

    private List<Item> spanning;
    private List<StructureColumn> columns;

    public StructurePageSide() {
        this.spanning = new ArrayList<>();
        this.columns = new ArrayList<>();
    }

    public List<Item> spanning() {
        return spanning;
    }

    public List<StructureColumn> columns() {
        return columns;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof StructurePageSide)) return false;

        StructurePageSide that = (StructurePageSide) o;

        if (!columns.equals(that.columns)) return false;
        if (!spanning.equals(that.spanning)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = spanning.hashCode();
        result = 31 * result + columns.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "StructurePageSide{" +
                "spanning=" + spanning +
                ", columns=" + columns +
                '}';
    }
}
