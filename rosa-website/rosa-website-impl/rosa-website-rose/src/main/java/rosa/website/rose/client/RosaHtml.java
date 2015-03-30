package rosa.website.rose.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.ExternalTextResource;
import com.google.gwt.resources.client.TextResource;

public interface RosaHtml extends ClientBundle {
    RosaHtml INSTANCE = GWT.create(RosaHtml.class);

    @ClientBundle.Source("html/character_names.html")
    ExternalTextResource characterNamesHtml();

    @ClientBundle.Source("html/collection_data.html")
    ExternalTextResource collectionDataHtml();

    @ClientBundle.Source("html/contact.html")
    ExternalTextResource contactHtml();

    @ClientBundle.Source("html/donation.html")
    ExternalTextResource donationHtml();

    @ClientBundle.Source("html/home.html")
    TextResource homeHtml();

    @ClientBundle.Source("html/illustration_titles.html")
    ExternalTextResource illustrationTitlesHtml();

    @ClientBundle.Source("html/narrative_sections.html")
    ExternalTextResource narrativeSectionsHtml();

    @ClientBundle.Source("html/partners.html")
    ExternalTextResource partnersHtml();

    @ClientBundle.Source("html/project_history.html")
    ExternalTextResource projectHistoryHtml();

    @ClientBundle.Source("html/rose_history.html")
    ExternalTextResource roseHistoryHtml();

    @ClientBundle.Source("html/terms_and_conditions.html")
    ExternalTextResource termsHtml();
}
