package rosa.archive.aor;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public class GitCommit implements Comparable<GitCommit> {
    private static final SimpleDateFormat ISO_8601_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ssZ");

    final String id;
    final Date date;
    final TimeZone timeZone;
    final String author;
    final String message;

    public GitCommit(String id, Date date, TimeZone timeZone, String author, String message) {
        this.id = id;
        this.date = date;
        this.timeZone = timeZone;
        this.author = author;
        this.message = message;
    }

    public String getISO8601Date() {
        return ISO_8601_FORMAT.format(date);
    }

    @Override
    public int compareTo(GitCommit other) {
        // Order by date, for those commits made on the same day, sort by ID
        int dateCompare = this.date.compareTo(other.date);
        return dateCompare != 0 ? dateCompare : this.id.compareTo(other.id);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        GitCommit commit = (GitCommit) o;

        if (id != null ? !id.equals(commit.id) : commit.id != null) return false;
        if (date != null ? !date.equals(commit.date) : commit.date != null) return false;
        if (timeZone != null ? !timeZone.equals(commit.timeZone) : commit.timeZone != null) return false;
        if (author != null ? !author.equals(commit.author) : commit.author != null) return false;
        return !(message != null ? !message.equals(commit.message) : commit.message != null);

    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (date != null ? date.hashCode() : 0);
        result = 31 * result + (timeZone != null ? timeZone.hashCode() : 0);
        result = 31 * result + (author != null ? author.hashCode() : 0);
        result = 31 * result + (message != null ? message.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "GitCommit{" +
                "id='" + id + '\'' +
                ", date=" + date +
                ", timeZone=" + timeZone +
                ", author='" + author + '\'' +
                ", message='" + message + '\'' +
                '}';
    }
}
