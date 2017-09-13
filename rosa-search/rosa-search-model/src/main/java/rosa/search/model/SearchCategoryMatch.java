package rosa.search.model;

import java.io.Serializable;
import java.util.Arrays;

/**
 * Represents a search category which includes the results of a search. Each
 * value matched in the category together with a count of those matches is
 * available.
 */
public class SearchCategoryMatch implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private String name;
    private CategoryValueCount[] values;

    public SearchCategoryMatch() {}

    public SearchCategoryMatch(String name, CategoryValueCount[] values) {
        this.name = name;
        this.values = values;
    }

    public String getFieldName() {
        return name;
    }

    public CategoryValueCount[] getValues() {
        return values;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + ((values == null) ? 0 : Arrays.hashCode(values));
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (!(obj instanceof SearchCategoryMatch))
            return false;
        SearchCategoryMatch other = (SearchCategoryMatch) obj;
        if (name == null) {
            if (other.name != null)
                return false;
        } else if (!name.equals(other.name))
            return false;
        if (values == null) {
            if (other.values != null)
                return false;
        } else if (!Arrays.equals(values, other.values))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "SearchCategoryMatch [name=" + name + ", values=" + Arrays.toString(values) + "]";
    }
}
