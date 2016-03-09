package rosa.search.model;

import java.io.Serializable;
import java.util.Arrays;

/**
 * A query is a tree whose leaves are terms and inner nodes are logical
 * operations that combine children together.
 */
public class Query implements Serializable {
    private static final long serialVersionUID = 1L;

    private Query[] children;
    private QueryOperation op;
    private QueryTerm term;

    public Query() {
        this(null);
    }

    public Query(QueryTerm term) {
        this.children = null;
        this.term = term;
        this.op = null;
    }

    public Query(SearchField term_field, String term_value) {
        this(new QueryTerm(term_field.getFieldName(), term_value));
    }

    public Query(String term_field, String term_value) {
        this(new QueryTerm(term_field, term_value));
    }

    public Query(QueryOperation op, Query... children) {
        this.op = op;
        this.children = children;
        this.term = null;
    }

    public Query[] children() {
        return children;
    }

    public boolean isTerm() {
        return term != null;
    }

    public boolean isOperation() {
        return op != null && children != null;
    }

    public QueryOperation getOperation() {
        return op;
    }

    public QueryTerm getTerm() {
        return term;
    }

    public String toString() {
        return term != null ? term.toString() : "{" + op.name()
                + Arrays.toString(children) + "}";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + Arrays.hashCode(children);
        result = prime * result + ((op == null) ? 0 : op.hashCode());
        result = prime * result + ((term == null) ? 0 : term.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (!(obj instanceof Query))
            return false;
        Query other = (Query) obj;
        if (!Arrays.equals(children, other.children))
            return false;
        if (op != other.op)
            return false;
        if (term == null) {
            if (other.term != null)
                return false;
        } else if (!term.equals(other.term))
            return false;
        return true;
    }
}
