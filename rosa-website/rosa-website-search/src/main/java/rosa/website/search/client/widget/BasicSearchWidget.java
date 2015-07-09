package rosa.website.search.client.widget;

import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Hyperlink;
import com.google.gwt.user.client.ui.TextBox;

/**
 * A widget to facilitate basic search capabilities. It includes one area to input
 * a text query, a button to start the search, and a link to an advanced search
 * place, if applicable. It does not include any more advanced features for search
 * such as boolean search operations or search restriction by category.
 */
public class BasicSearchWidget extends Composite {

    private final TextBox searchBox;
    private final Button searchButton;
    private Hyperlink advancedSearchLink;

    /**  */
    public BasicSearchWidget() {
        FlowPanel root = new FlowPanel();

        this.searchBox = new TextBox();
        this.searchButton = new Button();
        this.advancedSearchLink = new Hyperlink();

        advancedSearchLink.setVisible(false);

        root.add(searchBox);
        root.add(searchButton);
        root.add(advancedSearchLink);

        root.setStylePrimaryName("Search");

        initWidget(root);
    }

    /**
     * Set the text that displays on the button.
     *
     * @param text .
     */
    public void setSearchButtonText(String text) {
        searchButton.setText(text);
    }

    /**
     * Add a handler to deal with click events on the search button.
     *
     * @param handler .
     * @return .
     */
    public HandlerRegistration addSearchButtonClickHandler(ClickHandler handler) {
        return searchButton.addClickHandler(handler);
    }

    /**
     * Set focus on this widget, or take focus from this widget.
     *
     * @param focused does the widget have focus?
     */
    public void setFocus(boolean focused) {
        searchBox.setFocus(focused);
    }

    /**
     * Set a hyperlink to another place in this application. Often,
     * this link is used to point to an "advanced search"
     *
     * @param displayText text to display
     * @param historyToken target history token
     */
    public void setHyperlink(String displayText, String historyToken) {
        advancedSearchLink.setText(displayText);
        advancedSearchLink.setTargetHistoryToken(historyToken);
        advancedSearchLink.setVisible(true);
    }

    /**
     * Set a hyperlink to another place in this application and give it a CSS
     * class.
     *
     * @param displayText text to display
     * @param historyToken target history token
     * @param primaryStyleName primary style name
     */
    public void setHyperlink(String displayText, String historyToken, String primaryStyleName) {
        setHyperlink(displayText, historyToken);
        advancedSearchLink.setStylePrimaryName(primaryStyleName);
    }

    public String getText() {
        return searchBox.getText();
    }
}
