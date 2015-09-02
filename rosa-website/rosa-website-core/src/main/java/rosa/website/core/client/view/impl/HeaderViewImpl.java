package rosa.website.core.client.view.impl;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyPressHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Image;
import rosa.website.core.client.view.HeaderView;
import rosa.website.search.client.widget.BasicSearchWidget;

public class HeaderViewImpl extends Composite implements HeaderView {
    private final FlowPanel root;
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
    public HeaderViewImpl() {
        root = new FlowPanel();
        searchWidget = new BasicSearchWidget();

        root.setStylePrimaryName("Header");

        root.add(searchWidget);

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

        root.add(image);
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
        return "ALL;" + escaped(searchWidget.getText()) + ";0";
    }

    private String escaped(String str) {
        if (str == null || str.isEmpty()) {
            return "";
        }

        str = str.replaceAll("\\\"", "\"\"");

        if (str.contains(",") || str.contains("\"") || str.contains("\n") || str.contains(" ")) {
            return "\"" + str + "\"";
        } else {
            return str;
        }
    }
}
