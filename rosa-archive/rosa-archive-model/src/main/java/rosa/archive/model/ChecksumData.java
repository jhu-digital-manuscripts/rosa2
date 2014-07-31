package rosa.archive.model;

import com.google.gwt.user.client.rpc.IsSerializable;

public class ChecksumData implements IsSerializable {

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

        ChecksumData that = (ChecksumData) o;

        if (algorithm != that.algorithm) return false;
        if (hash != null ? !hash.equals(that.hash) : that.hash != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = algorithm != null ? algorithm.hashCode() : 0;
        result = 31 * result + (hash != null ? hash.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "ChecksumData{" +
                "algorithm=" + algorithm +
                ", hash='" + hash + '\'' +
                '}';
    }
}
