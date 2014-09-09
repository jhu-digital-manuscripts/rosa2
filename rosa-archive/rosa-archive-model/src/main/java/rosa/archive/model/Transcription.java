package rosa.archive.model;

import java.io.Serializable;

/**
 *
 */
public class Transcription implements HasId, Serializable {
    private static final long serialVersionUID = 1L;

    private String id;
    private String content;

    public Transcription() {  }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Transcription)) return false;

        Transcription that = (Transcription) o;

        if (content != null ? !content.equals(that.content) : that.content != null) return false;
        if (id != null ? !id.equals(that.id) : that.id != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (content != null ? content.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "Transcription{" +
                "id='" + id + '\'' +
                ", content='" + content + '\'' +
                '}';
    }
}
