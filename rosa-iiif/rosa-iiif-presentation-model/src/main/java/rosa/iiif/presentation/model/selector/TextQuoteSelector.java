package rosa.iiif.presentation.model.selector;

public class TextQuoteSelector implements Selector {
    private String text;
    private String pre;
    private String post;

    public TextQuoteSelector(String text) {
        this.text = text;
    }

    public TextQuoteSelector(String text, String pre, String post) {
        this.text = text;
        this.pre = pre;
        this.post = post;
    }

    public String getText() {
        return text;
    }

    public String getPre() {
        return pre;
    }

    public String getPost() {
        return post;
    }

    @Override
    public String context() {
        return "http://www.w3.org/ns/anno.jsonld";
    }

    @Override
    public String type() {
        return "TextQuoteSelector";
    }

    @Override
    public String content() {
        return (hasStuff(pre) ? pre : "") +
                text +
                (hasStuff(post) ? post : "");
    }

    public boolean hasContent() {
        return hasStuff(pre) || hasStuff(text) || hasStuff(post);
    }

    private boolean hasStuff(String s) {
        return s != null && !s.isEmpty();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TextQuoteSelector that = (TextQuoteSelector) o;

        if (text != null ? !text.equals(that.text) : that.text != null) return false;
        if (pre != null ? !pre.equals(that.pre) : that.pre != null) return false;
        return post != null ? post.equals(that.post) : that.post == null;
    }

    @Override
    public int hashCode() {
        int result = text != null ? text.hashCode() : 0;
        result = 31 * result + (pre != null ? pre.hashCode() : 0);
        result = 31 * result + (post != null ? post.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "TextQuoteSelector{" +
                "text='" + text + '\'' +
                ", pre='" + pre + '\'' +
                ", post='" + post + '\'' +
                '}';
    }
}
