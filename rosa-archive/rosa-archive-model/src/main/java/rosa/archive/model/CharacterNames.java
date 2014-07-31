package rosa.archive.model;

import com.google.gwt.user.client.rpc.IsSerializable;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

/**
 *
 */
public class CharacterNames implements IsSerializable {

    private HashMap<String, CharacterName> names;

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

    public String getNameInLanguage(String id, String language) {
        return names.get(id).getNameInLanguage(language);
    }

    public Set<String> getAllCharacterIds() {
        return names.keySet();
    }

    public void addCharacterName(CharacterName name) {
        names.put(name.getId(), name);
    }

//    TODO expose underlying map in this way?
//    public void setNames(HashMap<String, CharacterName> names) {
//        this.names = names;
//    }

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
}
