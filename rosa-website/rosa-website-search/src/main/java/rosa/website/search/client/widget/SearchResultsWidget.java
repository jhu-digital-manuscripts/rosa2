package rosa.website.search.client.widget;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.cellview.client.CellList;
import com.google.gwt.user.cellview.client.SimplePager;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.view.client.Range;
import com.google.gwt.view.client.RangeChangeEvent;
import rosa.website.search.client.model.SearchMatchModel;

import java.util.ArrayList;
import java.util.List;

public class SearchResultsWidget extends Composite {

    private CellList<SearchMatchModel> resultsList;
    private SimplePager pager;

    public SearchResultsWidget() {
        VerticalPanel root = new VerticalPanel();

        CellList.Resources css = GWT.create(SearchResultsCSS.class);
        resultsList = new CellList<>(new SearchMatchCell(), css);
        resultsList.setPageSize(20);

        pager = new SimplePager();
        pager.setDisplay(resultsList);
        pager.setPageSize(20);

        root.setSize("100%", "100%");
        root.add(resultsList);
        root.add(pager);

        initWidget(root);
    }

    public void setRowCount(int count) {
        resultsList.setRowCount(count);
    }

    public void clear() {
        resultsList.setRowData(new ArrayList<SearchMatchModel>());
    }

    public void setPageSize(int pageSize) {
        resultsList.setPageSize(pageSize);
        pager.setPageSize(pageSize);
    }

    public HandlerRegistration addRangeChangeHandler(RangeChangeEvent.Handler handler) {
        return resultsList.addRangeChangeHandler(handler);
    }

    public void setVisibleRange(int start, int length) {
        resultsList.setVisibleRangeAndClearData(new Range(start, length), true);
    }

    public void setRowData(int start, List<SearchMatchModel> dataList) {
        resultsList.setRowData(start, dataList);
    }

}
