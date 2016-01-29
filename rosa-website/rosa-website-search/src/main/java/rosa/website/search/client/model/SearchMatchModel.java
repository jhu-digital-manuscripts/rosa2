package rosa.website.search.client.model;

import rosa.search.model.SearchMatch;

import java.io.Serializable;
import java.util.List;

/**
 * Model object for a single search match UI object. Wraps {@link SearchMatch} and
 * provides access to a single image URL, which can be used to hold a URL to display
 * a thumbnail, for example.
 */
public class SearchMatchModel extends SearchMatch implements Serializable {
    private static final long serialVersionUID = 1l;

    private SearchMatch match;
    private String imageUrl;
    private String targetUrl;
    private String display;

    /** No-arg constructor for GWT */
    @SuppressWarnings("unused")
    private SearchMatchModel() {}

    public SearchMatchModel(SearchMatch match, String imageUrl, String targetUrl, String display) {
        this.match = match;
        this.imageUrl = imageUrl;
        this.targetUrl = targetUrl;
        this.display = display;
    }

    public String getId() {
        return match.getId();
    }

    public String getTargetUrl() {
        return targetUrl;
    }

    public String getDisplay() {
        return display;
    }

    public List<String> getContext() {
        return match.getContext();
    }

    public String getImageUrl() {
        return imageUrl;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        SearchMatchModel that = (SearchMatchModel) o;

        if (match != null ? !match.equals(that.match) : that.match != null) return false;
        if (imageUrl != null ? !imageUrl.equals(that.imageUrl) : that.imageUrl != null) return false;
        if (targetUrl != null ? !targetUrl.equals(that.targetUrl) : that.targetUrl != null) return false;
        return !(display != null ? !display.equals(that.display) : that.display != null);

    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (match != null ? match.hashCode() : 0);
        result = 31 * result + (imageUrl != null ? imageUrl.hashCode() : 0);
        result = 31 * result + (targetUrl != null ? targetUrl.hashCode() : 0);
        result = 31 * result + (display != null ? display.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "SearchMatchModel{" + "match=" + match + ", imageUrl='" + imageUrl + '\'' +
                ", targetUrl='" + targetUrl + '\'' + ", display='" + display + '\'' + '}';
    }
}
