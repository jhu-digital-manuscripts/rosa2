package rosa.website.rose.client.activity;

import com.google.gwt.activity.shared.Activity;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import rosa.website.core.client.ClientFactory;
import rosa.website.core.client.place.AdvancedSearchPlace;
import rosa.website.core.client.view.AdvancedSearchView;

public class SearchActivity implements Activity {

    private final AdvancedSearchPlace place;
    private final AdvancedSearchView view;

    public SearchActivity(AdvancedSearchPlace place, ClientFactory clientFactory) {
        this.place = place;
        this.view = clientFactory.advancedSearchView();
    }

    @Override
    public String mayStop() {
        return null;
    }

    @Override
    public void onCancel() {

    }

    @Override
    public void onStop() {

    }

    @Override
    public void start(AcceptsOneWidget panel, EventBus eventBus) {
        panel.setWidget(view);
        console("Starting SearchActivity!");
        view.setFakeSearchModel();
    }

    private native void console(String message) /*-{
        console.log(message);
    }-*/;
}
