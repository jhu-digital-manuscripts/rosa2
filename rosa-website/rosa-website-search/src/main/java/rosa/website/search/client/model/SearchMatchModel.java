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

    /** No-arg constructor for GWT */
    private SearchMatchModel() {}

    public SearchMatchModel(SearchMatch match, String imageUrl) {
        this.match = match;
        this.imageUrl = imageUrl;
    }

    public String getId() {
        return match.getId();
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
        return !(imageUrl != null ? !imageUrl.equals(that.imageUrl) : that.imageUrl != null);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (match != null ? match.hashCode() : 0);
        result = 31 * result + (imageUrl != null ? imageUrl.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "SearchMatchModel{" + "match=" + match + ", imageUrl='" + imageUrl + '\'' + '}';
    }
}
