package rosa.archive.model.aor;

/**
 * Element representing a bit of text
 */
public class TextEl {

    private final String hand;
    private final String language;
    private final String anchor_text;

    public TextEl(String hand, String language) {
        this(hand, language, null);
    }

    public TextEl(String hand, String language, String anchor_text) {
        this.hand = hand;
        this.language = language;
        this.anchor_text = anchor_text;
    }

    public String getHand() {
        return hand;
    }

    public String getLanguage() {
        return language;
    }

    public String getAnchor_text() {
        return anchor_text;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TextEl textEl = (TextEl) o;

        if (hand != null ? !hand.equals(textEl.hand) : textEl.hand != null) return false;
        if (language != null ? !language.equals(textEl.language) : textEl.language != null) return false;
        return anchor_text != null ? anchor_text.equals(textEl.anchor_text) : textEl.anchor_text == null;
    }

    @Override
    public int hashCode() {
        int result = hand != null ? hand.hashCode() : 0;
        result = 31 * result + (language != null ? language.hashCode() : 0);
        result = 31 * result + (anchor_text != null ? anchor_text.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "TextEl{" +
                "hand='" + hand + '\'' +
                ", language='" + language + '\'' +
                ", anchor_text='" + anchor_text + '\'' +
                '}';
    }
}
