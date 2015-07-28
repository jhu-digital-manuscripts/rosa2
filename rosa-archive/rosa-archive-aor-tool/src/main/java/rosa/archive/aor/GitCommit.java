package rosa.archive.aor;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

public class GitCommit implements Comparable<GitCommit> {
    private final SimpleDateFormat ISO_8601_FORMAT;

    final String id;
    final Date date;
    final TimeZone timeZone;
    final String author;
    final String email;
    final String message;
    final Calendar calendar;

    public GitCommit(String id, Date date, TimeZone timeZone, String author, String email, String message) {
        this.id = id;
        this.date = date;
        this.timeZone = timeZone;
        this.author = author;
        this.email = email;
        this.message = message;
        this.calendar = new GregorianCalendar(timeZone);
        calendar.setTime(date);

        ISO_8601_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ssZ");
        ISO_8601_FORMAT.setTimeZone(timeZone);
    }

    public String getISO8601Date() {
        return ISO_8601_FORMAT.format(date);
    }

    @Override
    public int compareTo(GitCommit other) {
        // Order by date, for those commits made on the same day, sort by ID
        if (this.calendar.compareTo(other.calendar) != 0) {
            return this.calendar.compareTo(other.calendar);
        }

        if (this.date.compareTo(other.date) != 0) {
            return this.date.compareTo(other.date);
        }

        if (this.id.compareTo(other.id) != 0) {
            return this.id.compareTo(other.id);
        }



        return 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        GitCommit commit = (GitCommit) o;

        if (ISO_8601_FORMAT != null ? !ISO_8601_FORMAT.equals(commit.ISO_8601_FORMAT) : commit.ISO_8601_FORMAT != null)
            return false;
        if (id != null ? !id.equals(commit.id) : commit.id != null) return false;
        if (date != null ? !date.equals(commit.date) : commit.date != null) return false;
        if (timeZone != null ? !timeZone.equals(commit.timeZone) : commit.timeZone != null) return false;
        if (author != null ? !author.equals(commit.author) : commit.author != null) return false;
        if (email != null ? !email.equals(commit.email) : commit.email != null) return false;
        if (message != null ? !message.equals(commit.message) : commit.message != null) return false;
        return !(calendar != null ? !calendar.equals(commit.calendar) : commit.calendar != null);

    }

    @Override
    public int hashCode() {
        int result = ISO_8601_FORMAT != null ? ISO_8601_FORMAT.hashCode() : 0;
        result = 31 * result + (id != null ? id.hashCode() : 0);
        result = 31 * result + (date != null ? date.hashCode() : 0);
        result = 31 * result + (timeZone != null ? timeZone.hashCode() : 0);
        result = 31 * result + (author != null ? author.hashCode() : 0);
        result = 31 * result + (email != null ? email.hashCode() : 0);
        result = 31 * result + (message != null ? message.hashCode() : 0);
        result = 31 * result + (calendar != null ? calendar.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "GitCommit{" +
                "ISO_8601_FORMAT=" + ISO_8601_FORMAT +
                ", id='" + id + '\'' +
                ", date=" + date +
                ", timeZone=" + timeZone +
                ", author='" + author + '\'' +
                ", email='" + email + '\'' +
                ", message='" + message + '\'' +
                ", calendar=" + calendar +
                '}';
    }
}
