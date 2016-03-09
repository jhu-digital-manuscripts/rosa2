package rosa.search.model;

/**
 * An AND operation returns the intersection of all the results from its child queries.
 * An OR operation returns the union of all the results from its child queries. 
 */
public enum QueryOperation {
    AND, OR
}
