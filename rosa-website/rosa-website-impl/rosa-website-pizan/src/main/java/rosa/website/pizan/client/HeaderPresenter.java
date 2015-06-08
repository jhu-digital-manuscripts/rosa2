package rosa.website.pizan.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.Widget;
import rosa.website.core.client.ClientFactory;
import rosa.website.core.client.view.HeaderView;
import rosa.website.core.client.view.HeaderView.Presenter;

// TODO add search stuff here
public class HeaderPresenter implements Presenter, IsWidget {

    private final HeaderView view;

    /**
     * @param clientFactory .
     */
    public HeaderPresenter(ClientFactory clientFactory) {
        this.view = clientFactory.headerView();
        view.setPresenter(this);

        view.addHeaderImage(GWT.getModuleBaseURL() + "banner_image1.gif", "");
        view.addHeaderImage(GWT.getModuleBaseURL() + "banner_text.jpg", "Roman de la Rose Digital Library");
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
