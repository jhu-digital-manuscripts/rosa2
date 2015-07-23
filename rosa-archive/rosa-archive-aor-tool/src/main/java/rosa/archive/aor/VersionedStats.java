package rosa.archive.aor;

public class VersionedStats extends Stats {
    final String commitId;

    public VersionedStats(String id, String commitId) {
        super(id);
        this.commitId = commitId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        VersionedStats that = (VersionedStats) o;

        return !(commitId != null ? !commitId.equals(that.commitId) : that.commitId != null);

    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (commitId != null ? commitId.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "VersionedStats{" +
                "commitId='" + commitId + '\'' +
                super.toString() +
                '}';
    }
}
