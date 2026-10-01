package rosa.archive.model;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * The name of a character, supporting multiple languages.
 * Names are stored keyed by language code.
 */
public class CharacterName {

    private static final String DEFAULT_LANGUAGE = "en";

    /**
     * Key used for the canonical name of the character as it appears in the
     * "Site name" column of {@code character_names.csv}.
     */
    private static final String SITE_NAME_LANGUAGE = "site name";

    private String id;
    private Map<String, String> names;

    /**
     * Creates an empty CharacterName.
     */
    public CharacterName() {
        this.names = new HashMap<>();
    }

    /**
     * Returns the identifier for this character name.
     *
     * @return the character name id
     */
    public String getId() {
        return id;
    }

    /**
     * Sets the identifier for this character name.
     *
     * @param id the character name id
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Returns the name for this character in the specified language.
     *
     * @param language the language code
     * @return the name in the specified language, or {@code null} if not available
     */
    public String getNameInLanguage(String language) {
        return names.get(language.toLowerCase());
    }

    /**
     * Returns all names for this character across all languages.
     *
     * @return a set of all names
     */
    public Set<String> getAllNames() {
        return new HashSet<>(names.values());
    }

    /**
     * Returns the canonical site name of this character, if known.
     *
     * @return the site name, or {@code null} if not available
     */
    public String getSiteName() {
        return trimToNull(names.get(SITE_NAME_LANGUAGE));
    }

    /**
     * Returns a human readable name for this character, suitable for display.
     * The canonical site name is preferred, with the English name appended in
     * parentheses when it adds information. Falls back to the English name, then
     * to any other available name.
     *
     * @return a display name, or {@code null} if this character has no names
     */
    public String getDisplayName() {
        String site = getSiteName();
        String english = trimToNull(names.get(DEFAULT_LANGUAGE));

        if (site != null && english != null && !site.equalsIgnoreCase(english)) {
            return site + " (" + english + ")";
        }
        if (site != null) {
            return site;
        }
        if (english != null) {
            return english;
        }

        for (String name : names.values()) {
            String trimmed = trimToNull(name);
            if (trimmed != null) {
                return trimmed;
            }
        }

        return null;
    }

    private static String trimToNull(String s) {
        if (s == null) {
            return null;
        }
        String trimmed = s.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    /**
     * Adds a name for a language. If a name already exists for the specified language,
     * the new name overwrites the old one. Normalizes language headers like "English"
     * to standard two-character codes.
     *
     * @param name     the character's name
     * @param language the language code
     */
    public void addName(String name, String language) {
        language = language.toLowerCase().trim();
        if (language.contains("english")) {
            language = "en";
        } else if (language.contains("french")) {
            language = "fr";
        }
        names.put(language, name);
    }

    /**
     * Adds a name in the default language (English).
     *
     * @param name the name
     */
    public void addName(String name) {
        addName(name, DEFAULT_LANGUAGE);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CharacterName that)) return false;
        return Objects.equals(id, that.id) &&
                Objects.equals(names, that.names);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, names);
    }

    @Override
    public String toString() {
        return "CharacterName{" +
                "id='" + id + '\'' +
                ", names=" + names +
                '}';
    }
}
