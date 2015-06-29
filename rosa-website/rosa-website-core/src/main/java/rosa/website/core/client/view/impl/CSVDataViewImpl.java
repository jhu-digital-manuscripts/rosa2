package rosa.website.core.client.view.impl;

import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.SimplePanel;
import rosa.website.core.client.view.CSVDataView;
import rosa.website.core.client.widget.CSVWidget;
import rosa.website.model.csv.CSVData;

public class CSVDataViewImpl extends Composite implements CSVDataView {

    private CSVWidget display;
    private SimplePanel description;

    /**  */
    public CSVDataViewImpl() {
        FlowPanel root = new FlowPanel();
        display = new CSVWidget();
        description = new SimplePanel();

        root.add(description);
        root.add(display);
        root.setSize("100%", "100%");

        initWidget(root);
    }

    @Override
    public void clear() {
        display.clear();
        description.clear();
    }

    @Override
    public void setData(CSVData data) {
        display.setData(data);
    }

    @Override
    public void setDescription(String description) {
        this.description.clear();
        this.description.setWidget(new HTML(description));
    }
}
