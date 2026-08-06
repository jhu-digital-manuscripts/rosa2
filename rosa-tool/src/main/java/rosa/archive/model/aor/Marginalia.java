package rosa.archive.model.aor;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * A marginalia annotation representing handwritten notes in the margins of a page.
 *
 * <p>Marginalia may contain multiple language sections, each with positional data
 * including text, symbols, people, books, and locations referenced.</p>
 */
public final class Marginalia extends AbstractAnnotation implements MultiPart {

    private String hand;
    private String date;
    private String otherReader;
    private String topic;
    private String translation;
    private List<MarginaliaLanguage> languages;
    private String bookId;
    private String marginaliaBefore;
    private String marginaliaAfter;
    private String transcriptBefore;
    private String transcriptAfter;
    private String color;

    /**
     * Creates an empty marginalia annotation.
     */
    public Marginalia() {
        languages = new ArrayList<>();
    }

    /**
     * Returns the hand (writer) of this marginalia.
     *
     * @return the hand identifier, or null
     */
    public String getHand() {
        return hand;
    }

    /**
     * Sets the hand.
     *
     * @param hand the hand identifier
     */
    public void setHand(String hand) {
        this.hand = hand;
    }

    /**
     * Returns the date associated with this marginalia.
     *
     * @return the date string, or null
     */
    public String getDate() {
        return date;
    }

    /**
     * Sets the date.
     *
     * @param date the date string
     */
    public void setDate(String date) {
        this.date = date;
    }

    /**
     * Returns an alternative reader attribution.
     *
     * @return the other reader name, or null
     */
    public String getOtherReader() {
        return otherReader;
    }

    /**
     * Sets the other reader.
     *
     * @param otherReader the other reader name
     */
    public void setOtherReader(String otherReader) {
        this.otherReader = otherReader;
    }

    /**
     * Returns the topic of this marginalia.
     *
     * @return the topic, or null
     */
    public String getTopic() {
        return topic;
    }

    /**
     * Sets the topic.
     *
     * @param topic the topic
     */
    public void setTopic(String topic) {
        this.topic = topic;
    }

    /**
     * Returns the translation of this marginalia.
     *
     * @return the translation text, or null
     */
    public String getTranslation() {
        return translation;
    }

    /**
     * Sets the translation.
     *
     * @param translation the translation text
     */
    public void setTranslation(String translation) {
        this.translation = translation;
    }

    /**
     * Returns the language sections of this marginalia.
     *
     * @return the list of language sections (never null)
     */
    public List<MarginaliaLanguage> getLanguages() {
        return languages;
    }

    /**
     * Sets the language sections.
     *
     * @param languages the language sections
     */
    public void setLanguages(List<MarginaliaLanguage> languages) {
        this.languages = languages;
    }

    /**
     * Returns the associated book identifier.
     *
     * @return the book ID, or null
     */
    public String getBookId() {
        return bookId;
    }

    /**
     * Sets the book identifier.
     *
     * @param bookId the book ID
     */
    public void setBookId(String bookId) {
        this.bookId = bookId;
    }

    /**
     * Returns the color of this marginalia.
     *
     * @return the color, or null
     */
    public String getColor() {
        return color;
    }

    /**
     * Sets the color.
     *
     * @param color the color
     */
    public void setColor(String color) {
        this.color = color;
    }

    @Override
    public String getContinuesTo() {
        return marginaliaAfter;
    }

    @Override
    public String getContinuesFrom() {
        return marginaliaBefore;
    }

    @Override
    public String getToTranscription() {
        return transcriptAfter;
    }

    @Override
    public String getFromTranscription() {
        return transcriptBefore;
    }

    @Override
    public void setContinuesTo(String continuesTo) {
        marginaliaAfter = continuesTo;
    }

    @Override
    public void setContinuesFrom(String continuesFrom) {
        marginaliaBefore = continuesFrom;
    }

    @Override
    public void setToTranscription(String toTranscription) {
        transcriptAfter = toTranscription;
    }

    @Override
    public void setFromTranscription(String fromTranscription) {
        transcriptBefore = fromTranscription;
    }

    @Override
    public String toPrettyString() {
        return "Marginalia";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Marginalia that)) return false;
        if (!super.equals(o)) return false;
        return Objects.equals(hand, that.hand) &&
                Objects.equals(date, that.date) &&
                Objects.equals(otherReader, that.otherReader) &&
                Objects.equals(topic, that.topic) &&
                Objects.equals(translation, that.translation) &&
                Objects.equals(languages, that.languages) &&
                Objects.equals(bookId, that.bookId) &&
                Objects.equals(marginaliaBefore, that.marginaliaBefore) &&
                Objects.equals(marginaliaAfter, that.marginaliaAfter) &&
                Objects.equals(transcriptBefore, that.transcriptBefore) &&
                Objects.equals(transcriptAfter, that.transcriptAfter) &&
                Objects.equals(color, that.color);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), hand, date, otherReader, topic, translation,
                languages, bookId, marginaliaBefore, marginaliaAfter,
                transcriptBefore, transcriptAfter, color);
    }

    @Override
    public String toString() {
        return "Marginalia{" +
                "hand='" + hand + '\'' +
                ", date='" + date + '\'' +
                ", otherReader='" + otherReader + '\'' +
                ", topic='" + topic + '\'' +
                ", translation='" + translation + '\'' +
                ", languages=" + languages.size() +
                ", bookId='" + bookId + '\'' +
                ", color='" + color + '\'' +
                '}';
    }
}
