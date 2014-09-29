package rosa.archive.model;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Holds the checksums for all items in the archive for a book.
 */
public class ChecksumInfo implements HasId, Serializable {
    private static final long serialVersionUID = 1L;

    private String id;
    /**
     * Archive item ID -> checksum
     */
    private Map<String, String> checksums;

    public ChecksumInfo() {
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

    public Set<String> getAllIds() {
        return checksums.keySet();
    }

    public Map<String, String> checksums() {
        return checksums;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ChecksumInfo)) return false;

        ChecksumInfo info = (ChecksumInfo) o;

        if (checksums != null ? !checksums.equals(info.checksums) : info.checksums != null) return false;
        if (id != null ? !id.equals(info.id) : info.id != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (checksums != null ? checksums.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "ChecksumInfo{" +
                "id='" + id + '\'' +
                ", checksums=" + checksums +
                '}';
    }
}
