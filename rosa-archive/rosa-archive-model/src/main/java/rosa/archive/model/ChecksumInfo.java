package rosa.archive.model;

import com.google.gwt.user.client.rpc.IsSerializable;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Holds the checksums for all items in the archive for a book.
 */
public class ChecksumInfo implements IsSerializable {

    /**
     * Archive item ID -> ChecksumData
     */
    private Map<String, ChecksumData> checksums;

    public ChecksumInfo() {
        this.checksums = new HashMap<>();
    }

    public Set<String> getAllIds() {
        return checksums.keySet();
    }

    public ChecksumData getChecksumDataForId(String id) {
        return checksums.get(id);
    }

    public void addChecksum(ChecksumData checksum) {
        checksums.put(checksum.getId(), checksum);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ChecksumInfo)) return false;

        ChecksumInfo that = (ChecksumInfo) o;

        if (!checksums.equals(that.checksums)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return checksums.hashCode();
    }

    @Override
    public String toString() {
        return "ChecksumInfo{" +
                "checksums=" + checksums +
                '}';
    }
}
