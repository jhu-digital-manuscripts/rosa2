package rosa.archive.model.aor;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * The {@code id_locations.csv} of an AoR collection: where each annotation or page id used
 * by the original Archaeology of Reading data lives in this archive.
 *
 * <p>The ids are the ones internal references ({@code <target ref="…">}) and the
 * {@code id} attributes of annotations use, {@code {book}:{page token}[:{annotation}]},
 * where the page token is the library's original scan name. Each row names the
 * collection, the book, the page (a folio such as {@code 35r}, an image file such as
 * {@code RCP7134.131r.tif}, or an image label such as {@code front matter 5r}) and, for
 * annotation ids, the id again.
 */
public final class IdLocations {

    /**
     * One row of the sheet.
     *
     * @param collection   the collection id
     * @param book         the book id
     * @param page         the page: folio, image file or image label; empty for a book-level id
     * @param annotationId the annotation id, or empty for a page id
     */
    public record IdLocation(String collection, String book, String page, String annotationId) {
    }

    private final Map<String, IdLocation> locations = new LinkedHashMap<>();

    /**
     * Adds a row. A later row with the same id replaces the earlier one.
     */
    public void add(String id, IdLocation location) {
        locations.put(Objects.requireNonNull(id), Objects.requireNonNull(location));
    }

    /**
     * The location of an id, or null when the sheet does not have it.
     */
    public IdLocation get(String id) {
        return id == null ? null : locations.get(id);
    }

    public boolean contains(String id) {
        return id != null && locations.containsKey(id);
    }

    public int size() {
        return locations.size();
    }

    /** All rows, by id, in file order. */
    public Map<String, IdLocation> asMap() {
        return Collections.unmodifiableMap(locations);
    }

    @Override
    public boolean equals(Object o) {
        return this == o || (o instanceof IdLocations that && locations.equals(that.locations));
    }

    @Override
    public int hashCode() {
        return locations.hashCode();
    }

    @Override
    public String toString() {
        return "IdLocations{" + locations.size() + " ids}";
    }
}
