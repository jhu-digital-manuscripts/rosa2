package rosa.website.core.client.view.impl;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import rosa.website.core.client.view.ErrorComposite;
import rosa.website.core.client.view.HTMLView;

public class HTMLViewImpl extends ErrorComposite implements HTMLView {

    private VerticalPanel root;

    /**  */
    public HTMLViewImpl() {
        super();

        this.root = new VerticalPanel();
        root.setSize("100%", "100%");

        root.add(errorPanel);

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
        clearErrors();
        root.clear();
    }
}
