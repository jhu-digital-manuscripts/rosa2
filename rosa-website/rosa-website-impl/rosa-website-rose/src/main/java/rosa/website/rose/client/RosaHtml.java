package rosa.website.rose.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.resources.client.ClientBundle;

public interface RosaHtml extends ClientBundle {
    RosaHtml INSTANCE = GWT.create(RosaHtml.class);

    @ClientBundle.Source("html/character_names.html")
    String characterNamesHtml();

    @ClientBundle.Source("html/collection_data.html")
    String collectionDataHtml();

    @ClientBundle.Source("html/contact.html")
    String contactHtml();

    @ClientBundle.Source("html/donation.html")
    String donationHtml();

    @ClientBundle.Source("html/home.html")
    String homeHtml();

    @ClientBundle.Source("html/illustration_titles.html")
    String illustrationTitlesHtml();

    @ClientBundle.Source("html/narrative_sections.html")
    String narrativeSectionsHtml();

    @ClientBundle.Source("html/partners.html")
    String partnersHtml();

    @ClientBundle.Source("html/project_history.html")
    String projectHistoryHtml();

    @ClientBundle.Source("html/rose_history.html")
    String roseHistoryHtml();

    @ClientBundle.Source("html/terms_and_conditions.html")
    String termsHtml();
}
