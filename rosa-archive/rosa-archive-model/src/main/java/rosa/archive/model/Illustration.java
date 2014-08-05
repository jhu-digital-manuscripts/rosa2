package rosa.archive.model;

import com.google.gwt.user.client.rpc.IsSerializable;

import java.util.Arrays;

/**
 * Image tagging information for a single image on a page.
 */
public class Illustration implements IsSerializable {
    private String id;
    private String page;
    private String textualElement;
    private String costume;
    private String object;
    private String landscape;
    private String architecture;
    private String other;
    private String[] characters;
    private String[] titles;

    public Illustration() {  }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getPage() {
        return page;
    }

    public void setPage(String page) {
        this.page = page;
    }

    public String getTextualElement() {
        return textualElement;
    }

    public void setTextualElement(String textualElement) {
        this.textualElement = textualElement;
    }

    public String getCostume() {
        return costume;
    }

    public void setCostume(String costume) {
        this.costume = costume;
    }

    public String getObject() {
        return object;
    }

    public void setObject(String object) {
        this.object = object;
    }

    public String getLandscape() {
        return landscape;
    }

    public void setLandscape(String landscape) {
        this.landscape = landscape;
    }

    public String getArchitecture() {
        return architecture;
    }

    public void setArchitecture(String architecture) {
        this.architecture = architecture;
    }

    public String getOther() {
        return other;
    }

    public void setOther(String other) {
        this.other = other;
    }

    public String[] getCharacters() {
        return characters;
    }

    public void setCharacters(String[] characters) {
        this.characters = characters;
    }

    public String[] getTitles() {
        return titles;
    }

    public void setTitles(String[] titles) {
        this.titles = titles;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Illustration)) return false;

        Illustration that = (Illustration) o;

        if (architecture != null ? !architecture.equals(that.architecture) : that.architecture != null) return false;
        if (!Arrays.equals(characters, that.characters)) return false;
        if (costume != null ? !costume.equals(that.costume) : that.costume != null) return false;
        if (id != null ? !id.equals(that.id) : that.id != null) return false;
        if (landscape != null ? !landscape.equals(that.landscape) : that.landscape != null) return false;
        if (object != null ? !object.equals(that.object) : that.object != null) return false;
        if (other != null ? !other.equals(that.other) : that.other != null) return false;
        if (textualElement != null ? !textualElement.equals(that.textualElement) : that.textualElement != null)
            return false;
        if (!Arrays.equals(titles, that.titles)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (textualElement != null ? textualElement.hashCode() : 0);
        result = 31 * result + (costume != null ? costume.hashCode() : 0);
        result = 31 * result + (object != null ? object.hashCode() : 0);
        result = 31 * result + (landscape != null ? landscape.hashCode() : 0);
        result = 31 * result + (architecture != null ? architecture.hashCode() : 0);
        result = 31 * result + (other != null ? other.hashCode() : 0);
        result = 31 * result + (characters != null ? Arrays.hashCode(characters) : 0);
        result = 31 * result + (titles != null ? Arrays.hashCode(titles) : 0);
        return result;
    }

    @Override
    public String toString() {
        return "Illustration{" +
                "id='" + id + '\'' +
                ", textualElement='" + textualElement + '\'' +
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