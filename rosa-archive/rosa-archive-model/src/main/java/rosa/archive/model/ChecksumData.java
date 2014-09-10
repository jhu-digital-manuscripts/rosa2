package rosa.archive.model;

import java.io.Serializable;

/**
 * Checksum data for an item in the archive, including the hash algorithm and hash itself.
 */
public class ChecksumData implements Serializable {
    private static final long serialVersionUID = 1L;

    private String id;
    private HashAlgorithm algorithm;
    private String hash;

    public ChecksumData() {  }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public HashAlgorithm getAlgorithm() {
        return algorithm;
    }

    public void setAlgorithm(HashAlgorithm algorithm) {
        this.algorithm = algorithm;
    }

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ChecksumData)) return false;

        ChecksumData data = (ChecksumData) o;

        if (algorithm != data.algorithm) return false;
        if (hash != null ? !hash.equals(data.hash) : data.hash != null) return false;
        if (id != null ? !id.equals(data.id) : data.id != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (algorithm != null ? algorithm.hashCode() : 0);
        result = 31 * result + (hash != null ? hash.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "ChecksumData{" +
                "id='" + id + '\'' +
                ", algorithm=" + algorithm +
                ", hash='" + hash + '\'' +
                '}';
    }
}
