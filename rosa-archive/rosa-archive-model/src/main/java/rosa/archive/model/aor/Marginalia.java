package rosa.archive.model.aor;

import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class Marginalia extends Annotation {

    public class Language {
        String lang;
        List<Position> positions;

        public Language() {
            positions = new ArrayList<>();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Language language = (Language) o;

            if (lang != null ? !lang.equals(language.lang) : language.lang != null) return false;
            if (positions != null ? !positions.equals(language.positions) : language.positions != null) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = lang != null ? lang.hashCode() : 0;
            result = 31 * result + (positions != null ? positions.hashCode() : 0);
            return result;
        }

        @Override
        public String toString() {
            return "Language{" +
                    "lang='" + lang + '\'' +
                    ", positions=" + positions +
                    '}';
        }
    }

    public class Position {
        String text;
        String place;
        int orientation;
        List<String> people;
        List<String> books;
        List<String> locations;
        List<Underline> emphasis;

        public Position() {
            people = new ArrayList<>();
            books = new ArrayList<>();
            locations = new ArrayList<>();
            emphasis = new ArrayList<>();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Position position = (Position) o;

            if (orientation != position.orientation) return false;
            if (books != null ? !books.equals(position.books) : position.books != null) return false;
            if (emphasis != null ? !emphasis.equals(position.emphasis) : position.emphasis != null) return false;
            if (locations != null ? !locations.equals(position.locations) : position.locations != null) return false;
            if (people != null ? !people.equals(position.people) : position.people != null) return false;
            if (place != null ? !place.equals(position.place) : position.place != null) return false;
            if (text != null ? !text.equals(position.text) : position.text != null) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = text != null ? text.hashCode() : 0;
            result = 31 * result + (place != null ? place.hashCode() : 0);
            result = 31 * result + orientation;
            result = 31 * result + (people != null ? people.hashCode() : 0);
            result = 31 * result + (books != null ? books.hashCode() : 0);
            result = 31 * result + (locations != null ? locations.hashCode() : 0);
            result = 31 * result + (emphasis != null ? emphasis.hashCode() : 0);
            return result;
        }

        @Override
        public String toString() {
            return "Position{" +
                    "text='" + text + '\'' +
                    ", place='" + place + '\'' +
                    ", orientation=" + orientation +
                    ", people=" + people +
                    ", books=" + books +
                    ", locations=" + locations +
                    ", emphasis=" + emphasis +
                    '}';
        }
    }

    private String hand;
    private String date;
    private String otherReader;
    private String topic;
    private String anchorText;
    private String translation;
    private List<Language> languages;

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

    public String getAnchorText() {
        return anchorText;
    }

    public void setAnchorText(String anchorText) {
        this.anchorText = anchorText;
    }

    public String getTranslation() {
        return translation;
    }

    public void setTranslation(String translation) {
        this.translation = translation;
    }

    public List<Language> getLanguages() {
        return languages;
    }

    public void setLanguages(List<Language> languages) {
        this.languages = languages;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        Marginalia that = (Marginalia) o;

        if (anchorText != null ? !anchorText.equals(that.anchorText) : that.anchorText != null) return false;
        if (date != null ? !date.equals(that.date) : that.date != null) return false;
        if (hand != null ? !hand.equals(that.hand) : that.hand != null) return false;
        if (languages != null ? !languages.equals(that.languages) : that.languages != null) return false;
        if (otherReader != null ? !otherReader.equals(that.otherReader) : that.otherReader != null) return false;
        if (topic != null ? !topic.equals(that.topic) : that.topic != null) return false;
        if (translation != null ? !translation.equals(that.translation) : that.translation != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (hand != null ? hand.hashCode() : 0);
        result = 31 * result + (date != null ? date.hashCode() : 0);
        result = 31 * result + (otherReader != null ? otherReader.hashCode() : 0);
        result = 31 * result + (topic != null ? topic.hashCode() : 0);
        result = 31 * result + (anchorText != null ? anchorText.hashCode() : 0);
        result = 31 * result + (translation != null ? translation.hashCode() : 0);
        result = 31 * result + (languages != null ? languages.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "Marginalia{" +
                "hand='" + hand + '\'' +
                ", date='" + date + '\'' +
                ", otherReader='" + otherReader + '\'' +
                ", topic='" + topic + '\'' +
                ", anchorText='" + anchorText + '\'' +
                ", translation='" + translation + '\'' +
                ", languages=" + languages +
                '}';
    }
}
