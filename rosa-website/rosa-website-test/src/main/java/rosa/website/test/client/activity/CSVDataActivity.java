package rosa.website.test.client.activity;

import com.google.gwt.activity.shared.Activity;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import rosa.website.test.client.ClientFactory;
import rosa.website.test.client.place.CSVDataPlace;

public class CSVDataActivity implements Activity {

    private final CSVDataPlace place;

    public CSVDataActivity(CSVDataPlace place, ClientFactory clientFactory) {
        this.place = place;
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

    }
}
