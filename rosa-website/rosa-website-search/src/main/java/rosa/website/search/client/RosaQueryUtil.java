package rosa.website.search.client;

import rosa.search.model.Query;
import rosa.search.model.QueryOperation;
import rosa.search.model.QueryTerm;
import rosa.search.model.SearchFields;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class RosaQueryUtil implements QueryUtil {
    private static final Logger LOG = Logger.getLogger(RosaQueryUtil.class.toString());
    private static final String DELIMITER = ";";

    @Override
    public Query toQuery(String token) {
        List<QueryTerm> terms = queryParts(token);

        List<Query> top = new ArrayList<>();
        for (QueryTerm term : terms) {
            top.add(adaptToSearchFields(SearchCategory.category(term.getField()), term.getValue()));
        }
        top.add(restrictByBooks(bookRestrictionList(token)));

        return new Query(QueryOperation.AND, top.toArray(new Query[top.size()]));
    }

    // #search;ALL;1234;POETRY;qwer-;rewq;RUBRIC;asdf;ALL;lkhj;BOOK;Marne3,AssembleeNationale1230,CodGall80;0
    @Override
    public List<QueryTerm> queryParts(String token) {
        if (token == null || token.isEmpty()) {
            return null;
        }
        token = token.replaceAll("(-;)", ";").replaceAll("(--)", "-");

        List<QueryTerm> queries = new ArrayList<>();
        String[] parts = token.split(DELIMITER);

        SearchCategory current_category = null;

        for (int i = 0; i < parts.length; i+=2) {
            SearchCategory category = SearchCategory.category(parts[i]);
            if (category != null && (i+1 < parts.length)) {
                current_category = category;
                queries.add(new QueryTerm(category.toString(), parts[i + 1]));
            } else if (parts[i].equals("BOOK")) {
                // Ignore Books
                break;
            } else if (category == null && current_category != null) {
                // If first position is ever not a SearchCategory, then the previous term most
                // likely ended with a semi-colon. Append this current term to the last one.
                QueryTerm lastQuery = queries.remove(queries.size() - 1);
                queries.add(
                        new QueryTerm(current_category.toString(), lastQuery.getValue() + ";" + parts[i--])
                );
            }
        }

        return queries;
    }

    @Override
    public String[] bookRestrictionList(String token) {
        String[] parts = token.split(DELIMITER);
        for (int i = 0; i < parts.length; i++) {
            if (parts[i].equals("BOOK")) {
                return parts[++i].split(",");
            }
        }

        return new String[0];
    }

    @Override
    public int offset(String token) {
        int index = token.lastIndexOf(';');

        if (index > -1) {
            String offset = token.substring(index + 1);
            try {
                return Integer.parseInt(offset);
            } catch (NumberFormatException e) {
                LOG.warning("Search token does not contain an offset. [" + token + "]");
            }
        }

        return 0;
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
        if (category.getFields().length == 1) {
            return new Query(category.getFields()[0], queryTerm);
        }

        List<Query> top = new ArrayList<>();
        for (SearchFields field : category.getFields()) {
            top.add(new Query(field, queryTerm));
        }

        return new Query(QueryOperation.OR, top.toArray(new Query[top.size()]));
    }

    /**
     * Create a new query to restrict results to only those books that appear in
     * a list.
     *
     * @param books list of books
     * @return query
     */
    private Query restrictByBooks(String[] books) {
        if (books.length == 1) {
            return new Query(SearchFields.BOOK_ID, books[0]);
        }

        List<Query> top = new ArrayList<>();
        for (String book : books) {
            top.add(new Query(SearchFields.BOOK_ID, book));
        }

        return new Query(QueryOperation.OR, top.toArray(new Query[top.size()]));
    }
}
