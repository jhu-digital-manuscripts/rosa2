package rosa.search.model;

import java.io.Serializable;

/**
 * Count of occurrences of a value in a category.
 */
public class CategoryValueCount implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private String value;
    private int count;
    
    public CategoryValueCount() {}
    
    public CategoryValueCount(String value, int count) {
        this.value = value;
        this.count = count;
    }   
    
    public String getValue() {
        return value;
    }
    
    public int getCount() {
        return count;
    }
    
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + count;
        result = prime * result + ((value == null) ? 0 : value.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (!(obj instanceof CategoryValueCount))
            return false;
        CategoryValueCount other = (CategoryValueCount) obj;
        if (count != other.count)
            return false;
        if (value == null) {
            if (other.value != null)
                return false;
        } else if (!value.equals(other.value))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "CategoryValueCount [value=" + value + ", count=" + count + "]";
    }
}
