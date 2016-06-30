package rosa.website.rose.client;

import com.google.gwt.user.client.History;
import com.google.gwt.user.client.ui.Widget;
import rosa.website.core.client.ClientFactory;
import rosa.website.core.client.Labels;
import rosa.website.core.client.event.BookSelectEvent;
import rosa.website.core.client.event.FlashStatusChangeEvent;
import rosa.website.core.client.event.SidebarItemSelectedEvent;
import rosa.website.core.client.view.SidebarView;
import rosa.website.model.select.SelectCategory;

import java.util.HashMap;
import java.util.Map;

public class SidebarPresenter implements SidebarView.Presenter {
    private SidebarView view;
    private ClientFactory clientFactory;
    private final Labels labels = Labels.INSTANCE;

    /**
     * @param clientFactory .
     */
    public SidebarPresenter(ClientFactory clientFactory) {
        this.view = clientFactory.sidebarView();
        this.clientFactory = clientFactory;

        view.setPresenter(this);
        addSiteNavLinks();
        addBookSelectLinks();
        addProjectLinks();
        addLanguageLinks();
    }

    /**
     * Add links to read a book with the specified ID.
     *
     * @param bookId book ID
     */
    public void addBookLinks(String bookId) {
        Map<String, String> links = new HashMap<>();

        links.put(labels.description(), "book;" + bookId);
        links.put(labels.pageTurner(), "read;" + bookId);
        links.put(labels.browseImages(), "browse;" + bookId);

        view.setBookLinks(labels.book(), links);
    }

    /**
     * Clear book reading links when book is no longer selected.
     */
    public void clearBookLinks() {
        view.clearBookLinks();
    }

    private void addSiteNavLinks() {
        Map<String, String> nav_links = new HashMap<>();

        nav_links.put(labels.roseHistory(), "rose");
        nav_links.put(labels.roseCorpus(), "corpus");
        nav_links.put(labels.collectionData(), "data");
        nav_links.put(labels.narrativeSections(), "sections");
        nav_links.put(labels.illustrationTitles(), "illustrations");
        nav_links.put(labels.characterNames(), "chars");

        view.setSiteNavigationLinks(nav_links);
        view.addHelpLink(RosaHistoryConfig.getHelpUrl());
    }

    private void addBookSelectLinks() {
        Map<String, String> links = new HashMap<>();

        links.put(labels.repository(), "select;" + SelectCategory.REPOSITORY);
        links.put(labels.commonName(), "select;" + SelectCategory.COMMON_NAME);
        links.put(labels.currentLocation(), "select;" + SelectCategory.LOCATION);
        links.put(labels.date(), "select;" + SelectCategory.DATE);
        links.put(labels.origin(), "select;" + SelectCategory.ORIGIN);
        links.put(labels.type(), "select;" + SelectCategory.TYPE);
        links.put(labels.numIllustrations(), "select;" + SelectCategory.NUM_ILLUSTRATIONS);
        links.put(labels.numFolios(), "select;" + SelectCategory.NUM_FOLIOS);
        links.put(labels.transcription(), "select;" + SelectCategory.TRANSCRIPTION);

        view.addSection(labels.selectBookBy(), links);
    }

    private void addProjectLinks() {
        Map<String, String> links = new HashMap<>();

        links.put(labels.blog(), "http://romandelarose.blogspot.com");
        links.put(labels.termsAndConditions(), "terms");
        links.put(labels.partners(), "partners");
        links.put(labels.projectHistory(), "project");
//        links.put(labels.donation(), "donation");
        links.put(labels.contactUs(), "contact");

        view.addSection(labels.project(), links);
    }

    private void addLanguageLinks() {
        view.addLanguageLink("English", "en");
        view.addLanguageLink("Français", "fr");
    }

    /**
     * Resize this widget
     *
     * @param width .
     * @param height .
     */
    public void resize(String width, String height) {
        view.resize(width, height);
    }

    @Override
    public Widget asWidget() {
        return view.asWidget();
    }

    @Override
    public void setUseFlash(boolean useFlash) {
        clientFactory.eventBus().fireEvent(new FlashStatusChangeEvent(useFlash));
    }

    @Override
    public String getCurrentToken() {
        return History.getToken();
    }

// ------------------------------------------------------------------------------------------
// ----- Can be centralized in some app controller ------------------------------------------
// ------------------------------------------------------------------------------------------
    @Override
    public void onBookSelect(BookSelectEvent event) {
        if (event.isSelected()) {
            addBookLinks(event.getBookId());
        } else {
            clearBookLinks();
        }
    }

    @Override
    public void onFlashStatusChange(FlashStatusChangeEvent event) {
        clientFactory.context().setUseFlash(event.status());
    }

    @Override
    public void onSelected(SidebarItemSelectedEvent event) {
        view.selectItem(event.selectedItem);
    }
}
