package rosa.website.core.client.activity;

import com.google.gwt.activity.shared.Activity;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import rosa.website.core.client.BaseClientFactory;
import rosa.website.core.client.StaticResourceServiceAsync;
import rosa.website.core.client.place.TestPlace;
import rosa.website.core.client.view.TestView;

import java.util.logging.Level;
import java.util.logging.Logger;

public class TestActivity implements Activity {
    private static final Logger logger = Logger.getLogger(TestActivity.class.toString());

    private String name;
    private TestView view;
    private StaticResourceServiceAsync service;

    public TestActivity(TestPlace place, BaseClientFactory clientFactory) {
        this.name = place.getName();
        this.view = clientFactory.testView();
        this.service = clientFactory.staticResourceService();
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

        service.getStaticHtml(name, null, new AsyncCallback<String>() {
            @Override
            public void onFailure(Throwable caught) {
                logger.log(Level.SEVERE, "Failed to get resource.", caught);
            }

            @Override
            public void onSuccess(String result) {
                view.setHTML(result);
            }
        });
    }
}
