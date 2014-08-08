package rosa.archive.model;

import com.google.gwt.user.client.rpc.IsSerializable;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 *
 */
public class CharacterNames implements IsSerializable {

    private Map<String, CharacterName> names;

    public CharacterNames() {
        this.names = new HashMap<>();
    }

    /**
     * Get a set of all character names in a particular language.
     *
     * @param language
     *          desired language
     */
    public Set<String> getAllNamesInLanguage(String language) {
        Set<String> namesInLanguage = new HashSet<>();

        for (String id : names.keySet()) {
            CharacterName characterName = names.get(id);
            String name = characterName.getNameInLanguage(language);

            if (StringUtils.isNotBlank(name)) {
                namesInLanguage.add(name);
            }
        }

        return namesInLanguage;
    }

    /**
     * Get the name of a character in a specific language. If the character name's ID
     * does not exist, or the character name does not exist in the specified language,
     * NULL will be returned.
     *
     * @param id
     *          the CharacterName id
     * @param language
     *          language code
     * @return
     *          the name of a character in the desired language or NULL if not available.
     */
    public String getNameInLanguage(String id, String language) {
        return names.get(id) == null ? null : names.get(id).getNameInLanguage(language);
    }

    public Set<String> getAllCharacterIds() {
        return names.keySet();
    }

    public void addCharacterName(CharacterName name) {
        names.put(name.getId(), name);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CharacterNames)) return false;

        CharacterNames that = (CharacterNames) o;

        if (names != null ? !names.equals(that.names) : that.names != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return names != null ? names.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "CharacterNames{" +
                "names=" + names +
                '}';
    }
}
