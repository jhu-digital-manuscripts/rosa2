package rosa.website.search.client;

import rosa.search.model.Query;
import rosa.search.model.QueryOperation;
import rosa.search.model.SearchFields;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;

public class RosaQueryUtil implements QueryUtil {
    private static final Logger LOG = Logger.getLogger(RosaQueryUtil.class.toString());

    private static final String DELIMITER = ";";

    // #search;ALL;1234;POETRY;qwer-;rewq;RUBRIC;asdf;ALL;lkhj;BOOK;Marne3,AssembleeNationale1230,CodGall80;0
    @Override
    public Map<SearchCategory, String> queryParts(String token) {
        if (token == null || token.isEmpty()) {
            return null;
        }
        token = token.replaceAll("(-;)", ";").replaceAll("(--)", "-");

        Map<SearchCategory, String> map = new HashMap<>();
        String[] parts = token.split(DELIMITER);

        SearchCategory current_category = null;

        for (int i = 0; i < parts.length; i++) {
            SearchCategory category = SearchCategory.category(parts[i]);
            if (category != null && (i+1 < parts.length)) {
                current_category = category;
                map.put(current_category, parts[++i]);
            } else if (parts[i].equals("BOOK")) {
                break;
            } else if (category == null) {
                String term = map.get(current_category) + ";" + parts[i];
                map.put(current_category, term);
            }
        }

        return map;
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
            String offset = token.substring(index);
            try {
                return Integer.parseInt(offset);
            } catch (NumberFormatException e) {
                LOG.warning("Search token does not contain an offset. [" + token + "]");
            }
        }

        return 0;
    }

    @Override
    public Query toQuery(String token) {
        Map<SearchCategory, String> terms = queryParts(token);

        List<Query> top = new ArrayList<>();
        for (Entry<SearchCategory, String> entry : terms.entrySet()) {
            top.add(adaptToSearchFields(entry.getKey(), entry.getValue()));
        }
        top.add(restrictByBooks(bookRestrictionList(token)));

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
