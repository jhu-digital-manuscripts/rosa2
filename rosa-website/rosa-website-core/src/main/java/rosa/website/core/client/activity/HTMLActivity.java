package rosa.website.core.client.activity;

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
    private StaticResourceServiceAsync staticResourceService;
    private HTMLView view;

    public HTMLActivity(HTMLPlace place, ClientFactory clientFactory) {
        this.name = place.getName();
        this.staticResourceService = clientFactory.staticResourceService();
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

        staticResourceService.getStaticHtml(name, null, new AsyncCallback<String>() {
            @Override
            public void onFailure(Throwable throwable) {
                logger.log(Level.SEVERE, "Failed to retrieve static HTML page.", throwable);
            }

            @Override
            public void onSuccess(String s) {
                view.setHTML(s);
            }
        });
    }
}
