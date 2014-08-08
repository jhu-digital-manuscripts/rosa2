package rosa.archive.model;

import com.google.gwt.user.client.rpc.IsSerializable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 *
 */
public class BookStructure implements Iterable<StructurePage>, IsSerializable {

    /**
     * List of pages in order.
     */
    private List<StructurePage> pages;

    public BookStructure() {
        this.pages = new ArrayList<>();
    }

    /**
     * @return
     *          number of pages in the book
     */
    public int size() {
        return pages.size();
    }

    /**
     * @param id
     *          ID of page to find
     * @return
     *          index of the page. -1 is returned if the page is not present.
     */
    public int findIndex(String id) {
        for (int i = 0; i < size(); i++) {
            StructurePage page = pages.get(i);
            if (page.getId().equals(id)) {
                return i;
            }
        }

        return -1;
    }

    /**
     * Retrieve the list of pages that defines the book structure. This list can be modified.
     *
     * @return
     *          modifiable list of pages
     */
    public List<StructurePage> pages() {
        return pages;
    }

    /**
     * Get a list of columns of text in the order in which they appear.
     *
     * @return
     *          a list of all columns
     */
    public List<StructureColumn> columns() {
        List<StructureColumn> columns = new ArrayList<>();

        for (StructurePage page : pages) {
            StructurePageSide recto = page.getRecto();
            if (recto != null) {
                columns.addAll(recto.columns());
            }

            StructurePageSide verso = page.getVerso();
            if (verso != null) {
                columns.addAll(verso.columns());
            }
        }

        return Collections.unmodifiableList(columns);
    }

    @Override
    public Iterator<StructurePage> iterator() {
        return new Iterator<StructurePage>() {
            int next = 0;

            @Override
            public boolean hasNext() {
                return next < size();
            }

            @Override
            public StructurePage next() {
                return pages.get(next++);
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException("Cannot remove item!");
            }
        };
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
