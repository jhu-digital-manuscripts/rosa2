package rosa.archive.model;

import com.google.gwt.user.client.rpc.IsSerializable;

import java.util.List;

/**
 *
 */
public class ImageTagging implements IsSerializable {

    private int numberOfImages;
    private List<Page> pages;

    public ImageTagging() {  }

    public int getNumberOfImages() {
        return numberOfImages;
    }

    public void setNumberOfImages(int numberOfImages) {
        this.numberOfImages = numberOfImages;
    }

    public List<Page> getPages() {
        return pages;
    }

    public void setPages(List<Page> pages) {
        this.pages = pages;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ImageTagging)) return false;

        ImageTagging that = (ImageTagging) o;

        if (numberOfImages != that.numberOfImages) return false;
        if (!pages.equals(that.pages)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = numberOfImages;
        result = 31 * result + pages.hashCode();
        return result;
    }

    public class Page implements IsSerializable {
        private String textualElement;
        private String costume;
        private String object;
        private String landscape;
        private String architecture;
        private String other;
        private String[] characters;
        private String[] titles;

        public Page() {  }

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

        // TODO equals/hashCode
    }

}
