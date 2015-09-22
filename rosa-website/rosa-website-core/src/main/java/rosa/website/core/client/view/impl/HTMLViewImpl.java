package rosa.website.core.client.view.impl;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.SimplePanel;
import rosa.website.core.client.view.HTMLView;

public class HTMLViewImpl extends Composite implements HTMLView {

    private SimplePanel root;

    /**  */
    public HTMLViewImpl() {
        this.root = new SimplePanel();
        root.setSize("100%", "100%");

        initWidget(root);
    }

    @Override
    public void setHTML(String html) {
        clear();
        root.add(new HTML(html));

        Scheduler.get().scheduleDeferred(new ScheduledCommand() {
            @Override
            public void execute() {
                if (getParent() instanceof ScrollPanel) {
                    ((ScrollPanel) getParent()).setVerticalScrollPosition(0);
                }
            }
        });
    }

    @Override
    public void clear() {
        if (root.getWidget() != null) {
            root.clear();
        }
    }
}
