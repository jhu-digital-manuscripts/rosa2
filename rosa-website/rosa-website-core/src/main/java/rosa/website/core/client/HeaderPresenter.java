package rosa.website.core.client;

import com.google.gwt.user.client.History;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.Widget;
import rosa.website.core.client.view.HeaderView;
import rosa.website.core.client.view.HeaderView.Presenter;

// TODO add search stuff here
public class HeaderPresenter implements Presenter, IsWidget {

    private final HeaderView view;

    public HeaderPresenter(ClientFactory clientFactory) {
        this.view = clientFactory.headerView();
        view.setPresenter(this);
    }

    @Override
    public Widget asWidget() {
        return view.asWidget();
    }

    @Override
    public void goHome() {
        History.newItem("home");
    }
}
