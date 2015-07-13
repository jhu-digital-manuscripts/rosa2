package rosa.website.search.client;

import rosa.search.model.Query;
import rosa.search.model.QueryOperation;
import rosa.search.model.SearchFields;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class RosaQueryAdapter implements QueryAdapter {

    @Override
    public String toToken(Query query) {
        StringBuilder sb = new StringBuilder();

        if (query.isOperation()) {
            for (Query child : query.children()) {
                if (isBookQuery(child)) {
                    sb.append(bookToken(child));
                } else {
                    sb.append(toToken(child));
                }
            }
        } else if (query.isTerm()) {
            sb.append(toCategory(SearchFields.valueOf(query.getTerm().getField())));
            sb.append(";");
            sb.append(query.getTerm().getValue());
            sb.append(";");
        }

        return sb.toString();
    }

    @Override
    public Query toQuery(String token) {
        String[] parts = token.split(";");

        List<Query> top = new ArrayList<>();
        for (int i = 0; i < parts.length; i++) {
            SearchCategory category = SearchCategory.valueOf(parts[i]);

            if (category != null) {
                top.add(adaptToSearchFields(category, parts[++i]));
            } else if (parts[i].equalsIgnoreCase("BOOK")) {
                top.add(restrictByBooks(parts[++i].split(",")));
            }
        }

        return new Query(QueryOperation.AND, top.toArray(new Query[top.size()]));
    }

    /**
     * Adapt UI facing search categories and query term to Lucene facing queries.
     * The UI categories map to one or more Lucene field.
     *
     * @param category the UI facing search category
     * @param queryTerm query term
     * @return the Lucene facing search query
     */
    private Query adaptToSearchFields(SearchCategory category, String queryTerm) {
        List<Query> top = new ArrayList<>();

        for (SearchFields field : category.getFields()) {
            top.add(new Query(field, queryTerm));
        }

        return new Query(QueryOperation.AND, top.toArray(new Query[top.size()]));
    }

    /**
     * Create a new query to restrict results to only those books that appear in
     * a list.
     *
     * @param books list of books
     * @return query
     */
    private Query restrictByBooks(String[] books) {
        List<Query> top = new ArrayList<>();

        for (String book : books) {
            top.add(new Query(SearchFields.BOOK_ID, book));
        }

        return new Query(QueryOperation.OR, top.toArray(new Query[top.size()]));
    }

    /**
     * @param query the query in question
     * @return is the query a book restriction query?
     */
    private boolean isBookQuery(Query query) {
        return query.isOperation()
                && query.children().length > 0
                && query.children()[0].isTerm()
                && SearchFields.valueOf(query.children()[0].getTerm().getField()) == SearchFields.BOOK_ID;
    }

    /**
     * Convert a Lucene facing SearchFields field into a String version of a
     * UI facing SearchCategory.
     *
     * @param field Lucene search field
     * @return UI search category as String
     */
    private String toCategory(SearchFields field) {
        for (SearchCategory category : SearchCategory.values()) {
            if (arrayContains(category.getFields(), field)) {
                return category.toString();
            }
        }

        return null;
    }

    /**
     * @param arr array of values
     * @param obj object to look for
     * @return does the array contain the object?
     */
    private boolean arrayContains(Object[] arr, Object obj) {
        return Arrays.asList(arr).contains(obj);
    }

    /**
     * A Query representing a restriction according to a list of book titles must be
     * treated different from other queries. Such a query will contain several queries
     * with the same SearchFields field ORed together. All query terms should be
     * combined into a comma separated list.
     *
     * Example result: BOOK;BOOK1,BOOK2,Book3,Book4;
     *
     * @param query book ID restriction query
     * @return a String version of the query
     */
    private String bookToken(Query query) {
        if (!isBookQuery(query)) {
            return "";
        }

        StringBuilder sb = new StringBuilder("BOOK;");
        boolean first = true;

        for (Query child : query.children()) {
            if (!child.isTerm()) {
                continue;
            }

            if (!first) {
                sb.append(',');
            }

            sb.append(child.getTerm().getValue());
            first = false;
        }
        sb.append(';');

        return sb.toString();
    }
}
