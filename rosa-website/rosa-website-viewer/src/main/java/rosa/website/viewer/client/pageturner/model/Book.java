package rosa.website.viewer.client.pageturner.model;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class Book {
    /** Directory in FSI server that represents this book. All page IDs must be prefixed with this value. */
    public final String fsiDirectory;
    public final List<Opening> openings;

    public final Page missingImage;
    public final Page[] endSingles;

    public Book(String fsiDirectory, List<Opening> openings, Page missingImage, Page ... endSingles) {
        if (fsiDirectory == null || fsiDirectory.isEmpty()) {
            throw new IllegalArgumentException("Fsi directory must be specified for this book.");
        }
        if (openings == null || openings.isEmpty()) {
            throw new IllegalArgumentException("List of openings must be provided for this book.");
        }
        this.fsiDirectory = fsiDirectory;
        this.openings = Collections.unmodifiableList(openings);
        this.missingImage = missingImage;
        this.endSingles = endSingles;
    }

    /**
     * @param pageId desired page ID
     * @return the opening containing pageId, NULL if no opening is found.
     * @throws IllegalArgumentException if no pageId is specified
     */
    public Opening getOpening(String pageId) {
        if (pageId == null || pageId.isEmpty()) {
            throw new IllegalArgumentException("Page ID must be specified.");
        }

        for (Opening opening : openings) {
            if ((opening.verso != null && opening.verso.id.equals(pageId)) ||
                    (opening.recto != null && opening.recto.id.equals(pageId))) {
                return opening;
            }
        }

        return null;
    }

    public Opening getOpening(int index) {
        if (index < 0 || index >= openings.size()) {
            throw new IndexOutOfBoundsException();
        }
        return openings.get(index);
    }

    public int getPagePosition(String pageId) {
        // Check openings
        Opening fromOpenings = getOpening(pageId);
        if (fromOpenings != null) {
            if (fromOpenings.verso != null && fromOpenings.verso.id.equals(pageId)) {
                return fromOpenings.position * 2;
            } else if (fromOpenings.recto != null && fromOpenings.recto.id.equals(pageId)) {
                return fromOpenings.position * 2 + 1;
            }
        }

        // Check single images at end
        if (endSingles != null) {
            for (int i = 0; i < endSingles.length; i++) {
                Page p = endSingles[i];
                if (p != null &&p.id.equals(pageId)) {
                    return openings.size() * 2 + 1 + i;
                }
            }
        }

        return -1;
    }

    /**
     * Get a single string containing all page IDs separated by commas. This list
     * preserves the ordering of the images.
     *
     * @return a comma separated list of all page IDs
     */
    public String getPagesList() {
        StringBuilder sb = new StringBuilder();

        for (Opening current : openings) {
            if (current.verso != null) {
                sb.append(current.verso.id).append(',');
            }
            if (current.recto != null) {
                sb.append(current.recto.id).append(',');
            }
        }

        return sb.deleteCharAt(sb.length()).toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Book)) return false;

        Book book = (Book) o;

        if (fsiDirectory != null ? !fsiDirectory.equals(book.fsiDirectory) : book.fsiDirectory != null) return false;
        if (openings != null ? !openings.equals(book.openings) : book.openings != null) return false;
        if (missingImage != null ? !missingImage.equals(book.missingImage) : book.missingImage != null) return false;
        // Probably incorrect - comparing Object[] arrays with Arrays.equals
        return Arrays.equals(endSingles, book.endSingles);

    }

    @Override
    public int hashCode() {
        int result = fsiDirectory != null ? fsiDirectory.hashCode() : 0;
        result = 31 * result + (openings != null ? openings.hashCode() : 0);
        result = 31 * result + (missingImage != null ? missingImage.hashCode() : 0);
        result = 31 * result + Arrays.hashCode(endSingles);
        return result;
    }

    @Override
    public String toString() {
        return "Book{" +
                "fsiDirectory='" + fsiDirectory + '\'' +
                ", openings=" + openings +
                ", missingImage=" + missingImage +
                ", endSingles=" + Arrays.toString(endSingles) +
                '}';
    }
}
