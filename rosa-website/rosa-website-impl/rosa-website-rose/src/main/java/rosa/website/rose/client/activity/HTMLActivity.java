package rosa.website.rose.client.activity;

import com.google.gwt.activity.shared.Activity;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import rosa.website.core.client.ClientFactory;
import rosa.website.core.client.StaticResourceServiceAsync;
import rosa.website.core.client.place.HTMLPlace;
import rosa.website.core.client.view.HTMLView;

import java.util.logging.Level;
import java.util.logging.Logger;

public class HTMLActivity implements Activity {
    private static final Logger logger = Logger.getLogger(HTMLActivity.class.toString());

    private String name;
    private HTMLView view;

    public HTMLActivity(HTMLPlace place, ClientFactory clientFactory) {
        this.name = place.getName();
        this.view = clientFactory.htmlView();
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
        view.clear();
    }

    @Override
    public void start(AcceptsOneWidget panel, EventBus eventBus) {
        panel.setWidget(view);

        logger.fine("Enter HTMLActivity.start(...)");
        // TODO use ClientBundle to grab HTML
    }
}
