package rosa.archive.model;

import java.util.Arrays;
import java.util.Objects;

/**
 * Image tagging information for a single illustration on a page,
 * including characters, titles, and descriptive categories.
 */
public class Illustration {

    private String id;
    private String page;
    private String textualElement;
    private String initials;
    private String costume;
    private String object;
    private String landscape;
    private String architecture;
    private String other;
    private String[] characters;
    private String[] titles;

    /**
     * Creates an empty Illustration.
     */
    public Illustration() {}

    /**
     * Returns the illustration identifier.
     *
     * @return the illustration id
     */
    public String getId() {
        return id;
    }

    /**
     * Sets the illustration identifier.
     *
     * @param id the illustration id
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Returns the page on which this illustration appears.
     *
     * @return the page identifier
     */
    public String getPage() {
        return page;
    }

    /**
     * Sets the page on which this illustration appears.
     *
     * @param page the page identifier
     */
    public void setPage(String page) {
        this.page = page;
    }

    /**
     * Returns the textual elements associated with this illustration.
     *
     * @return the textual element description
     */
    public String getTextualElement() {
        return textualElement;
    }

    /**
     * Sets the textual elements.
     *
     * @param textualElement the textual element description
     */
    public void setTextualElement(String textualElement) {
        this.textualElement = textualElement;
    }

    /**
     * Returns the initials associated with this illustration.
     *
     * @return the initials
     */
    public String getInitials() {
        return initials;
    }

    /**
     * Sets the initials.
     *
     * @param initials the initials
     */
    public void setInitials(String initials) {
        this.initials = initials;
    }

    /**
     * Returns the costume description.
     *
     * @return the costume description
     */
    public String getCostume() {
        return costume;
    }

    /**
     * Sets the costume description.
     *
     * @param costume the costume description
     */
    public void setCostume(String costume) {
        this.costume = costume;
    }

    /**
     * Returns the objects depicted.
     *
     * @return the object description
     */
    public String getObject() {
        return object;
    }

    /**
     * Sets the objects depicted.
     *
     * @param object the object description
     */
    public void setObject(String object) {
        this.object = object;
    }

    /**
     * Returns the landscape elements.
     *
     * @return the landscape description
     */
    public String getLandscape() {
        return landscape;
    }

    /**
     * Sets the landscape elements.
     *
     * @param landscape the landscape description
     */
    public void setLandscape(String landscape) {
        this.landscape = landscape;
    }

    /**
     * Returns the architectural elements.
     *
     * @return the architecture description
     */
    public String getArchitecture() {
        return architecture;
    }

    /**
     * Sets the architectural elements.
     *
     * @param architecture the architecture description
     */
    public void setArchitecture(String architecture) {
        this.architecture = architecture;
    }

    /**
     * Returns other notable elements.
     *
     * @return the other description
     */
    public String getOther() {
        return other;
    }

    /**
     * Sets other notable elements.
     *
     * @param other the other description
     */
    public void setOther(String other) {
        this.other = other;
    }

    /**
     * Returns the characters depicted in this illustration.
     *
     * @return the character names array
     */
    public String[] getCharacters() {
        return characters;
    }

    /**
     * Sets the characters depicted.
     *
     * @param characters the character names array
     */
    public void setCharacters(String[] characters) {
        this.characters = characters;
    }

    /**
     * Returns the titles associated with this illustration.
     *
     * @return the titles array
     */
    public String[] getTitles() {
        return titles;
    }

    /**
     * Sets the titles.
     *
     * @param titles the titles array
     */
    public void setTitles(String[] titles) {
        this.titles = titles;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Illustration that)) return false;
        return Objects.equals(id, that.id) &&
                Objects.equals(page, that.page) &&
                Objects.equals(textualElement, that.textualElement) &&
                Objects.equals(initials, that.initials) &&
                Objects.equals(costume, that.costume) &&
                Objects.equals(object, that.object) &&
                Objects.equals(landscape, that.landscape) &&
                Objects.equals(architecture, that.architecture) &&
                Objects.equals(other, that.other) &&
                Arrays.equals(characters, that.characters) &&
                Arrays.equals(titles, that.titles);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(id, page, textualElement, initials, costume,
                object, landscape, architecture, other);
        result = 31 * result + Arrays.hashCode(characters);
        result = 31 * result + Arrays.hashCode(titles);
        return result;
    }

    @Override
    public String toString() {
        return "Illustration{" +
                "id='" + id + '\'' +
                ", page='" + page + '\'' +
                ", textualElement='" + textualElement + '\'' +
                ", initials='" + initials + '\'' +
                ", costume='" + costume + '\'' +
                ", object='" + object + '\'' +
                ", landscape='" + landscape + '\'' +
                ", architecture='" + architecture + '\'' +
                ", other='" + other + '\'' +
                ", characters=" + Arrays.toString(characters) +
                ", titles=" + Arrays.toString(titles) +
                '}';
    }
}
