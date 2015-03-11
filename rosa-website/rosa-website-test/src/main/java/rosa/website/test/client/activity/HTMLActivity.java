package rosa.website.test.client.activity;

import com.google.gwt.activity.shared.Activity;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.resources.client.ExternalTextResource;
import com.google.gwt.resources.client.ResourceCallback;
import com.google.gwt.resources.client.ResourceException;
import com.google.gwt.resources.client.TextResource;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import rosa.website.test.client.ClientFactory;
import rosa.website.test.client.place.HTMLPlace;
import rosa.website.test.client.view.HTMLView;

import java.util.logging.Level;
import java.util.logging.Logger;

public class HTMLActivity implements Activity {
    private static final Logger logger = Logger.getLogger(HTMLActivity.class.toString());

    private String name;
    private ExternalTextResource resource;
    private HTMLView view;

    public HTMLActivity(HTMLPlace place, ClientFactory clientFactory) {
        this.name = place.getName();
        this.resource = clientFactory.getExternalHtml(name);
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

        if (resource == null) {
            logger.log(Level.SEVERE, "Name not mapped to a resource. [" + name + "]");
            return;
        }

        try {
            resource.getText(new ResourceCallback<TextResource>() {
                @Override
                public void onError(ResourceException e) {
                    logger.log(Level.SEVERE, "Unable to retrieve resource.", e);
                }

                @Override
                public void onSuccess(TextResource resource) {
                    view.setHTML(resource.getText());
                }
            });
        } catch (ResourceException e) {
            logger.log(Level.SEVERE, "Unable to retrieve resource.", e);
        }
    }
}
