package rosa.website.core.client;

import com.google.gwt.user.client.rpc.AsyncCallback;
import rosa.archive.model.ImageList;
import rosa.website.model.view.BookDescriptionViewModel;
import rosa.website.model.view.FSIViewerModel;
import rosa.website.model.csv.BookDataCSV;
import rosa.website.model.csv.CSVData;
import rosa.website.model.csv.CollectionCSV;
import rosa.website.model.csv.CSVType;
import rosa.website.model.csv.IllustrationTitleCSV;
import rosa.website.model.select.BookSelectList;
import rosa.website.model.select.SelectCategory;

/**
 * Async interface pair with {@link rosa.website.core.client.ArchiveDataService}
 */
public interface ArchiveDataServiceAsync {
    void loadCSVData(String collection, String lang, CSVType type, AsyncCallback<CSVData> cb);
    void loadCollectionData(String collection, String lang, AsyncCallback<CollectionCSV> cb);
    void loadCollectionBookData(String collection, String lang, AsyncCallback<BookDataCSV> cb);
    void loadBookSelectionData(String collection, SelectCategory category, String lang, AsyncCallback<BookSelectList> cb);
    void loadIllustrationTitles(String collection, AsyncCallback<IllustrationTitleCSV> cb);
    void loadPermissionStatement(String collection, String book, String lang, AsyncCallback<String> cb);
    void loadImageListAsString(String collection, String book, AsyncCallback<String> cb);
    void loadImageList(String collection, String book, AsyncCallback<ImageList> cb);
    void loadFSIViewerModel(String collection, String book, String language, AsyncCallback<FSIViewerModel> cb);
    void loadBookDescriptionModel(String collection, String book, String language, AsyncCallback<BookDescriptionViewModel> cb);
}
