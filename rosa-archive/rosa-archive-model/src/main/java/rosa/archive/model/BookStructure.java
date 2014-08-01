package rosa.archive.model;

import com.google.gwt.user.client.rpc.IsSerializable;

import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class BookStructure implements IsSerializable {

    /**
     * List of pages in order.
     */
    private List<StructurePage> pages;

    public BookStructure() {
        this.pages = new ArrayList<>();
    }

    public List<StructurePage> getPages() {
        return pages;
    }

    public List<StructureColumn> getAllColumns() {

        return null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BookStructure)) return false;

        BookStructure that = (BookStructure) o;

        if (pages != null ? !pages.equals(that.pages) : that.pages != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return pages != null ? pages.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "BookStructure{" +
                "pages=" + pages +
                '}';
    }
}
