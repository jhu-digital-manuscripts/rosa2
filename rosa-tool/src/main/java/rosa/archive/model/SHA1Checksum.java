package rosa.archive.model;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Holds SHA-1 checksums for all items in the archive for a book.
 * Maps archive item identifiers to their checksum values.
 */
public class SHA1Checksum implements HasId {

    /** The checksum algorithm name. */
    public static final String ALGORITHM = "SHA1";

    private String id;
    private Map<String, String> checksums;

    /**
     * Creates an empty checksum mapping.
     */
    public SHA1Checksum() {
        this.checksums = new HashMap<>();
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
     * Returns the set of all item identifiers with stored checksums.
     *
     * @return the set of item IDs
     */
    public Set<String> getAllIds() {
        return checksums.keySet();
    }

    /**
     * Returns the map of item identifiers to checksum values.
     *
     * @return the checksums map
     */
    public Map<String, String> checksums() {
        return checksums;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SHA1Checksum that)) return false;
        return Objects.equals(id, that.id) &&
                Objects.equals(checksums, that.checksums);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, checksums);
    }

    @Override
    public String toString() {
        return "SHA1Checksum{" +
                "id='" + id + '\'' +
                ", checksums=" + checksums +
                '}';
    }
}
