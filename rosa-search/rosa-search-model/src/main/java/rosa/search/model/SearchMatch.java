package rosa.search.model;

import java.io.Serializable;
import java.util.List;

/**
 * Represents the match of an object in an index against a query. The optional
 * context of the match is a list of search field name, html pairs. The html
 * contains formatted text that matched the query.
 */
public class SearchMatch implements Serializable {
    private static final long serialVersionUID = 1L;

    private String id;
    private List<String> context;

    public SearchMatch() {
        this(null, null);
    }

    public SearchMatch(String id, List<String> context) {
        this.id = id;
        this.context = context;
    }

    public String getId() {
        return id;
    }

    public List<String> getContext() {
        return context;
    }

    @Override
    public String toString() {
        return "SearchMatch [id=" + id + ", context=" + context + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((context == null) ? 0 : context.hashCode());
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (!(obj instanceof SearchMatch))
            return false;
        SearchMatch other = (SearchMatch) obj;
        if (context == null) {
            if (other.context != null)
                return false;
        } else if (!context.equals(other.context))
            return false;
        if (id == null) {
            if (other.id != null)
                return false;
        } else if (!id.equals(other.id))
            return false;
        return true;
    }
}
