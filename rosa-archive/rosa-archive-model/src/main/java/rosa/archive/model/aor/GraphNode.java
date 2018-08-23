package rosa.archive.model.aor;

import java.io.Serializable;

public class GraphNode implements Serializable {
    private static final long serialVersionUID = 1L;

    private String id;
    private String person;
    private String text;
    private String content;

    public GraphNode(String id) {
        this(id, null, null, null);
    }

    public GraphNode(String id, String person, String text, String content) {
        this.id = id;
        this.person = person;
        this.text = text;
        this.content = content;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getPerson() {
        return person;
    }

    public void setPerson(String person) {
        this.person = person;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
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
        if (o == null || getClass() != o.getClass()) return false;

        GraphNode graphNode = (GraphNode) o;

        if (id != null ? !id.equals(graphNode.id) : graphNode.id != null) return false;
        if (person != null ? !person.equals(graphNode.person) : graphNode.person != null) return false;
        if (text != null ? !text.equals(graphNode.text) : graphNode.text != null) return false;
        return content != null ? content.equals(graphNode.content) : graphNode.content == null;
    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (person != null ? person.hashCode() : 0);
        result = 31 * result + (text != null ? text.hashCode() : 0);
        result = 31 * result + (content != null ? content.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "GraphNode{" +
                "id='" + id + '\'' +
                ", person='" + person + '\'' +
                ", text='" + text + '\'' +
                ", content='" + content + '\'' +
                '}';
    }
}
