package rosa.archive.model;

/**
 *
 */
public class Transcription {

    private String content;

    public Transcription() {  }

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

        return true;
    }

    @Override
    public int hashCode() {
        return content != null ? content.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "Transcription{" +
                "content='" + content + '\'' +
                '}';
    }
}
