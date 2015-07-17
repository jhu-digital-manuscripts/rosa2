package rosa.website.search.client.widget;

import com.google.gwt.user.cellview.client.CellList;
import com.google.gwt.user.cellview.client.SimplePager;
import com.google.gwt.user.cellview.client.SimplePager.TextLocation;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.view.client.ListDataProvider;
import rosa.website.search.client.model.SearchMatchModel;
import rosa.website.search.client.model.SearchResultModel;

import java.util.ArrayList;

public class SearchResultsWidget extends Composite {

    private ListDataProvider<SearchMatchModel> matchDataProvider;
    private SimplePager pager;

    public SearchResultsWidget() {
        SimplePanel root = new SimplePanel();

        CellList<SearchMatchModel> cellList = new CellList<>(new SearchMatchCell());
        this.matchDataProvider = new ListDataProvider<>();

        matchDataProvider.addDataDisplay(cellList);
        cellList.setPageSize(20);

        pager = new SimplePager(TextLocation.CENTER, true, true);
        pager.setDisplay(cellList);
        pager.setPageSize(20);

        root.setSize("100%", "100%");
        root.setWidget(cellList);

        initWidget(root);
    }

    public void setData(SearchResultModel searchResult) {
        if (searchResult.getMatchList() != null && !searchResult.getMatchList().isEmpty()) {
            matchDataProvider.setList(searchResult.getMatchList());
            pager.setPageStart((int) searchResult.getOffset()); // Note: casting LONG as INT, possible data loss
            matchDataProvider.flush();
        }
    }

    public void clear() {
        matchDataProvider.setList(new ArrayList<SearchMatchModel>());
        matchDataProvider.flush();
    }

}
