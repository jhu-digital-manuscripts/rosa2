package rosa.website.core.client.view.impl;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyPressHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Panel;
import rosa.website.core.client.view.HeaderViewWithSearch;
import rosa.website.search.client.widget.BasicSearchWidget;

import java.util.Map;

public class HeaderViewWithSearchImpl extends Composite implements HeaderViewWithSearch {
    private final FlowPanel bannerPanel;
    private final Panel navPanel;
    private final BasicSearchWidget searchWidget;

    private Presenter presenter;

    private final ClickHandler goHomeClickHandler = new ClickHandler() {
        @Override
        public void onClick(ClickEvent event) {
            if (presenter != null) {
                presenter.goHome();
            }
        }
    };

    /**  */
    public HeaderViewWithSearchImpl() {
        Panel root = new FlowPanel();
        searchWidget = new BasicSearchWidget();
        bannerPanel = new FlowPanel();
        navPanel = new HorizontalPanel();

        root.setStylePrimaryName("Header");

        bannerPanel.add(searchWidget);

        root.add(bannerPanel);
        root.add(navPanel);

        initWidget(root);
    }

    @Override
    public void setPresenter(Presenter presenter) {
        this.presenter = presenter;
    }

    @Override
    public void addHeaderImage(String imageUrl, String altText) {
        Image image = new Image(imageUrl);

        image.setAltText(altText);
        image.addStyleName("link");
        image.addClickHandler(goHomeClickHandler);

        if (bannerPanel.getWidgetCount() > 1) {
            bannerPanel.insert(image, bannerPanel.getWidgetCount() - 1);
        } else {
            bannerPanel.add(image);
        }

    }

    @Override
    public void setSearchButtonText(String text) {
        searchWidget.setSearchButtonText(text);
    }

    @Override
    public HandlerRegistration addSearchClickHandler(ClickHandler handler) {
        return searchWidget.addSearchButtonClickHandler(handler);
    }

    @Override
    public HandlerRegistration addSearchKeyPressHandler(KeyPressHandler handler) {
        return searchWidget.addSearchTextBoxKeyPressHandler(handler);
    }

    @Override
    public void addAdvancedSearchLink(String displayText, String targetHistoryToken) {
        searchWidget.setHyperlink(displayText, targetHistoryToken, "link");
    }

    @Override
    public String getSearchToken() {
        String searchTerm = searchWidget.getText();
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            return null;
        }

        return "ALL;" + escaped(searchTerm.trim()) + ";0";
    }

    @Override
    public void addNavLink(String label, String target) {
        Anchor link = new Anchor(label, target);
        link.addStyleName("headerLink");
        navPanel.add(new Anchor(label, target));
    }

    @Override
    public void addNavMenu(String topLabel, Map<String, String> subMenu) {

    }

    private String escaped(String str) {
        if (str == null || str.isEmpty()) {
            return "";
        }

        if (str.contains(",") || str.contains("\n")) {
            return "\"" + str + "\"";
        }
//        else if (str.length() > 3 && (str.substring(1, str.length() - 2).contains("\""))) {
//
//        }
        else {
            return str;
        }
    }
}
