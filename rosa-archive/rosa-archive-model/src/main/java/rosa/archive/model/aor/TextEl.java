package rosa.archive.model.aor;

import java.util.Objects;

/**
 * Element representing a bit of text
 */
public class TextEl {

    private final String hand;
    private final String language;
    private final String anchor_text;
    private final String text;

    public TextEl(String hand, String language) {
        this(hand, language, null, null);
    }

    public TextEl(String hand, String language, String anchor_text, String text) {
        this.hand = hand;
        this.language = language;
        this.anchor_text = anchor_text;
        this.text = text;
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

    public String getText() {
        return text;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TextEl textEl = (TextEl) o;
        return Objects.equals(hand, textEl.hand) &&
                Objects.equals(language, textEl.language) &&
                Objects.equals(anchor_text, textEl.anchor_text) &&
                Objects.equals(text, textEl.text);
    }

    @Override
    public int hashCode() {

        return Objects.hash(hand, language, anchor_text, text);
    }

    @Override
    public String toString() {
        return "TextEl{" + "hand='" + hand + '\'' + ", language='" + language + '\'' +
                ", anchor_text='" + anchor_text + '\'' + ", text='" + text + '\'' + '}';
    }
}
