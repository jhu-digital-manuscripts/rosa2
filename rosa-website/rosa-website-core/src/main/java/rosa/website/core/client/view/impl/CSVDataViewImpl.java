package rosa.website.core.client.view.impl;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.SimplePanel;
import rosa.website.core.client.view.CSVDataView;
import rosa.website.core.client.widget.CSVWidget;
import rosa.website.model.csv.CSVData;

import java.util.Map;

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
    public void setPresenter(Presenter presenter) {
        display.setPresenter(presenter);
    }

    @Override
    public void setData(CSVData data) {
        display.setData(data);
        resetPosition();
    }

    @Override
    public void setData(CSVData data, Map<Enum, String> links, String[] headers) {
        display.setData(data, links, headers);
        resetPosition();
    }

    @Override
    public void setDescription(String description) {
        this.description.clear();
        this.description.setWidget(new HTML(description));
        resetPosition();
    }

    private void resetPosition() {
        Scheduler.get().scheduleDeferred(new ScheduledCommand() {
            @Override
            public void execute() {
                if (getParent() instanceof ScrollPanel) {
                    ((ScrollPanel) getParent()).setVerticalScrollPosition(0);
                }
            }
        });
    }
}
