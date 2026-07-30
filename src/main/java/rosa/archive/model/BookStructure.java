package rosa.archive.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Represents the physical structure of a book as an ordered list of pages.
 * Each page may have recto and verso sides with columns of text.
 */
public class BookStructure implements HasId {

    private String id;
    private List<StructurePage> pages;

    /**
     * Creates an empty BookStructure.
     */
    public BookStructure() {
        this.pages = new ArrayList<>();
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Returns the number of pages in the structure.
     *
     * @return the page count
     */
    public int size() {
        return pages.size();
    }

    /**
     * Finds the index of a page by its id.
     *
     * @param id the page id to find
     * @return the index of the page, or -1 if not found
     */
    public int findIndex(String id) {
        for (int i = 0; i < pages.size(); i++) {
            if (pages.get(i).getId().equals(id)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Returns the modifiable list of pages.
     *
     * @return the pages list
     */
    public List<StructurePage> pages() {
        return pages;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BookStructure that)) return false;
        return Objects.equals(id, that.id) &&
                Objects.equals(pages, that.pages);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, pages);
    }

    @Override
    public String toString() {
        return "BookStructure{" +
                "id='" + id + '\'' +
                ", pages=" + pages +
                '}';
    }

    /**
     * A page in the book structure with an identifier.
     */
    public static class StructurePage {
        private String id;

        /**
         * Creates an empty StructurePage.
         */
        public StructurePage() {}

        /**
         * Creates a StructurePage with the given id.
         *
         * @param id the page identifier
         */
        public StructurePage(String id) {
            this.id = id;
        }

        /**
         * Returns the page identifier.
         *
         * @return the page id
         */
        public String getId() {
            return id;
        }

        /**
         * Sets the page identifier.
         *
         * @param id the page id
         */
        public void setId(String id) {
            this.id = id;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof StructurePage that)) return false;
            return Objects.equals(id, that.id);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id);
        }

        @Override
        public String toString() {
            return "StructurePage{id='" + id + "'}";
        }
    }
}
