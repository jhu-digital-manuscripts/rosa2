package rosa.website.core.client.widget;

import com.google.gwt.user.cellview.client.CellBrowser;

public interface BookSelectionBrowserResources extends CellBrowser.Resources {

    @Override
    @Source("BookSelectBrowser.css")
    CellBrowser.Style cellBrowserStyle();

}
