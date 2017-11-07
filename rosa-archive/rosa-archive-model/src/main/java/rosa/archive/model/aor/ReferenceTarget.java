package rosa.archive.model.aor;

import java.io.Serializable;

/**
 *
 * &lt;target filename="" book_id="" text="" ref="" prefix="" suffix=""/&gt;
 *
 * <h3>target</h3>
 * Attributes:
 * <ul>
 *   <li>filename (deprecated) : target file to link to</li>
 *   <li>book_id (deprecated) : ID of book where the reference points</li>
 *   <li>text (optional) : possible text that is being linked to this book/page </li>
 *   <li>prefix (optional) : prefix for 'text' for disambiguation</li>
 *   <li>suffix (optional) : suffix for 'text' for disambiguation</li>
 *   <li>ref (required) : ID to the referenced object</li>
 * </ul>
 *
 */
public class ReferenceTarget implements Serializable {
    private static final long serialVersionUID = 1L;

    private String filename;
    private String bookId;
    private String text;
    private String targetId;
    private String textPrefix;
    private String textSuffix;

    public ReferenceTarget() {}

    @Deprecated
    public ReferenceTarget(String filename, String bookId, String text) {
        this.filename = filename;
        this.bookId = bookId;
        this.text = text;
    }

    public ReferenceTarget(String ref, String text) {
        this.targetId = ref;
        this.text = text;
    }

    public ReferenceTarget(String targetId, String text, String textPrefix, String textSuffix) {
        this.text = text;
        this.targetId = targetId;
        this.textPrefix = textPrefix;
        this.textSuffix = textSuffix;
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

    public String getTargetId() {
        return targetId;
    }

    public void setTargetId(String targetId) {
        this.targetId = targetId;
    }

    public String getTextPrefix() {
        return textPrefix;
    }

    public void setTextPrefix(String textPrefix) {
        this.textPrefix = textPrefix;
    }

    public String getTextSuffix() {
        return textSuffix;
    }

    public void setTextSuffix(String textSuffix) {
        this.textSuffix = textSuffix;
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
        if (text != null ? !text.equals(that.text) : that.text != null) return false;
        if (targetId != null ? !targetId.equals(that.targetId) : that.targetId != null) return false;
        if (textPrefix != null ? !textPrefix.equals(that.textPrefix) : that.textPrefix != null) return false;
        return textSuffix != null ? textSuffix.equals(that.textSuffix) : that.textSuffix == null;
    }

    @Override
    public int hashCode() {
        int result = filename != null ? filename.hashCode() : 0;
        result = 31 * result + (bookId != null ? bookId.hashCode() : 0);
        result = 31 * result + (text != null ? text.hashCode() : 0);
        result = 31 * result + (targetId != null ? targetId.hashCode() : 0);
        result = 31 * result + (textPrefix != null ? textPrefix.hashCode() : 0);
        result = 31 * result + (textSuffix != null ? textSuffix.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "ReferenceTarget{" +
                "filename='" + filename + '\'' +
                ", bookId='" + bookId + '\'' +
                ", text='" + text + '\'' +
                ", targetId='" + targetId + '\'' +
                ", textPrefix='" + textPrefix + '\'' +
                ", textSuffix='" + textSuffix + '\'' +
                '}';
    }
}
