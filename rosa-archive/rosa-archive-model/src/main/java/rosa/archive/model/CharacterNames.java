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

    // TODO equals/hashCode
}
