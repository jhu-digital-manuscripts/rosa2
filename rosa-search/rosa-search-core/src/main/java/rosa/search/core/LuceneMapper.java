package rosa.search.core;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;

import rosa.archive.model.Book;
import rosa.archive.model.BookCollection;
import rosa.search.model.Query;
import rosa.search.model.SearchField;
import rosa.search.model.SearchFieldType;

/**
 * Map between search queries and lucene queries.
 * Create lucene documents from the archive.
 */
public interface LuceneMapper {
    /**
     * Return search field which will be unique per document.
     * 
     * @return search field
     */
    SearchField getIdentifierSearchField();

    /**
     * Return Lucene field corresponding to a search field of a given type.
     * 
     * @param field search field
     * @param type search field type (language)
     * @return lucene field
     */
    String getLuceneField(SearchField field, SearchFieldType type);

    /**
     * @return Analyzer used for all documents.
     */
    Analyzer getAnalyzer();

    /**
     * @param name lucene field
     * @return Search field name corresponding to a lucene field.
     */
    String getSearchFieldNameFromLuceneField(String name);

    /**
     * Transform a search query into a lucene query.
     * Throw an IllegalArgumentException if the query cannot be handled, perhaps because of an unknown field name.
     * 
     * @param query rosa query
     * @return Lucene query
     */
    org.apache.lucene.search.Query createLuceneQuery(Query query) throws IllegalArgumentException;

    
    /**
     * Create a lucene query from a string no matter what the contents of the string.
     * 
     * @param query query string
     * @return Lucene query or null if query string has no terms.
     */
    org.apache.lucene.search.Query createLuceneQuery(String query);
    
    /**
     * @param query rosa query
     * @return Return the names of lucene field used by this query which should be included in context
     */
    Set<String> getLuceneContextFields(Query query);

    /**
     * Create and index Lucene documents for a given book within a book
     * collection.
     *
     * @param col BookCollection object
     * @param book Book object
     * @return list of documents representing the book
     * @throws IOException .
     */
    List<Document> createDocuments(BookCollection col, Book book) throws IOException;

    /**
     * Return search fields whose values should be included in search results.
     */
    List<SearchField> getIncludeValueSearchFields();
}
