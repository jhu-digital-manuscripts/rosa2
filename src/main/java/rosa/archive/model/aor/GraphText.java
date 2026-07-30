package rosa.archive.model.aor;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * A text element within a graph annotation, containing notes, people, books,
 * locations, symbols, and translations.
 */
public final class GraphText {

    private List<GraphNote> notes;
    private List<String> people;
    private List<String> books;
    private List<String> locations;
    private List<String> symbols;
    private List<String> translations;

    /**
     * Creates an empty graph text element.
     */
    public GraphText() {
        this.notes = new ArrayList<>();
        this.people = new ArrayList<>();
        this.books = new ArrayList<>();
        this.locations = new ArrayList<>();
        this.symbols = new ArrayList<>();
        this.translations = new ArrayList<>();
    }

    /**
     * Returns the notes in this graph text.
     *
     * @return the notes (never null)
     */
    public List<GraphNote> getNotes() {
        return notes;
    }

    /**
     * Sets the notes.
     *
     * @param notes the notes
     */
    public void setNotes(List<GraphNote> notes) {
        this.notes = notes;
    }

    /**
     * Adds a note to this graph text.
     *
     * @param note the note to add
     */
    public void addNote(GraphNote note) {
        notes.add(note);
    }

    /**
     * Returns the people referenced.
     *
     * @return the people (never null)
     */
    public List<String> getPeople() {
        return people;
    }

    /**
     * Sets the people list.
     *
     * @param people the people
     */
    public void setPeople(List<String> people) {
        this.people = people;
    }

    /**
     * Adds a person reference.
     *
     * @param person the person name
     */
    public void addPerson(String person) {
        people.add(person);
    }

    /**
     * Returns the books referenced.
     *
     * @return the books (never null)
     */
    public List<String> getBooks() {
        return books;
    }

    /**
     * Sets the books list.
     *
     * @param books the books
     */
    public void setBooks(List<String> books) {
        this.books = books;
    }

    /**
     * Adds a book reference.
     *
     * @param book the book title
     */
    public void addBook(String book) {
        books.add(book);
    }

    /**
     * Returns the locations referenced.
     *
     * @return the locations (never null)
     */
    public List<String> getLocations() {
        return locations;
    }

    /**
     * Sets the locations list.
     *
     * @param locations the locations
     */
    public void setLocations(List<String> locations) {
        this.locations = locations;
    }

    /**
     * Adds a location reference.
     *
     * @param loc the location name
     */
    public void addLocation(String loc) {
        locations.add(loc);
    }

    /**
     * Returns the symbols referenced.
     *
     * @return the symbols (never null)
     */
    public List<String> getSymbols() {
        return symbols;
    }

    /**
     * Sets the symbols list.
     *
     * @param symbols the symbols
     */
    public void setSymbols(List<String> symbols) {
        this.symbols = symbols;
    }

    /**
     * Adds a symbol reference.
     *
     * @param symbol the symbol name
     */
    public void addSymbol(String symbol) {
        symbols.add(symbol);
    }

    /**
     * Returns the translations.
     *
     * @return the translations (never null)
     */
    public List<String> getTranslations() {
        return translations;
    }

    /**
     * Sets the translations.
     *
     * @param translations the translations
     */
    public void setTranslations(List<String> translations) {
        this.translations = translations;
    }

    /**
     * Adds a translation.
     *
     * @param translation the translation text
     */
    public void addTranslation(String translation) {
        translations.add(translation);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof GraphText graphText)) return false;
        return Objects.equals(notes, graphText.notes) &&
                Objects.equals(people, graphText.people) &&
                Objects.equals(books, graphText.books) &&
                Objects.equals(locations, graphText.locations) &&
                Objects.equals(symbols, graphText.symbols) &&
                Objects.equals(translations, graphText.translations);
    }

    @Override
    public int hashCode() {
        return Objects.hash(notes, people, books, locations, symbols, translations);
    }

    @Override
    public String toString() {
        return "GraphText{" +
                "notes=" + notes.size() +
                ", people=" + people.size() +
                ", books=" + books.size() +
                ", locations=" + locations.size() +
                ", symbols=" + symbols.size() +
                ", translations=" + translations.size() +
                '}';
    }
}
