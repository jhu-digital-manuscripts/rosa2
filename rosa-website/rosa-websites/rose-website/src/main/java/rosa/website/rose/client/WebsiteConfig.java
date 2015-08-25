package rosa.website.rose.client;

import com.google.gwt.core.shared.GWT;
import com.google.gwt.i18n.client.Constants;

public interface WebsiteConfig extends Constants {
    WebsiteConfig INSTANCE = GWT.create(WebsiteConfig.class);

    /**
     * @return collection ID
     */
    String collection();

    /**
     * @return fsi share name
     */
    String fsiShare();

    /**
     * @return delimiter used in history token
     */
    String historyDelimiter();

    /**
     * @return default Place name
     */
    String defaultPage();

    /**
     * @return list of html page names
     */
    String htmlHistory();

    /**
     * @return list of CSV page names
     */
    String csvHistory();

    /**
     * @return FSI URL
     */
    String fsiUrl();

    /**
     * @return should Google analytics be tracked?
     */
    @DefaultBooleanValue(false)
    boolean trackAnalytics();
}
