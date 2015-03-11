package rosa.website.test.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.ExternalTextResource;

public interface Resources extends ClientBundle {
    Resources INSTANCE = GWT.create(Resources.class);

    @Source("home.html")
    public ExternalTextResource home();

    @Source("notHome.html")
    public ExternalTextResource notHome();

}
