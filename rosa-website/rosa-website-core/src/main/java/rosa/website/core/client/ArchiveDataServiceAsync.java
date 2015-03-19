package rosa.website.core.client;

import com.google.gwt.user.client.rpc.AsyncCallback;
import rosa.archive.model.Book;
import rosa.archive.model.BookCollection;
import rosa.website.model.csv.BookDataCSV;
import rosa.website.model.csv.CSVData;
import rosa.website.model.csv.CollectionCSV;
import rosa.website.model.csv.CsvType;
import rosa.website.model.csv.IllustrationTitleCSV;

/**
 * Async interface pair with {@link rosa.website.core.client.ArchiveDataService}
 */
public interface ArchiveDataServiceAsync {
    void loadCSVData(String collection, String lang, CsvType type, AsyncCallback<CSVData> cb);
    void loadCollectionData(String collection, String lang, AsyncCallback<CollectionCSV> cb);
    void loadCollectionBookData(String collection, String lang, AsyncCallback<BookDataCSV> cb);
    void loadIllustrationTitles(String collection, AsyncCallback<IllustrationTitleCSV> cb);
    void loadBookCollection(String collection, AsyncCallback<BookCollection> cb);
    void loadBook(String collection, String book, AsyncCallback<Book> cb);
}
