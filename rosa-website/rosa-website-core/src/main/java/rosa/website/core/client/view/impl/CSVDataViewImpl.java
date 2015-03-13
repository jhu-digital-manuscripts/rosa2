package rosa.website.core.client.view.impl;

import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.SimplePanel;
import rosa.website.core.client.view.CSVDataView;

public class CSVDataViewImpl extends Composite implements CSVDataView {

    private SimplePanel root;

    public CSVDataViewImpl() {
        root = new SimplePanel();

        initWidget(root);
    }

    @Override
    public void clear() {
        root.clear();
    }

    @Override
    public void setData(String data) {
        root.setWidget(new Label(data));
    }
}
