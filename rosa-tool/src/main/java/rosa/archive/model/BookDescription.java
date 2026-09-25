package rosa.archive.model;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * A book description parsed from a TEI P5 XML file.
 * 
 * <p>The description is organized as a series of notes with different categories
 * (e.g., IDENTIFICATION, BASIC INFORMATION, MATERIAL, QUIRES, LAYOUT, SCRIPT,
 * DECORATION, BINDING, HISTORY, TEXT). Each note may contain structured content
 * represented here as plain text.
 */
public class BookDescription implements HasId {

    private String id;
    private String language;
    private Map<String, String> notes;

    /**
     * Creates an empty BookDescription.
     */
    public BookDescription() {
        this.notes = new LinkedHashMap<>();
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
     * Returns the language code for this description (e.g., "en", "fr").
     *
     * @return the language code, or {@code null} if not set
     */
    public String getLanguage() {
        return language;
    }

    /**
     * Sets the language code for this description.
     *
     * @param language the language code
     */
    public void setLanguage(String language) {
        this.language = language;
    }

    /**
     * Returns all notes as a map from category to text content.
     * Categories include IDENTIFICATION, BASIC INFORMATION, MATERIAL, etc.
     *
     * @return the notes map (never null)
     */
    public Map<String, String> getNotes() {
        return notes;
    }

    /**
     * Adds a note with the given category.
     *
     * @param category the note category (e.g., "IDENTIFICATION")
     * @param text     the note text content
     */
    public void addNote(String category, String text) {
        notes.put(category, text);
    }

    /**
     * Returns the note text for a specific category.
     *
     * @param category the note category
     * @return the note text, or {@code null} if not present
     */
    public String getNote(String category) {
        return notes.get(category);
    }

    /**
     * Returns a combined text representation of all notes,
     * suitable for full-text indexing.
     *
     * @return all notes concatenated with spaces
     */
    public String getFullText() {
        if (notes.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (String text : notes.values()) {
            if (text != null && !text.isBlank()) {
                if (!sb.isEmpty()) {
                    sb.append(" ");
                }
                sb.append(text.trim());
            }
        }
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BookDescription that)) return false;
        return Objects.equals(id, that.id) &&
                Objects.equals(language, that.language) &&
                Objects.equals(notes, that.notes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, language, notes);
    }

    @Override
    public String toString() {
        return "BookDescription{" +
                "id='" + id + '\'' +
                ", language='" + language + '\'' +
                ", notes=" + notes +
                '}';
    }
}
