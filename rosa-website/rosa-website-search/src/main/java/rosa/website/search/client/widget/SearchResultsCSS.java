package rosa.website.search.client.widget;

import com.google.gwt.user.cellview.client.CellList;

public interface SearchResultsCSS extends CellList.Resources {
    @Override
    @Source("SearchResultsWidget.css")
    CellList.Style cellListStyle();
}
