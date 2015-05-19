package rosa.website.core.client;

import com.google.gwt.user.client.History;
import com.google.gwt.user.client.ui.Widget;
import rosa.website.core.client.event.FlashStatusChangeEvent;
import rosa.website.core.client.view.SidebarView;
import rosa.website.model.select.SelectCategory;

import java.util.HashMap;
import java.util.Map;

// TODO i18n
public class SidebarPresenter implements SidebarView.Presenter {
    private SidebarView view;
    private ClientFactory clientFactory;

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

        links.put("Description", "book;" + bookId);
        links.put("Page turner", "read;" + bookId);
        links.put("Browse images", "browse;" + bookId);

        view.setBookLinks("Book", links);
    }

    public void clearBookLinks() {
        view.clearBookLinks();
    }

    private void addSiteNavLinks() {
        Map<String, String> nav_links = new HashMap<>();

        nav_links.put("Rose summary", "rose");
        nav_links.put("Extant manuscripts", "corpus");
        nav_links.put("Collection spreadsheet", "data");
        nav_links.put("Narrative sections", "sections");
        nav_links.put("Illustration titles", "illustrations");
        nav_links.put("Character names", "chars");

        view.setSiteNavigationLinks(nav_links);
    }

    private void addBookSelectLinks() {
        Map<String, String> links = new HashMap<>();

        links.put("Repository", "select;" + SelectCategory.REPOSITORY);
        links.put("Common name", "select;" + SelectCategory.COMMON_NAME);
        links.put("Current location", "select;" + SelectCategory.LOCATION);
        links.put("Date", "select;" + SelectCategory.DATE);
        links.put("Origin", "select;" + SelectCategory.ORIGIN);
        links.put("Type", "select;" + SelectCategory.TYPE);
        links.put("Illustrations", "select;" + SelectCategory.NUM_ILLUSTRATIONS);
        links.put("Folios", "select;" + SelectCategory.NUM_FOLIOS);
        links.put("Transcription", "select;" + SelectCategory.TRANSCRIPTION);

        view.addSection("Select book by", links);
    }

    private void addProjectLinks() {
        Map<String, String> links = new HashMap<>();

        links.put("Terms and conditions", "terms");
        links.put("Partners", "partners");
        links.put("Project history", "project");
        links.put("Donation", "donation");
        links.put("Contact us", "contact");

        view.addSection("Project", links);
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
