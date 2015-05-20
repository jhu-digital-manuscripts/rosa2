package rosa.website.core.client;

import com.google.gwt.user.client.History;
import com.google.gwt.user.client.ui.Widget;
import rosa.website.core.client.event.FlashStatusChangeEvent;
import rosa.website.core.client.view.SidebarView;
import rosa.website.model.select.SelectCategory;

import java.util.HashMap;
import java.util.Map;

public class SidebarPresenter implements SidebarView.Presenter {
    private SidebarView view;
    private ClientFactory clientFactory;
    private final Labels labels = Labels.INSTANCE;

    public SidebarPresenter(SidebarView view, ClientFactory clientFactory) {
        this.view = view;
        this.clientFactory = clientFactory;

        view.setPresenter(this);
        addSiteNavLinks();
        addBookSelectLinks();
        addProjectLinks();
    }

    public void addBookLinks(String bookId) {
        Map<String, String> links = new HashMap<>();

        links.put(labels.description(), "book;" + bookId);
        links.put(labels.pageTurner(), "read;" + bookId);
        links.put(labels.browseImages(), "browse;" + bookId);

        view.setBookLinks(labels.book(), links);
    }

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

        links.put(labels.termsAndConditions(), "terms");
        links.put(labels.partners(), "partners");
        links.put(labels.projectHistory(), "project");
        links.put(labels.donation(), "donation");
        links.put(labels.contactUs(), "contact");

        view.addSection(labels.project(), links);
    }

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

//    TODO should this be handled here or in the AppController?
//    @Override
//    public void onBookSelect(BookSelectEvent event) {
//        if (event.isSelected()) {
//            addBookLinks(event.getBookId());
//        } else {
//            clearBookLinks();
//        }
//    }
}
