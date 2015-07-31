package rosa.archive.aor;

import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffEntry.ChangeType;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.TimeZone;

public class GitCommit implements Comparable<GitCommit> {

    public static class Builder {
        public static Builder newBuilder() {
            return new Builder();
        }

        private Builder() {
            this.diffs = new ArrayList<>();
        }

        private String id;
        private String parentCommit;
        private Date date;
        private TimeZone timeZone;
        private String author;
        private String email;
        private String message;
        private List<DiffEntry> diffs;

        public GitCommit build() {
            return new GitCommit(this);
        }

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder parentCommit(String parentCommit) {
            this.parentCommit = parentCommit;
            return this;
        }

        public Builder date(Date date) {
            this.date = date;
            return this;
        }

        public Builder timeZone(TimeZone timeZone) {
            this.timeZone = timeZone;
            return this;
        }

        public Builder author(String author) {
            this.author = author;
            return this;
        }

        public Builder email(String email) {
            this.email = email;
            return this;
        }

        public Builder message(String message) {
            this.message = message;
            return this;
        }

        public Builder diffs(List<DiffEntry> diffs) {
            this.diffs = diffs;
            return this;
        }
    }

    private final SimpleDateFormat ISO_8601_FORMAT;
    private final Calendar calendar;

    final String id;
    final String parentCommit;
    final Date date;
    final TimeZone timeZone;
    final String author;
    final String email;
    final String message;
    final List<DiffEntry> diffs;

    public GitCommit(String id, String parentCommit, Date date, TimeZone timeZone, String author,
                     String email, String message, List<DiffEntry> diffs) {
        this.id = id;
        this.parentCommit = parentCommit;
        this.date = date;
        this.timeZone = timeZone;
        this.author = author;
        this.email = email;
        this.message = message;
        this.diffs = diffs;

        this.calendar = new GregorianCalendar(timeZone);
        calendar.setTime(date);

        ISO_8601_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ssZ");
        ISO_8601_FORMAT.setTimeZone(timeZone);
    }

    private GitCommit(Builder b) {
        this(b.id, b.parentCommit, b.date, b.timeZone, b.author, b.email, b.message, b.diffs);
    }

    public String getISO8601Date() {
        return ISO_8601_FORMAT.format(date);
    }

    public int getFilesCount(ChangeType type) {
        int count = 0;

        for (DiffEntry diff : diffs) {
            if (diff.getChangeType() == type) {
                count++;
            }
        }

        return count;
    }

    public int getFilesChangedForBook(String book, ChangeType type) {
        int count = 0;

        for (DiffEntry diff : diffs) {
            if (diff.getChangeType() != type) {
                continue;
            }

            String file;
            if (type == ChangeType.DELETE) {
                file = diff.getOldPath();
            } else {
                file = diff.getNewPath();
            }

            if (file.startsWith(book)) {
                count++;
            }
        }

        return count;
    }

    @Override
    public int compareTo(GitCommit other) {
        // Order by date, for those commits made on the same day, sort by ID
        if (this.calendar.compareTo(other.calendar) != 0) {
            return this.calendar.compareTo(other.calendar);
        } else if (this.date.compareTo(other.date) != 0) {
            return this.date.compareTo(other.date);
        } else if (this.parentCommit.compareTo(other.parentCommit) != 0) {
            return this.parentCommit.compareTo(other.parentCommit);
        } else if (this.id.compareTo(other.id) != 0) {
            return this.id.compareTo(other.id);
        } else if (this.author.compareTo(other.author) != 0) {
            return this.author.compareTo(other.author);
        } else if (this.email.compareTo(other.email) != 0) {
            return this.email.compareTo(other.email);
        } else {
            return this.message.compareTo(other.message);
        }
    }

    private int compareLists(List<String> list1, List<String> list2) {
        int last = list1.size() < list2.size() ? list1.size() : list2.size();
        for (int i = 0; i < last; i++) {
            if (list1.get(i).compareTo(list2.get(i)) != 0) {
                return list1.get(i).compareTo(list2.get(i));
            }
        }

        return list1.size() - list2.size();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        GitCommit commit = (GitCommit) o;

        if (ISO_8601_FORMAT != null ? !ISO_8601_FORMAT.equals(commit.ISO_8601_FORMAT) : commit.ISO_8601_FORMAT != null)
            return false;
        if (calendar != null ? !calendar.equals(commit.calendar) : commit.calendar != null) return false;
        if (id != null ? !id.equals(commit.id) : commit.id != null) return false;
        if (parentCommit != null ? !parentCommit.equals(commit.parentCommit) : commit.parentCommit != null)
            return false;
        if (date != null ? !date.equals(commit.date) : commit.date != null) return false;
        if (timeZone != null ? !timeZone.equals(commit.timeZone) : commit.timeZone != null) return false;
        if (author != null ? !author.equals(commit.author) : commit.author != null) return false;
        if (email != null ? !email.equals(commit.email) : commit.email != null) return false;
        if (message != null ? !message.equals(commit.message) : commit.message != null) return false;
        return !(diffs != null ? !diffs.equals(commit.diffs) : commit.diffs != null);

    }

    @Override
    public int hashCode() {
        int result = ISO_8601_FORMAT != null ? ISO_8601_FORMAT.hashCode() : 0;
        result = 31 * result + (calendar != null ? calendar.hashCode() : 0);
        result = 31 * result + (id != null ? id.hashCode() : 0);
        result = 31 * result + (parentCommit != null ? parentCommit.hashCode() : 0);
        result = 31 * result + (date != null ? date.hashCode() : 0);
        result = 31 * result + (timeZone != null ? timeZone.hashCode() : 0);
        result = 31 * result + (author != null ? author.hashCode() : 0);
        result = 31 * result + (email != null ? email.hashCode() : 0);
        result = 31 * result + (message != null ? message.hashCode() : 0);
        result = 31 * result + (diffs != null ? diffs.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "GitCommit{" +
                ", id='" + id + '\'' +
                ", parentCommit='" + parentCommit + '\'' +
                ", date=" + date +
                ", timeZone=" + timeZone +
                ", author='" + author + '\'' +
                ", email='" + email + '\'' +
                ", message='" + message + '\'' +
                ", diffs=" + diffs +
                '}';
    }
}
