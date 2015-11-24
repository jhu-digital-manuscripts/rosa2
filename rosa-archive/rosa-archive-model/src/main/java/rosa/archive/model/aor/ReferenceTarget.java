package rosa.archive.model.aor;

import java.io.Serializable;

/**
 *
 * &lt;target filename="" book_id="" text="" /&gt;
 *
 * <h3>
 * target
 * </h3>
 * <p>
 * Attributes:
 * <ul>
 * <li>filename (required) : target file to link to</li>
 * <li>book_id (required) : ID of book where the reference points</li>
 * <li>text (required) : ?? </li>
 * </ul>
 * </p>
 *
 */
public class ReferenceTarget implements Serializable {
    private static final long serialVersionUID = 1L;

    private String filename;
    private String bookId;
    private String text;

    public ReferenceTarget() {}

    public ReferenceTarget(String filename, String bookId, String text) {
        this.filename = filename;
        this.bookId = bookId;
        this.text = text;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getBookId() {
        return bookId;
    }

    public void setBookId(String bookId) {
        this.bookId = bookId;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ReferenceTarget that = (ReferenceTarget) o;

        if (filename != null ? !filename.equals(that.filename) : that.filename != null) return false;
        if (bookId != null ? !bookId.equals(that.bookId) : that.bookId != null) return false;
        return !(text != null ? !text.equals(that.text) : that.text != null);

    }

    @Override
    public int hashCode() {
        int result = filename != null ? filename.hashCode() : 0;
        result = 31 * result + (bookId != null ? bookId.hashCode() : 0);
        result = 31 * result + (text != null ? text.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "ReferenceTarget{" +
                "filename='" + filename + '\'' +
                ", bookId='" + bookId + '\'' +
                ", text='" + text + '\'' +
                '}';
    }
}
