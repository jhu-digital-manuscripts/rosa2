package rosa.archive.model;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * The name of a character. Supports multiple languages.
 */
public class CharacterName implements Serializable {
    private static final long serialVersionUID = 1L;
    private static final String DEFAULT_LANGUAGE = "en";

    private String id;
    /**
     * Map of language to name
     */
    private Map<String, String> names;

    /**
     *
     */
    public CharacterName() {
        this.names = new HashMap<>();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    /**
     * @param language language code
     * @return the name in the specified language
     */
    public String getNameInLanguage(String language) {
        return names.get(language.toLowerCase());
    }

    /**
     * @return
     *          All names for this character in no particular order
     */
    public Set<String> getAllNames() {
        Set<String> nameSet = new HashSet<>();

        for (String lang : names.keySet()) {
            nameSet.add(names.get(lang));
        }

        return nameSet;
    }

    /**
     * Add a name for a language. If a name already exists for the specified language, the
     * new name will overwrite the old name.
     *
     * @param name
     *          the character's name
     * @param language
     *          the language that the name is in
     * @throws java.lang.NullPointerException thrown if language is NULL
     */
    public void addName(String name, String language) {
        // hack to change weird column headers into 2 char language codes
        language = language.toLowerCase().trim();
        if (language.contains("english")) {
            language = "en";
        } else if (language.contains("french")) {
            language = "fr";
        }
        names.put(language, name);
    }

    /**
     * Add a name in the default language.
     * @param name the name
     */
    public void addName(String name) {
        addName(name, DEFAULT_LANGUAGE);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CharacterName)) return false;

        CharacterName that = (CharacterName) o;

        if (id != null ? !id.equals(that.id) : that.id != null) return false;
        if (names != null ? !names.equals(that.names) : that.names != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (names != null ? names.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "CharacterName{" +
                "id='" + id + '\'' +
                ", names=" + names +
                '}';
    }
}