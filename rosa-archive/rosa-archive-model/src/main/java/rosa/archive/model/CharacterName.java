package rosa.archive.model;

import com.google.gwt.user.client.rpc.IsSerializable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

/**
 * The name of a character. Supports multiple languages.
 */
public class CharacterName implements IsSerializable {
    // TODO set a default language if non is specified when added? can be made configurable!
    private static final String DEFAULT_LANGUAGE = "EN";

    private String id;
    /**
     * Map of language -> name
     */
    private HashMap<String, String> names;

    public CharacterName() {
        this.names = new HashMap<>();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getNameInLanguage(String language) {
        return names.get(language);
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

    public void addName(String name, String language) {
        names.put(language.trim().toLowerCase(), name);
    }

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