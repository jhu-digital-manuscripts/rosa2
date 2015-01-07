package rosa.archive.model;

import java.io.Serializable;
import java.util.Arrays;

/**
 * A collection of books stored in the archive.
 */
public class BookCollection implements HasId, Serializable {
    private static final long serialVersionUID = 1L;

    private String id;
    /**
     * Array of book IDs for the books in this collection.
     */
    private String[] books;
    /**
     * Array of languages supported by this collection.
     */
    private String[] languages;
    private CharacterNames characterNames;
    private IllustrationTitles illustrationTitles;
    private NarrativeSections narrativeSections;
    private SHA1Checksum checksums;

    public BookCollection() {
        books = new String[0];
        languages = new String[0];
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    /**
     * @return
     *          array containing the IDs of all books in this collection.
     */
    public String[] books() {
        return books;
    }

    public void setBooks(String[] books) {
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
    public String[] getAllSupportedLanguages() {
        return languages;
    }

    public void setLanguages(String[] languages) {
        this.languages = languages;
    }

    /**
     * Checks whether or not a language is supported by this collection.
     *
     * @param language
     *          language code
     * @return
     *          TRUE if language is supported by collection. FALSE otherwise.
     */
    public boolean isLanguageSupported(String language) {
        for (String lang : languages) {
            if (lang.equals(language)) {
                return true;
            }
        }
        return false;
    }

    public SHA1Checksum getChecksum() {
        return checksums;
    }

    public void setChecksum(SHA1Checksum checksums) {
        this.checksums = checksums;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        BookCollection that = (BookCollection) o;

        if (!Arrays.equals(books, that.books)) return false;
        if (characterNames != null ? !characterNames.equals(that.characterNames) : that.characterNames != null)
            return false;
        if (checksums != null ? !checksums.equals(that.checksums) : that.checksums != null) return false;
        if (id != null ? !id.equals(that.id) : that.id != null) return false;
        if (illustrationTitles != null ? !illustrationTitles.equals(that.illustrationTitles) : that.illustrationTitles != null)
            return false;
        if (!Arrays.equals(languages, that.languages)) return false;
        if (narrativeSections != null ? !narrativeSections.equals(that.narrativeSections) : that.narrativeSections != null)
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (books != null ? Arrays.hashCode(books) : 0);
        result = 31 * result + (languages != null ? Arrays.hashCode(languages) : 0);
        result = 31 * result + (characterNames != null ? characterNames.hashCode() : 0);
        result = 31 * result + (illustrationTitles != null ? illustrationTitles.hashCode() : 0);
        result = 31 * result + (narrativeSections != null ? narrativeSections.hashCode() : 0);
        result = 31 * result + (checksums != null ? checksums.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "BookCollection{" +
                "id='" + id + '\'' +
                ", books=" + Arrays.toString(books) +
                ", languages=" + Arrays.toString(languages) +
                ", characterNames=" + characterNames +
                ", illustrationTitles=" + illustrationTitles +
                ", narrativeSections=" + narrativeSections +
                ", checksums=" + checksums +
                '}';
    }
}
