package rosa.archive.model.aor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * &lt;marginalia id hand method colour marginalia_continues_to marginalia_continues_from
 *      marginalia_to_transcription marginalia_from_transcription book_id internal_ref date
 *      other_reader topic anchor_text&gt;
 *   &lt;language&gt;
 *   &lt;translation&gt;
 * &lt;/marginalia&gt;
 *
 * <h3>Marginalia element</h3>
 * Attributes (all are optional):
 * <ul>
 *   <li>id</li>
 *   <li>hand</li>
 *   <li>method</li>
 *   <li>colour</li>
 *   <li>marginalia_continues_to</li>
 *   <li>marginalia_continues_from</li>
 *   <li>marginalia_from_transcription</li>
 *   <li>marginalia_to_transcription</li>
 *   <li>book_id</li>
 *   <li>internal_ref</li>
 *   <li>date</li>
 *   <li>other_reader</li>
 *   <li>topic</li>
 *   <li>anchor_text</li>
 * </ul>
 *
 * Contains elements:
 * <ul>
 *   <li>language (zero or more) {@link MarginaliaLanguage}</li>
 *   <li>translation (zero or one)</li>
 * </ul>
 */
public class Marginalia extends Annotation implements MultiPart, Serializable {
    private static final long serialVersionUID = 1L;

    private String hand;
    private String date;
    private String otherReader;
    private String topic;
    private String translation;
    private List<MarginaliaLanguage> languages;

    private String marginaliaBefore;
    private String marginaliaAfter;
    private String transcriptBefore;
    private String transcriptAfter;

    private String color;

    public Marginalia() {
        languages = new ArrayList<>();
    }

    public String getHand() {
        return hand;
    }

    public void setHand(String hand) {
        this.hand = hand;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getOtherReader() {
        return otherReader;
    }

    public void setOtherReader(String otherReader) {
        this.otherReader = otherReader;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public String getTranslation() {
        return translation;
    }

    public void setTranslation(String translation) {
        this.translation = translation;
    }

    public List<MarginaliaLanguage> getLanguages() {
        return languages;
    }

    public void setLanguages(List<MarginaliaLanguage> languages) {
        this.languages = languages;
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

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    @Override
    public String toPrettyString() {
        StringBuilder sb = new StringBuilder("<p>");
        sb.append("<strong>Marginalia</strong>");
        sb.append("<p>");

        sb.append("</p>");

        sb.append("</p>");
        return "";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        Marginalia that = (Marginalia) o;

        if (hand != null ? !hand.equals(that.hand) : that.hand != null) return false;
        if (date != null ? !date.equals(that.date) : that.date != null) return false;
        if (otherReader != null ? !otherReader.equals(that.otherReader) : that.otherReader != null) return false;
        if (topic != null ? !topic.equals(that.topic) : that.topic != null) return false;
        if (translation != null ? !translation.equals(that.translation) : that.translation != null) return false;
        if (languages != null ? !languages.equals(that.languages) : that.languages != null) return false;
        if (marginaliaBefore != null ? !marginaliaBefore.equals(that.marginaliaBefore) : that.marginaliaBefore != null)
            return false;
        if (marginaliaAfter != null ? !marginaliaAfter.equals(that.marginaliaAfter) : that.marginaliaAfter != null)
            return false;
        if (transcriptBefore != null ? !transcriptBefore.equals(that.transcriptBefore) : that.transcriptBefore != null)
            return false;
        if (transcriptAfter != null ? !transcriptAfter.equals(that.transcriptAfter) : that.transcriptAfter != null)
            return false;
        return color != null ? color.equals(that.color) : that.color == null;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (hand != null ? hand.hashCode() : 0);
        result = 31 * result + (date != null ? date.hashCode() : 0);
        result = 31 * result + (otherReader != null ? otherReader.hashCode() : 0);
        result = 31 * result + (topic != null ? topic.hashCode() : 0);
        result = 31 * result + (translation != null ? translation.hashCode() : 0);
        result = 31 * result + (languages != null ? languages.hashCode() : 0);
        result = 31 * result + (marginaliaBefore != null ? marginaliaBefore.hashCode() : 0);
        result = 31 * result + (marginaliaAfter != null ? marginaliaAfter.hashCode() : 0);
        result = 31 * result + (transcriptBefore != null ? transcriptBefore.hashCode() : 0);
        result = 31 * result + (transcriptAfter != null ? transcriptAfter.hashCode() : 0);
        result = 31 * result + (color != null ? color.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "Marginalia{" +
                "hand='" + hand + '\'' +
                ", date='" + date + '\'' +
                ", otherReader='" + otherReader + '\'' +
                ", topic='" + topic + '\'' +
                ", translation='" + translation + '\'' +
                ", languages=" + languages +
                ", marginaliaBefore='" + marginaliaBefore + '\'' +
                ", marginaliaAfter='" + marginaliaAfter + '\'' +
                ", transcriptBefore='" + transcriptBefore + '\'' +
                ", transcriptAfter='" + transcriptAfter + '\'' +
                ", color='" + color + '\'' +
                super.toString() + '}';
    }
}
