package rosa.archive.model.aor;

import java.io.Serializable;

public class GraphNote implements Serializable {
    private static final long serialVersionUID = 1L;

    public final String id;
    public final String hand;
    public final String language;
    public final String internalLink;
    public final String anchorText;
    public final String content;

    public GraphNote(String id, String hand, String language, String content) {
        this(id, hand, language, null, null, content);
    }

    public GraphNote(String id, String hand, String language, String internalLink, String anchorText, String content) {
        this.id = id;
        this.hand = hand;
        this.language = language;
        this.internalLink = internalLink;
        this.anchorText = anchorText;
        this.content = content;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        GraphNote graphNote = (GraphNote) o;

        if (id != null ? !id.equals(graphNote.id) : graphNote.id != null) return false;
        if (hand != null ? !hand.equals(graphNote.hand) : graphNote.hand != null) return false;
        if (language != null ? !language.equals(graphNote.language) : graphNote.language != null) return false;
        if (internalLink != null ? !internalLink.equals(graphNote.internalLink) : graphNote.internalLink != null)
            return false;
        if (anchorText != null ? !anchorText.equals(graphNote.anchorText) : graphNote.anchorText != null) return false;
        return content != null ? content.equals(graphNote.content) : graphNote.content == null;
    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (hand != null ? hand.hashCode() : 0);
        result = 31 * result + (language != null ? language.hashCode() : 0);
        result = 31 * result + (internalLink != null ? internalLink.hashCode() : 0);
        result = 31 * result + (anchorText != null ? anchorText.hashCode() : 0);
        result = 31 * result + (content != null ? content.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "GraphNote{" +
                "id='" + id + '\'' +
                ", hand='" + hand + '\'' +
                ", language='" + language + '\'' +
                ", internalLink='" + internalLink + '\'' +
                ", anchorText='" + anchorText + '\'' +
                ", content='" + content + '\'' +
                '}';
    }
}
