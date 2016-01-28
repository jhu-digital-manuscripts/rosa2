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
     * @param field
     * @param type
     * @return lucene field
     */
    String getLuceneField(SearchField field, SearchFieldType ype);

    /**
     * @return Analyzer used for all documents.
     */
    Analyzer getAnalyzer();

    /**
     * @param name
     * @return Search field name corresponding to a lucene field.
     */
    String getSearchFieldNameFromLuceneField(String name);

    
    /**
     * Transform a search query into a lucene query.
     * 
     * @param query
     * @return Lucene query 
     */
    org.apache.lucene.search.Query createLuceneQuery(Query query);

    /**
     * @param query .
     * @return Return the names of lucene field used by this query
     */
    Set<String> getLuceneFields(Query query);

    /**
     * Create and index Lucene documents for a given book within a book
     * collection.
     *
     * @param col BookCollection object
     * @param book Book object
     * @return list of documents representing the book
     * @throws IOException
     */
    List<Document> createDocuments(BookCollection col, Book book) throws IOException;
}
