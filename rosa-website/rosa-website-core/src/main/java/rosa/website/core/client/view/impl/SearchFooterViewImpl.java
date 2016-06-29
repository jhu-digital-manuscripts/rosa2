package rosa.website.core.client.view.impl;

import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyPressHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import rosa.website.core.client.view.SearchFooterView;
import rosa.website.search.client.widget.BasicSearchWidget;

public class SearchFooterViewImpl extends Composite implements SearchFooterView {
    private final BasicSearchWidget searchWidget;

    public SearchFooterViewImpl() {
        FlowPanel root = new FlowPanel();
        searchWidget = new BasicSearchWidget();
        root.add(searchWidget);

        initWidget(root);
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

    private String escaped(String str) {
        if (str == null || str.isEmpty()) {
            return "";
        } else if (str.contains(",") || str.contains("\n")) {
            return "\"" + str + "\"";
        } else {
            return str;
        }
    }
}
