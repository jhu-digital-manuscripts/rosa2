package rosa.website.core.client.widget;

import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.SimplePanel;

/**
 * Display a loading indicator to the user.
 */
public class LoadingPanel extends PopupPanel {
    public static LoadingPanel INSTANCE = new LoadingPanel();

    public LoadingPanel() {
        super(false, true);

        SimplePanel root = new SimplePanel();
        root.setStylePrimaryName("spinner-loader");

        super.setWidget(root);
    }

    @Override
    public void show() {
        super.show();
        center();
    }

}
