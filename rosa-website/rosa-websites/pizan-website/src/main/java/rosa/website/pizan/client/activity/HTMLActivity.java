package rosa.website.pizan.client.activity;

import com.google.gwt.activity.shared.Activity;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.resources.client.ExternalTextResource;
import com.google.gwt.resources.client.ResourceCallback;
import com.google.gwt.resources.client.ResourceException;
import com.google.gwt.resources.client.TextResource;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import rosa.website.core.client.ClientFactory;
import rosa.website.core.client.place.HTMLPlace;
import rosa.website.core.client.view.HTMLView;
import rosa.website.pizan.client.HistoryConfig;

import java.util.logging.Level;
import java.util.logging.Logger;

public class HTMLActivity implements Activity {
    private static final Logger logger = Logger.getLogger(HTMLActivity.class.toString());

    private String name;
    private HTMLView view;

    /**
     * @param place initial state
     * @param clientFactory .
     */
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
        view.clear();
        panel.setWidget(view);

        try {
            ExternalTextResource resource = HistoryConfig.getHtml(name);

            if (resource != null) {
                resource.getText(new ResourceCallback<TextResource>() {
                    @Override
                    public void onError(ResourceException e) {
                        logger.log(Level.SEVERE, "Failed to retrieve external text resource.", e);
                    }

                    @Override
                    public void onSuccess(TextResource resource) {
                        view.setHTML(resource.getText());
                    }
                });
            } else {
                view.setHTML(HistoryConfig.getHomeHtml().getText());
            }

        } catch (ResourceException e) {
            logger.log(Level.SEVERE,
                    "Failed to retrieve external text resource. Potential error with configuration.", e);
        }
    }
}
