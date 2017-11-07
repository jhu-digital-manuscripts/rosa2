package rosa.archive.model.aor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * graph_text element
 *
 * &lt;graph_text&gt;
 *   &lt;note id hand language internal_link anchor_text&gt;&lt;/note&gt;
 *   &lt;person /&gt;
 *   &lt;book /&gt;
 *   &lt;location /&gt;
 *   &lt;symbol_in_text /&gt;
 *   &lt;translation /&gt;
 * &lt;/graph_text&gt;
 *
 * Has no attributes.
 *
 * Contains elements:
 * <ul>
 *   <li>note (zero or more) : {@link Note}</li>
 *   <li>person (zero or more)</li>
 *   <li>book (zero or more)</li>
 *   <li>location (zero or more)</li>
 *   <li>symbol_in_text (zero or more)</li>
 *   <li>translation (zero or more)</li>
 * </ul>
 */
public class GraphText implements Serializable {
    private static final long serialVersionUID = 1L;

    public class Note implements Serializable {
        private static final long serialVersionUID = 1L;

        public final String id;
        public final String hand;
        public final String language;
        public final String internalLink;
        public final String anchorText;
        public final String content;

        public Note(String id, String hand, String language, String content) {
            this(id, hand, language, null, null, content);
        }

        public Note(String id, String hand, String language, String internalLink, String anchorText, String content) {
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

            Note note = (Note) o;

            if (id != null ? !id.equals(note.id) : note.id != null) return false;
            if (hand != null ? !hand.equals(note.hand) : note.hand != null) return false;
            if (language != null ? !language.equals(note.language) : note.language != null) return false;
            if (internalLink != null ? !internalLink.equals(note.internalLink) : note.internalLink != null)
                return false;
            return anchorText != null ? anchorText.equals(note.anchorText) : note.anchorText == null;
        }

        @Override
        public int hashCode() {
            int result = id != null ? id.hashCode() : 0;
            result = 31 * result + (hand != null ? hand.hashCode() : 0);
            result = 31 * result + (language != null ? language.hashCode() : 0);
            result = 31 * result + (internalLink != null ? internalLink.hashCode() : 0);
            result = 31 * result + (anchorText != null ? anchorText.hashCode() : 0);
            return result;
        }

        @Override
        public String toString() {
            return "Note{" +
                    "id='" + id + '\'' +
                    ", hand='" + hand + '\'' +
                    ", language='" + language + '\'' +
                    ", internalLink='" + internalLink + '\'' +
                    ", anchorText='" + anchorText + '\'' +
                    '}';
        }
    }

    private List<Note> notes;
    private List<String> people;
    private List<String> books;
    private List<String> locations;
    private List<String> symbols;
    private List<String> translations;

    public GraphText() {
        this.notes = new ArrayList<>();
        this.people = new ArrayList<>();
        this.books = new ArrayList<>();
        this.locations = new ArrayList<>();
        this.symbols = new ArrayList<>();
        this.translations = new ArrayList<>();
    }

    public List<Note> getNotes() {
        return notes;
    }

    public void setNotes(List<Note> notes) {
        this.notes = notes;
    }

    public void addNote(Note note) {
        notes.add(note);
    }

    public List<String> getPeople() {
        return people;
    }

    public void setPeople(List<String> people) {
        this.people = people;
    }

    public void addPerson(String person) {
        people.add(person);
    }

    public List<String> getBooks() {
        return books;
    }

    public void setBooks(List<String> books) {
        this.books = books;
    }

    public void addBook(String book) {
        books.add(book);
    }

    public List<String> getLocations() {
        return locations;
    }

    public void setLocations(List<String> locations) {
        this.locations = locations;
    }

    public void addLocation(String loc) {
        locations.add(loc);
    }

    public List<String> getSymbols() {
        return symbols;
    }

    public void setSymbols(List<String> symbols) {
        this.symbols = symbols;
    }

    public void addSymbol(String symbol) {
        symbols.add(symbol);
    }

    public List<String> getTranslations() {
        return translations;
    }

    public void setTranslations(List<String> translations) {
        this.translations = translations;
    }

    public void addTranslation(String translation) {
        translations.add(translation);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        GraphText graphText = (GraphText) o;

        if (notes != null ? !notes.equals(graphText.notes) : graphText.notes != null) return false;
        if (people != null ? !people.equals(graphText.people) : graphText.people != null) return false;
        if (books != null ? !books.equals(graphText.books) : graphText.books != null) return false;
        if (locations != null ? !locations.equals(graphText.locations) : graphText.locations != null) return false;
        if (symbols != null ? !symbols.equals(graphText.symbols) : graphText.symbols != null) return false;
        return translations != null ? translations.equals(graphText.translations) : graphText.translations == null;
    }

    @Override
    public int hashCode() {
        int result = notes != null ? notes.hashCode() : 0;
        result = 31 * result + (people != null ? people.hashCode() : 0);
        result = 31 * result + (books != null ? books.hashCode() : 0);
        result = 31 * result + (locations != null ? locations.hashCode() : 0);
        result = 31 * result + (symbols != null ? symbols.hashCode() : 0);
        result = 31 * result + (translations != null ? translations.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "GraphText{" +
                "notes=" + notes +
                ", people=" + people +
                ", books=" + books +
                ", locations=" + locations +
                ", symbols=" + symbols +
                ", translations=" + translations +
                '}';
    }
}
