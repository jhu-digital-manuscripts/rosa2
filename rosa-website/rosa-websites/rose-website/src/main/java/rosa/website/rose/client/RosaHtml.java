package rosa.website.rose.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.ExternalTextResource;
import com.google.gwt.resources.client.TextResource;

public interface RosaHtml extends ClientBundle {
    RosaHtml INSTANCE = GWT.create(RosaHtml.class);

    @ClientBundle.Source("html/extant_manuscripts.html")
    ExternalTextResource extantManuscriptsHtml();

    /**
     * @return HTML content of "character names" csv page.
     */
    @ClientBundle.Source("html/character_names.html")
    ExternalTextResource characterNamesHtml();

    /**
     * @return HTML content for collection data page
     */
    @ClientBundle.Source("html/collection_data.html")
    ExternalTextResource collectionDataHtml();

    /**
     * @return HTML content for the contact us page
     */
    @ClientBundle.Source("html/contact.html")
    ExternalTextResource contactHtml();

    /**
     * @return HTML content for the "donation" page
     */
    @ClientBundle.Source("html/donation.html")
    ExternalTextResource donationHtml();

    /**
     * @return HTML content for the home page
     */
    @ClientBundle.Source("html/home.html")
    TextResource homeHtml();

    /**
     * @return HTML content for "illustration titles" csv page
     */
    @ClientBundle.Source("html/illustration_titles.html")
    ExternalTextResource illustrationTitlesHtml();

    /**
     * @return HTML content for "narrative sections" csv page
     */
    @ClientBundle.Source("html/narrative_sections.html")
    ExternalTextResource narrativeSectionsHtml();

    /**
     * @return HTML content for the "partners" page
     */
    @ClientBundle.Source("html/partners.html")
    ExternalTextResource partnersHtml();

    /**
     * @return HTML content for "project history" page
     */
    @ClientBundle.Source("html/project_history.html")
    ExternalTextResource projectHistoryHtml();

    /**=
     * @return HTML content for "rose history" page
     */
    @ClientBundle.Source("html/rose_history.html")
    ExternalTextResource roseHistoryHtml();

    /**
     * @return HTML content for "terms and conditions" page
     */
    @ClientBundle.Source("html/terms_and_conditions.html")
    ExternalTextResource termsHtml();
}
