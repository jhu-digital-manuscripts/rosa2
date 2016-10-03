package rosa.website.pizan.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.i18n.client.Constants;

public interface WebsiteConfig extends Constants {
    WebsiteConfig INSTANCE = GWT.create(WebsiteConfig.class);

    String collection();
    String fsiShare();
    String historyDelimiter();
    String defaultPage();
    String htmlHistory();
    String csvHistory();
    String fsiFlashUrl();
    String fsiServerUrl();
    @DefaultBooleanValue(false)
    boolean trackAnalytics();
    @DefaultStringValue("left")
    String sideBarLocation();
}
