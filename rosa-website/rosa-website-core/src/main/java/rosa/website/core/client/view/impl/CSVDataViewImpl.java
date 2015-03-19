package rosa.website.core.client.view.impl;

import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.ScrollPanel;
import rosa.website.core.client.view.CSVDataView;
import rosa.website.core.client.widget.CsvWidget;
import rosa.website.model.csv.CSVData;

public class CSVDataViewImpl extends Composite implements CSVDataView {

    private CsvWidget display;

    public CSVDataViewImpl() {
        ScrollPanel root = new ScrollPanel();
        display = new CsvWidget();

        root.setWidget(display);
        root.setSize("100%", "100%");

        initWidget(root);
    }

    @Override
    public void clear() {
        display.clear();
    }

    @Override
    public void setData(CSVData data) {
        display.setData(data);
    }
}
