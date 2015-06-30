package rosa.website.pizan.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.i18n.client.Constants;

public interface Labels extends Constants {
    Labels INSTANCE = GWT.create(Labels.class);

    String whoIsPizan();
    String works();
    String properNames();

    String selectBookBy();
    String repository();
    String commonName();
    String currentLocation();
    String date();
    String origin();
    String type();
    String numIllustrations();
    String numFolios();
    String transcription();

    String book();
    String description();
    String pageTurner();
    String browseImages();

    String project();
    String partners();
    String termsAndConditions();
    String contactUs();

    String search();
    String advancedSearch();

    String headerAlt();
}
