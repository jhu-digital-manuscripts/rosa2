package rosa.website.pizan.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.ExternalTextResource;
import com.google.gwt.resources.client.TextResource;

public interface PizanHtml extends ClientBundle {
    PizanHtml INSTANCE = GWT.create(PizanHtml.class);

    @Source("html/character_names.html")
    ExternalTextResource characterNamesHtml();

    @Source("html/christine_de_pizan.html")
    ExternalTextResource pizanHtml();

    @Source("html/collection_data.html")
    ExternalTextResource collectionDataHtml();

    @Source("html/contact.html")
    ExternalTextResource contactHtml();

    @Source("html/home.html")
    TextResource homeHtml();

    @Source("html/illustration_titles.html")
    ExternalTextResource illustrationTitlesHtml();

    @Source("html/partners.html")
    ExternalTextResource partnersHtml();

    @Source("html/proper_names.html")
    ExternalTextResource properNamesHtml();

    @Source("html/terms_and_conditions.html")
    ExternalTextResource termsHtml();

    @Source("html/works.html")
    ExternalTextResource worksHtml();
}
