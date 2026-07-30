package rosa.archive.model;

import java.util.Objects;

/**
 * A transcription of a book, stored as raw XML content.
 */
public class Transcription implements HasId {

    private String id;
    private String content;

    /**
     * Creates an empty Transcription.
     */
    public Transcription() {}

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Returns the transcription XML content.
     *
     * @return the XML content, or {@code null} if not set
     */
    public String getXML() {
        return content;
    }

    /**
     * Sets the transcription XML content.
     *
     * @param xml the XML content
     */
    public void setXML(String xml) {
        this.content = xml;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Transcription that)) return false;
        return Objects.equals(id, that.id) &&
                Objects.equals(content, that.content);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, content);
    }

    @Override
    public String toString() {
        return "Transcription{" +
                "id='" + id + '\'' +
                ", content='" + content + '\'' +
                '}';
    }
}
