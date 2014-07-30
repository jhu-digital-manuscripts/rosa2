package rosa.archive.model;

import com.google.gwt.user.client.rpc.IsSerializable;

import java.util.HashMap;

/**
 *
 */
public class CharacterName implements IsSerializable {
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

    public void setNames(HashMap<String, String> names) {
        this.names = names;
    }

    // TODO equals/hashCode
}