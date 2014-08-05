package rosa.archive.model;

import com.google.gwt.user.client.rpc.IsSerializable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

/**
 * A collection of books stored in the archive.
 */
public class BookCollection implements IsSerializable {

    private HashMap<String, Book> books;
    private ArrayList<String> languages;
    private CharacterNames characterNames;
    private IllustrationTitles illustrationTitles;
    private NarrativeSections narrativeSections;

    public BookCollection() {
        this.books = new HashMap<>();
        this.languages = new ArrayList<>();
    }

    /**
     * Retrieve a book in the archive with its ID. If the book is not present in this collection,
     * NULL is returned.
     *
     * @param id
     *          ID of the book you want
     * @return
     *          a Book. NULL if Book is not present in the collection.
     */
    public Book getBook(String id) {
        return books.get(id);
    }

    public Set<String> getAllBookIds() {
        return books.keySet();
    }

    /**
     * Get a set containing all of the books in this collection.
     *
     * @return
     *          an empty set will be returned if no books exist in this collection.
     */
    // Is this necessary?
    public Set<Book> getAllBooks() {
        Set<Book> allBooks = new HashSet<>();

        for (Entry<String, Book> entry : books.entrySet()) {
            allBooks.add( entry.getValue() );
        }
        return allBooks;
    }

    public void setBooks(HashMap<String, Book> books) {
        this.books = books;
    }

    public CharacterNames getCharacterNames() {
        return characterNames;
    }

    public void setCharacterNames(CharacterNames characterNames) {
        this.characterNames = characterNames;
    }

    public IllustrationTitles getIllustrationTitles() {
        return illustrationTitles;
    }

    public void setIllustrationTitles(IllustrationTitles illustrationTitles) {
        this.illustrationTitles = illustrationTitles;
    }

    public NarrativeSections getNarrativeSections() {
        return narrativeSections;
    }

    public void setNarrativeSections(NarrativeSections narrativeSections) {
        this.narrativeSections = narrativeSections;
    }

    /**
     * Get a list of all languages supported by this collection.
     *
     * @return
     *          List of languages
     */
    public List<String> getAllSupportedLanguages() {
        return languages;
    }

    public void addSupportedLanguage(String language) {
        languages.add(language);
    }

    public void setLanguages(ArrayList<String> languages) {
        this.languages = languages;
    }

    /**
     * Checks whether or not a language is supported by this collection.
     *
     * @param language
     * @return
     */
    public boolean isLanguageSupported(String language) {
        return languages.contains(language);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BookCollection)) return false;

        BookCollection that = (BookCollection) o;

        if (books != null ? !books.equals(that.books) : that.books != null) return false;
        if (characterNames != null ? !characterNames.equals(that.characterNames) : that.characterNames != null)
            return false;
        if (illustrationTitles != null ? !illustrationTitles.equals(that.illustrationTitles) : that.illustrationTitles != null)
            return false;
        if (languages != null ? !languages.equals(that.languages) : that.languages != null) return false;
        if (narrativeSections != null ? !narrativeSections.equals(that.narrativeSections) : that.narrativeSections != null)
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = books != null ? books.hashCode() : 0;
        result = 31 * result + (languages != null ? languages.hashCode() : 0);
        result = 31 * result + (characterNames != null ? characterNames.hashCode() : 0);
        result = 31 * result + (illustrationTitles != null ? illustrationTitles.hashCode() : 0);
        result = 31 * result + (narrativeSections != null ? narrativeSections.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "BookCollection{" +
                "books=" + books +
                ", languages=" + languages +
                ", characterNames=" + characterNames +
                ", illustrationTitles=" + illustrationTitles +
                ", narrativeSections=" + narrativeSections +
                '}';
    }
}
