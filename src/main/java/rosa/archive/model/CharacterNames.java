package rosa.archive.model;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * A collection of character names associated with a book collection.
 * Characters may have names in multiple languages.
 */
public class CharacterNames implements HasId {

    private String id;
    private Map<String, CharacterName> names;

    /**
     * Creates an empty CharacterNames collection.
     */
    public CharacterNames() {
        this.names = new HashMap<>();
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
     * Checks whether a character with the given id exists.
     *
     * @param id the character id
     * @return {@code true} if the character exists
     */
    public boolean hasCharacter(String id) {
        return names.containsKey(id);
    }

    /**
     * Returns all character names available in the specified language.
     *
     * @param language the language code
     * @return a set of character names in the specified language
     */
    public Set<String> getAllNamesInLanguage(String language) {
        var namesInLanguage = new HashSet<String>();
        for (CharacterName characterName : names.values()) {
            String name = characterName.getNameInLanguage(language);
            if (name != null && !name.isEmpty()) {
                namesInLanguage.add(name);
            }
        }
        return namesInLanguage;
    }

    /**
     * Returns the name of a character in a specific language.
     *
     * @param id       the character id
     * @param language the language code
     * @return the name in the specified language, or {@code null} if not available
     */
    public String getNameInLanguage(String id, String language) {
        CharacterName characterName = names.get(id);
        return characterName == null ? null : characterName.getNameInLanguage(language);
    }

    /**
     * Returns the set of all character identifiers.
     *
     * @return all character ids
     */
    public Set<String> getAllCharacterIds() {
        return names.keySet();
    }

    /**
     * Returns the CharacterName object for the given id.
     *
     * @param id the character id
     * @return the CharacterName, or {@code null} if not found
     */
    public CharacterName getCharacterName(String id) {
        return names.get(id);
    }

    /**
     * Adds a character name to this collection.
     *
     * @param name the character name to add
     */
    public void addCharacterName(CharacterName name) {
        names.put(name.getId(), name);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CharacterNames that)) return false;
        return Objects.equals(id, that.id) &&
                Objects.equals(names, that.names);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, names);
    }

    @Override
    public String toString() {
        return "CharacterNames{" +
                "id='" + id + '\'' +
                ", names=" + names +
                '}';
    }
}
