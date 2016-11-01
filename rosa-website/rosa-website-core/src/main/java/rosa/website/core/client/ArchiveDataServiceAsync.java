package rosa.website.core.client;

import com.google.gwt.user.client.rpc.AsyncCallback;

import rosa.archive.model.ImageList;
import rosa.website.model.select.BookSelectList;
import rosa.website.model.select.SelectCategory;
import rosa.website.model.table.Table;
import rosa.website.model.table.Tables;
import rosa.website.model.view.BookDescriptionViewModel;
import rosa.website.model.view.FSIViewerModel;

/**
 * Async interface pair with {@link rosa.website.core.client.ArchiveDataService}
 */
public interface ArchiveDataServiceAsync {
    void loadCSVData(String collection, String lang, Tables type, AsyncCallback<Table> cb);
    void loadBookSelectionData(String collection, SelectCategory category, String lang, AsyncCallback<BookSelectList> cb);
    void loadPermissionStatement(String collection, String book, String lang, AsyncCallback<String> cb);
    void loadImageListAsString(String collection, String book, AsyncCallback<String> cb);
    void loadImageList(String collection, String book, AsyncCallback<ImageList> cb);
    void loadFSIViewerModel(String collection, String book, String language, AsyncCallback<FSIViewerModel> cb);
    void loadBookDescriptionModel(String collection, String book, String language, AsyncCallback<BookDescriptionViewModel> cb);
}
