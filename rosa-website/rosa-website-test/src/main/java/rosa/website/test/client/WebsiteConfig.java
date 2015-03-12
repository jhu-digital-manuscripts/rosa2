package rosa.website.test.client;

import com.google.gwt.core.shared.GWT;
import com.google.gwt.i18n.client.Constants;

public interface WebsiteConfig extends Constants {

    static WebsiteConfig INSTANCE = GWT.create(WebsiteConfig.class);

    String defaultPage();
    String htmlPages();
    String htmlPrefix();

}
